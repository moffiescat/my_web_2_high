# 项目缺失功能与优化分析

## 概览

对后端 40 个 Java 文件、前端 14 个 Vue/JS 文件、SQL/配置/Docker 文件进行了全面审查。以下按严重程度排列。

---

## 一、致命问题（系统无法正常工作）

### 1.1 Redis 库存未初始化 — 秒杀必失败

**文件**: `SeckillServiceImpl.java` / `stock_deduction.lua`

Lua 脚本读取 `seckill:stock:{goodsId}` 判断库存，但**没有任何代码将该 key 写入 Redis**。MySQL 有库存，Redis 却是空值，脚本读到 `nil` 转成 `0`，每次秒杀都返回"已售罄"。

```java
// 缺少：项目启动时或秒杀开始前，将 t_seckill_goods.stock_count 同步到 Redis
```

**修复**: 添加 `@PostConstruct` 或定时任务预热 Redis 库存，或在秒杀商品上线时写入。

### 1.2 MySQL 库存扣减无行锁 — 超卖风险

**文件**: `SeckillServiceImpl.java:160-172`

`createOrder()` 中 `LambdaUpdateWrapper` 的 `.gt(StockCount, 0)` 只在 WHERE 中加了条件，但没有 `SELECT ... FOR UPDATE` 锁住该行。两个 MQ 消费者可能同时通过 `.gt` 检查并扣减。

```java
// 当前: 无锁，并发下可能双扣
seckillGoodsMapper.update(null, new LambdaUpdateWrapper<SeckillGoods>()
    .eq(SeckillGoods::getGoodsId, goodsId)
    .gt(SeckillGoods::getStockCount, 0)
    .setSql("stock_count = stock_count - 1"));
```

**修复**: 使用 `selectForUpdate()` 锁行后再扣减，或依赖 MySQL 行锁 + 乐观锁版本号。

---

## 二、安全问题

### 2.1 密钥明文暴露

`application.yml` 中所有密码和 JWT Secret 都是明文。生产环境应使用环境变量或配置中心。

### 2.2 前端路由无守卫

未登录用户可直接访问 `/profile`、`/cart`、`/order/list`，不会跳转登录页。`router/index.js` 没有 `beforeEach` 守卫。

### 2.3 订单越权访问

`OrderController.getDetail(orderId)` 不校验订单是否属于当前用户，任何登录用户可查看任意订单。

### 2.4 输入校验缺失

| DTO | 问题 |
|-----|------|
| `RegisterDto` | 密码无长度限制，手机号无格式校验 |
| `LoginDto` | 同上 |
| `SeckillDto` | 无任何 `@NotNull` |
| `SeckillController.execute()` | 缺少 `@Valid` 注解 |

### 2.5 错误信息泄漏

`GlobalExceptionHandler` 将 `RuntimeException.getMessage()` 直接返回客户端，暴露内部错误详情。

### 2.6 Token 存储在 localStorage

存在 XSS 风险，生产环境应使用 httpOnly Cookie。

### 2.7 限流功能未实现

`RedisKey.java` 定义了 `LIMIT_USER_API` 和 `LOCK_ORDER` 两个限流 Key，但代码中零处使用。DESIGN.md 描述的四层限流策略一行代码都没有。

---

## 三、功能缺失

### 3.1 购物车 — 无后端、无持久化

`Cart.vue` 的购物车数据仅存于组件内存，刷新即丢失。没有 `POST /api/cart/add`、`GET /api/cart/list`、`DELETE /api/cart/remove` 等后端 API。

**需要新增**: 购物车表 + CRUD API + 前端持久化。

### 3.2 用户信息接口缺失

`Profile.vue` 展示用户信息但后端没有 `GET /api/user/info` 端点，前端只能从 JWT payload 解码 userId，昵称/手机号/注册时间全部显示 `--`。

**需要新增**: `GET /api/user/info` 端点 + `UserController.getInfo()`。

### 3.3 消息通知 — 全部硬编码

`Messages.vue` 的三条通知全是 HTML 写死的，没有任何后端 API。RabbitMQ 中 `notifyQueue` 已声明但无消费者消费。

**需要新增**: 通知表 + 消息消费者 + `GET /api/notification/list` API。

### 3.4 支付功能缺失

订单状态有"待支付/已支付/已取消"，但没有 `POST /api/order/pay` 端点，也无支付回调。

### 3.5 订单详情页缺失

后端有 `GET /api/order/detail/{orderId}`，前端有对应 API 调用，但 router 中没有路由，没有页面。

### 3.6 重置密码缺失

无修改密码、忘记密码功能。

---

## 四、数据完整性

### 4.1 取消订单不恢复库存

`OrderServiceImpl.cancel()` 只改状态为已取消，不恢复 `t_seckill_goods.stock_count`。

### 4.2 无订单超时自动取消

待支付订单永远不超时，需要定时任务扫描超时订单自动取消。

