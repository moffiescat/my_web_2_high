# 布隆过滤器数据量优化方案

## 当前实现

**文件**: `BloomFilterConfig.java` → `SeckillServiceImpl.getSeckillPath()`

| 参数 | 值 | 说明 |
|------|-----|------|
| 预期插入量 n | 10000 | `AppConstants.BLOOM_FILTER_EXPECTED_INSERTIONS` |
| 误判率 p | 0.001 (0.1%) | `AppConstants.BLOOM_FILTER_FPP` |
| 当前内存占用 | ~17 KB | 由公式 `bits = -n × ln(p) / (ln2)²` 计算 |
| 实现方式 | Guava `BloomFilter<Long>` | JVM 堆内存，单机，不可变 |

**核心问题**: Guava BloomFilter 只适用于**小数据量 + 数据不变**的场景。当秒杀商品 ID 达到百万甚至千万级时，内存、加载速度、动态更新都面临瓶颈。

---

## 一、内存占用随数据量增长

公式：`所需 bits = -n × ln(p) / (ln2)²`，内存与 n 成**线性**关系。

| 数据量 n | 误判率 p | 所需 bits | 占用内存 | 风险 |
|----------|----------|-----------|----------|------|
| 1 万 | 0.1% | 14 万 | ~17 KB | 无 |
| 10 万 | 0.1% | 140 万 | ~170 KB | 无 |
| 100 万 | 0.1% | 1400 万 | ~1.7 MB | 无 |
| 1000 万 | 0.1% | 1.4 亿 | ~17 MB | JVM 堆可接受 |
| 1 亿 | 0.1% | 14 亿 | ~170 MB | 堆内存紧张，GC 压力大 |
| 10 亿 | 0.1% | 140 亿 | ~1.7 GB | **直接 OOM** |

### 策略一：限制数据范围

不要把全部历史商品放进过滤器，只缓存**活跃商品**：

```java
// 当前：全量加载
List<SeckillGoods> list = seckillGoodsMapper.selectList(null);

// 改为：仅加载未结束的秒杀商品
List<SeckillGoods> list = seckillGoodsMapper.selectList(
    new LambdaQueryWrapper<SeckillGoods>()
        .ge(SeckillGoods::getEndTime, LocalDateTime.now())
);
```

1000 个活跃秒杀商品只占 ~17 KB，消除了全量历史数据的内存压力。

### 策略二：放宽误判率

| 误判率 p | 相对内存占用（同 n） | 漏到 MySQL 的概率 |
|----------|---------------------|-------------------|
| 0.01% | ×1.0 (基准) | 万分之一 |
| 0.1% | ×0.7 | 千分之一 |
| 1% | ×0.45 | 百分之一 |
| 3% | ×0.33 | 百分之三 |

**关键认知**: 布隆过滤器即使误判，后面还有 MySQL 实际查询兜底。误判只是「多查一次 DB」，而非「数据错误」。所以 p=1% 在多数场景下完全可接受。

```java
// p = 0.1% → 1%，n=1000万：内存从 17MB 降到 ~7MB
BloomFilter.create(Funnels.longFunnel(), 10_000_000, 0.01);
```

---

## 二、启动加载慢

百万级数据从 MySQL `SELECT *` 全量加载到内存并逐条 `put`，启动时间估算：

| 数据量 | 加载时间（估算） |
|--------|-----------------|
| 1 万 | <100ms |
| 10 万 | ~500ms |
| 100 万 | ~3-5s |
| 1000 万 | **30s+** |

### 方案 A — 分页批量加载

```java
int pageSize = 10000;
Long lastId = 0L;
BloomFilter<Long> filter = BloomFilter.create(Funnels.longFunnel(), totalCount, 0.001);
while (true) {
    List<SeckillGoods> batch = seckillGoodsMapper.selectList(
        new LambdaQueryWrapper<SeckillGoods>()
            .gt(SeckillGoods::getId, lastId)
            .last("LIMIT " + pageSize)
            .orderByAsc(SeckillGoods::getId)
    );
    if (batch.isEmpty()) break;
    for (SeckillGoods sg : batch) {
        filter.put(sg.getGoodsId());
    }
    lastId = batch.get(batch.size() - 1).getId();
}
```

### 方案 B — 序列化到本地文件（推荐）

Guava BloomFilter 支持序列化，可以在数据变更时离线构建并写入文件，启动时直接反序列化加载：

```java
// 构建时：离线生成并写文件
BloomFilter<Long> filter = BloomFilter.create(...);
// ... 填充数据 ...
try (FileOutputStream fos = new FileOutputStream("bloom_filter.dat")) {
    filter.writeTo(fos);
}

// 启动时：从文件直接加载（毫秒级）
BloomFilter<Long> filter;
try (FileInputStream fis = new FileInputStream("bloom_filter.dat")) {
    filter = BloomFilter.readFrom(fis, Funnels.longFunnel());
}
```

| 加载方式 | 10万 | 100万 | 1000万 |
|----------|------|-------|--------|
| 逐条 put | 500ms | 5s | 30s+ |
| 文件反序列化 | **<10ms** | **<50ms** | **<200ms** |