### 4.3 取消订单应恢复 Redis 库存

取消时不仅要恢复 MySQL 库存，也要恢复 Redis `seckill:stock:{goodsId}`。

### 4.4 MQ 消费者无幂等

消息处理失败 requeue 后重试，如果已经扣了库存但插入订单失败，重试会再次扣库存。

---

## 五、性能问题

### 5.1 N+1 查询

`GoodsServiceImpl.listSeckillGoods()` 先查秒杀商品列表，再对每个秒杀商品单独查商品详情（N+1），应合并为一条 JOIN。

### 5.2 全量返回无分页

`/api/goods/list`、`/api/goods/seckill`、`/api/order/list` 全部返回全量数据，无 `page`/`size` 参数。

### 5.3 SQL 日志输出到 stdout

`mybatis-plus` 配置了 `StdOutImpl`，高并发下大量刷屏，严重影响性能。生产环境应关闭或改用 SLF4J DEBUG。

### 5.4 Redis 序列化冗余

`GenericJackson2JsonRedisSerializer` 在 Redis 中存储完整 Java 类元数据（如 `{"@class":"com.seckill.vo.GoodsVo",...}`），数据膨胀约一倍。

### 5.5 前端轮询无退避

`Detail.vue` 秒杀结果轮询固定 1 秒一次，无指数退避。1000 用户同时轮询 = 1000 QPS。应改为 1s → 2s → 4s → 10s 递增。

### 5.6 倒计时组件重复计算

`Countdown.vue` 的 h/m/s 三个 computed 各独立计算时间戳差，每秒做 6 次 `Date` 构造，应合并为一个 computed。

---

## 六、运维与可观测性

| 缺失项 | 说明 |
|--------|------|
| Spring Boot Actuator | 无 `/actuator/health`、`/actuator/metrics` |
| 结构化日志 | 无 Trace ID、MDC 上下文、JSON 日志格式 |
| 死信队列 | MQ 配置中设计了死信队列，实际未配置 |
| Dockerfile | 前后端无容器化构建文件 |
| CI/CD | 无 GitHub Actions / Jenkins |
| 优雅停机 | 未配置 `server.shutdown=graceful` |
| 测试 | 零测试文件，零代码覆盖率 |

---

## 七、优化建议（按优先级）

### P0 — 系统无法运行

| # | 问题 | 涉及文件 |
|---|------|---------|
| 1 | Redis 库存预热 | `SeckillServiceImpl` + 新增初始化逻辑 |
| 2 | MySQL 库存扣减加锁 | `SeckillServiceImpl.createOrder()` |

### P1 — 安全与数据完整性

| # | 问题 | 涉及文件 |
|---|------|---------|
| 3 | 前端路由守卫 | `router/index.js` |
| 4 | 订单越权校验 | `OrderController` |
| 5 | 取消订单恢复库存 | `OrderServiceImpl.cancel()` |
| 6 | DTO 校验注解 + @Valid | `RegisterDto`, `SeckillDto`, `SeckillController` |
| 7 | 敏感配置环境变量化 | `application.yml` |

### P2 — 功能完善

| # | 问题 | 涉及文件 |
|---|------|---------|
| 8 | `GET /api/user/info` 端点 | `UserController` + `Profile.vue` |
| 9 | 购物车后端 CRUD | 新增 `CartController` + cart 表 |
| 10 | 订单详情页 | `router/index.js` + 新增 `OrderDetail.vue` |
| 11 | MQ 幂等 + 死信队列 | `SeckillConsumer` + `RabbitMQConfig` |

### P3 — 性能与体验

| # | 问题 | 涉及文件 |
|---|------|---------|
| 12 | N+1 查询合并为 JOIN | `GoodsServiceImpl.listSeckillGoods()` |
| 13 | 列表分页 | 所有 list 接口 |
| 14 | 轮询指数退避 | `Detail.vue` |
| 15 | 关闭 SQL 日志 | `application.yml` |
| 16 | 加载骨架屏 | `Home.vue`, `Detail.vue` |

### P4 — 生产就绪

| # | 问题 | 涉及文件 |
|---|------|---------|
| 17 | Actuator + Prometheus | `pom.xml` + `application.yml` |
| 18 | 前后端 Dockerfile | 新增 |
| 19 | 单元测试 + 集成测试 | 新增 |
| 20 | 限流实现 | 新增 `RateLimitInterceptor` |

---

## 八、改动量估算

| 优先级 | 任务数 | 新增文件 | 修改文件 | 工作量 |
|--------|--------|---------|---------|--------|
| P0 | 2 | 1 | 2 | 0.5天 |
| P1 | 5 | 0 | 5 | 1天 |
| P2 | 4 | 3 | 3 | 1.5天 |
| P3 | 5 | 0 | 5 | 1天 |
| P4 | 4 | 3 | 2 | 1.5天 |
| **合计** | **20** | **7** | **17** | **~5.5天** |