---

## 三、不支持动态添加

Guava BloomFilter 构建后是**不可变的**。秒杀商品上下线无法实时同步到过滤器：

- 新增秒杀商品 → 过滤器中不存在 → 请求**穿透到 MySQL**（本该拦截）
- 只有重启应用才能重新加载

### 方案：迁移到 Redis BloomFilter

#### 选择一 — Redisson RBloomFilter（推荐，无需额外模块）

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.redisson</groupId>
    <artifactId>redisson-spring-boot-starter</artifactId>
    <version>3.32.0</version>
</dependency>
```

```java
@Configuration
public class RedisBloomConfig {

    @Bean
    public RBloomFilter<Long> goodsBloomFilter(RedissonClient redisson) {
        RBloomFilter<Long> filter = redisson.getBloomFilter("bloom:goods");
        filter.tryInit(1_000_000L, 0.01); // n=100万, p=1%
        return filter;
    }
}

// 新增商品时动态添加
@PostMapping("/admin/seckill/add")
public void addSeckillGoods(@RequestBody SeckillGoods sg) {
    seckillGoodsMapper.insert(sg);
    bloomFilter.add(sg.getGoodsId()); // 实时加入过滤器
}
```

优势：
- 动态 `add`，商品上线即时生效
- 分布式共享，多个服务实例共用一份
- Redis 持久化，重启不丢失
- 不占用 JVM 堆内存

#### 选择二 — Redis Stack（redis:7-stack 镜像，内置 BF 模块）

如果已经在用 Redis Stack，可以直接使用 `BF.ADD` / `BF.EXISTS` 命令，比 Redisson 更底层、性能更高。

### Guava vs Redis BloomFilter 对比

| 维度 | Guava (当前) | Redisson (推荐) | Redis Stack |
|------|-------------|----------------|-------------|
| 存储位置 | JVM 堆内存 | Redis | Redis |
| 启动加载 | 全量 DB 查询 | Redis 持久化 | Redis 持久化 |
| 动态 add | 不支持 | ✅ 支持 | ✅ 支持 |
| 多实例共享 | 各自独立 | ✅ 共享 | ✅ 共享 |
| 额外依赖 | Guava (已有) | Redisson | Redis Stack 镜像 |
| 网络开销 | 无 | 每次判断 1 次 RTT | 每次判断 1 次 RTT |
| 适用数据量 | < 100 万 | < 10 亿 | < 10 亿 |

---

## 四、方案选择决策树

```
数据量 < 100 万且数据不变？
  ├── 是 → 保持当前 Guava 方案，调大 n 即可
  └── 否 →
          数据量 < 1000 万？
            ├── 是 → Guava + 文件序列化 + 限制数据范围（活跃商品）+ p=1%
            └── 否 →
                    需要动态添加？
                      ├── 是 → Redisson RBloomFilter（推荐）
                      └── 否 → Redis Stack BF 模块
```

---

## 五、迁移到 Redisson 的操作步骤

### 1. 添加依赖

```xml
<dependency>
    <groupId>org.redisson</groupId>
    <artifactId>redisson-spring-boot-starter</artifactId>
    <version>3.32.0</version>
</dependency>
```

### 2. Redisson 配置

```yaml
# application.yml
spring:
  redis:
    redisson:
      config: |
        singleServerConfig:
          address: "redis://${REDIS_HOST:localhost}:${REDIS_PORT:6380}"
          password: "${REDIS_PASSWORD:redis123}"
```

### 3. 替换 BloomFilter Bean

```java
@Configuration
public class RedisBloomConfig {

    @Bean
    public RBloomFilter<Long> goodsBloomFilter(RedissonClient redisson) {
        RBloomFilter<Long> filter = redisson.getBloomFilter("bloom:goods");
        filter.tryInit(1_000_000L, 0.01);
        return filter;
    }
}
```

### 4. 修改使用方

```java
// 旧：Guava
if (!bloomFilter.mightContain(goodsId)) { ... }

// 新：Redisson（API 相同）
if (!bloomFilter.contains(goodsId)) { ... }
```

> **兼容提示**: Guava 用 `mightContain()`，Redisson 用 `contains()`，只需改方法名。

### 5. 删除旧文件

移除 `BloomFilterConfig.java` 和 Guava 依赖（如果项目中无其他 Guava 使用场景）。

---

## 六、被过滤器拦截的请求量计算

布隆过滤器只能拦截「不存在的商品 ID」请求。它的价值取决于**无效请求占比**：

```
有效拦截量 = 总请求数 × 无效 goodsId 请求比例 × (1 - 误判率)
```

| 总请求 | 无效请求比例 | 误判率 | 拦截量 | 穿透到 DB 的量 |
|--------|-------------|--------|--------|---------------|
| 10000/s | 50% | 1% | 4950/s | 5050/s |
| 10000/s | 90% | 1% | 8910/s | 1090/s |
| 10000/s | 10% | 1% | 990/s | 9010/s |

**结论**: 布隆过滤器的价值在无效请求比例高时最大（如被恶意扫描），正常流量下效果有限。因此投入优化前，建议先通过日志统计实际无效请求占比。
