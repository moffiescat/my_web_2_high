# 秒杀系统 — 项目结构与工作流文档

> 本文档说明项目当前的完整结构、各层职责、模块间交互关系，以及核心秒杀链路的工作原理。

---

## 1. 项目总览

```
my-web-2-high/
├── design.md                   # 系统设计文档 (架构/DB/API设计)
├── docker-compose.yml          # 中间件编排 (MySQL + Redis + RabbitMQ)
├── sql/init.sql                # 建表 DDL + 测试数据
│
├── backend/                    # Java后端 Spring Boot 3.2
│   └── src/main/java/com/seckill/
│       ├── SeckillApplication.java
│       ├── config/             # 配置层 (6个文件)
│       ├── controller/         # 接口层 (4个控制器)
│       ├── service/            # 业务接口 (4个)
│       ├── service/impl/       # 业务实现 (4个 ⭐核心逻辑)
│       ├── mapper/             # 数据访问层 MyBatis-Plus (5个)
│       ├── entity/             # 实体类 对应数据库表 (5个)
│       ├── dto/                # 请求参数对象 (4个)
│       ├── vo/                 # 响应视图对象 (3个)
│       ├── mq/                 # RabbitMQ消费者 (1个)
│       ├── interceptor/        # JWT认证拦截器 (1个)
│       └── utils/              # 工具类 (4个)
│
├── frontend/                   # Vue 3 前端
│   └── src/
│       ├── views/              # 页面 (5个)
│       ├── components/         # 公共组件 (1个)
│       ├── api/                # 后端API封装 (5个模块)
│       ├── router/             # 路由定义 (1个)
│       └── store/              # Pinia状态管理 (1个)
│
├── agent/                      # Python Agent (预留, Phase 2)
│   ├── main.py                 # FastAPI入口
│   └── requirements.txt
│
├── docs/                       # 文档
│   └── project-structure.md    # <= 你正在看的这份
└── README.md
```

---

## 2. 分层架构与职责

项目严格遵循分层架构，从上到下为：

```
┌─────────────────────────────────────────────────────┐
│  前端 (Vue 3)                                        │
│  职责: 页面渲染 / 用户交互 / 秒杀状态机 / 结果轮询      │
└──────────────────────┬──────────────────────────────┘
                       │ HTTP (REST API)
                       ▼
┌─────────────────────────────────────────────────────┐
│  Controller 层 (4个)                                  │
│  职责: 接收请求参数 / JWT鉴权 / 调用Service / 返回结果   │
└──────────────────────┬──────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────┐
│  Service 层 (4个接口 + 4个实现)                        │
│  职责: 业务编排 / 事务管理 / Lua脚本执行 / MQ消息发送    │
└───────┬──────────────┬──────────────┬───────────────┘
        │              │              │
        ▼              ▼              ▼
┌──────────────┐ ┌──────────────┐ ┌──────────────┐
│   Mapper 层   │ │   Redis      │ │  RabbitMQ    │
│ (MyBatis-Plus)│ │ (缓存/锁/限流)│ │ (消息队列)    │
│   5个Mapper   │ │  Lua脚本     │ │  1个Consumer  │
└──────┬───────┘ └──────────────┘ └──────────────┘
       │
       ▼
┌──────────────┐
│    MySQL     │
│   5张业务表   │
└──────────────┘
```

### 2.1 各层详细说明

| 层 | 文件 | 职责 |
|----|------|------|
| **Config** | `RedisConfig` | 配置 RedisTemplate 序列化方式 (Key→String, Value→JSON)，注册库存扣减 Lua 脚本 |
| | `RabbitMQConfig` | 声明 Topic 交换机 + 2个持久化队列 (order / notify) + 绑定关系 |
| | `WebConfig` | CORS 跨域配置，允许前端 localhost:5173 访问 |
| | `InterceptorConfig` | 注册 JWT 拦截器，除 login/register 外的 `/api/**` 均需认证 |
| | `GlobalExceptionHandler` | 全局异常捕获，统一返回 `Result` 格式 |
| **Entity** | `User`, `Goods`, `SeckillGoods`, `Order`, `SeckillOrder` | JPA 注解实体，通过 MyBatis-Plus 映射到 5 张数据库表 |
| **DTO** | `LoginDto`, `RegisterDto`, `SeckillDto` | 前端请求体校验 (Jakarta Validation) |
| **VO** | `GoodsVo`, `SeckillGoodsVo`, `OrderVo` | 响应体组装，可包含联表查询结果 (如 GoodsVo 联查秒杀价) |
| **Utils** | `JwtUtil` | Token 生成/校验/解析 userId |
| | `Result` | 统一响应结构 `{code, message, data}` |
| | `SnowflakeUtil` | 雪花算法生成分布式唯一ID |
| | `RedisKey` | Redis Key 命名规范集中管理 |
| **MQ** | `SeckillConsumer` | 监听 `seckill.order.queue`，异步调用 `SeckillService.createOrder()` |

---

## 3. 请求工作流

### 3.1 认证流程 (注册/登录)

```
┌──────┐     POST /api/user/register     ┌──────────────┐     INSERT      ┌───────┐
│ 前端  │ ──────────────────────────────→ │ UserController│ ──────────────→ │ MySQL │
│      │                                  │  → UserService │               │t_user │
│      │ ←────────── {code:200} ──────── │                │ ←──────────── │       │
└──────┘                                  └──────────────┘               └───────┘

┌──────┐     POST /api/user/login        ┌──────────────┐    SELECT       ┌───────┐
│ 前端  │ ──────────────────────────────→ │ UserController│ ──────────────→ │ MySQL │
│      │                                  │  → UserService │   BCrypt比对   │t_user │
│      │ ←──── {code:200, data:token} ── │                │ ←──────────── │       │
└──────┘                                  └──────┘────────┘               └───────┘
                                           │
                                    JwtUtil.generateToken(userId)
                                           │
                                    Token存前端 localStorage
                                    后续请求携带 Header: Authorization: Bearer {token}
```

> **关键点**: BCrypt 加密存储密码，JWT 24小时过期。注册/登录是唯一两个无需拦截的 `/api/**` 接口 (`InterceptorConfig` 中配置 exclusion)。

### 3.2 认证拦截流程

```
请求进入 → JwtInterceptor.preHandle()
              │
              ├─ Header 无 Authorization? → 返回 401 "未登录"
              ├─ Token 格式非 "Bearer xxx"? → 返回 401
              ├─ Token 过期/无效?          → 返回 401 "Token已过期"
              └─ Token 校验通过
                   │
                   └─ 解析 userId → request.setAttribute("userId", userId)
                      后续 Controller 通过 request.getAttribute("userId") 获取
```

---

## 4. 秒杀核心链路 (最关键的工作流)

这是整个系统最核心的流程，实现了 Redis + RabbitMQ + MySQL 三阶段架构：

```
第1阶段: 获取秒杀路径          第2阶段: 执行秒杀            第3阶段: 异步下单 + 轮询结果
(防刷)                       (Redis原子扣减 + MQ)        (DB持久化 + 响应前端)
══════════════════          ══════════════════          ════════════════════════

┌──────┐                    ┌──────┐                    ┌──────┐
│ 前端  │                    │ 前端  │                    │ 前端  │
│      │ 1. GET /seckill/   │      │ 3. POST /seckill/  │      │ 6. 轮询 GET /seckill/
│      │    path?goodsId=1  │      │    {path}/execute   │      │    result/{goodsId}
└──┬───┘                    └──┬───┘                    └──┬───┘
   │                           │                           │
   ▼                           ▼                           │ 每1秒一次
┌──────────────┐          ┌──────────────┐             ┌──┴────────────┐
│SeckillController│        │SeckillController│           │SeckillController│
│  getPath()    │          │  execute()   │           │  result()     │
└──────┬───────┘          └──────┬───────┘           └──────┬───────┘
       │                         │                          │
       ▼                         ▼                          ▼
┌──────────────┐          ┌──────────────┐          ┌──────────────┐
│SeckillService │          │SeckillService │          │SeckillService │
│getSeckillPath │          │ doSeckill()  │          │getSeckillResult│
└──────┬───────┘          └──────┬───────┘          └──────┬───────┘
       │                         │                          │
       │ ①校验秒杀商品是否存在     │ ①校验path是否匹配         │ ①查t_seckill_order
       │   时间是否有效            │   (从Redis取+删除)        │   该用户是否存在订单
       │                         │                          │
       │ ②生成随机path            │ ②执行Lua脚本:             │ ②查Redis库存
       │   path = MD5(UUID)      │   - SISMEMBER 查重复     │   判断是否已售罄
       │                         │   - GET 查库存           │
       │ ③Redis存入:              │   - DECR 扣库存          │
       │   seckill:path:uid:gid  │   - SADD 标记用户        │
       │   TTL=60秒               │   ┌─────────────┐       │
       │                         │   │ 返回1:成功    │       │
       │ ④返回path给前端           │   │ 返回-1:重复  │       │
       └─────────────────────────┘   │ 返回-2:售罄  │       │
                                     └──────┬──────┘       │
                                            │ 返回1         │
                                            ▼              │
                                     ┌──────────────┐      │
                                     │ RabbitMQ     │      │
                                     │ 发送消息到    │      │
                                     │ seckill.order │      │
                                     │ .queue       │      │
                                     └──────┬───────┘      │
                                            │              │
                                            ▼              │
                          ┌─────────────────────────────┐  │
                          │ SeckillConsumer.handleOrder │  │
                          │  监听 seckill.order.queue    │  │
                          └─────────────┬───────────────┘  │
                                        │                  │
                                        ▼                  │
                          ┌─────────────────────────────┐  │
                          │ SeckillService.createOrder  │  │
                          │  @Transactional              │  │
                          │                              │  │
                          │ ①再次校验MySQL库存(防超卖)     │  │
                          │ ②UPDATE stock_count-1 (乐观锁)│  │
                          │ ③INSERT t_order              │  │
                          │ ④INSERT t_seckill_order ─────┼──┘
                          └─────────────────────────────┘    (前端轮询检测到
                                                               记录存在 → 成功)
```

### 4.1 各阶段详解

#### 阶段一: `GET /api/seckill/path?goodsId=1` — 获取秒杀地址

```
目的: 防止脚本直接调用秒杀接口
实现:
  1. 从 MySQL 查询 SeckillGoods，校验活动是否在有效时间内
  2. MD5(UUID) 生成一个随机字符串作为 path
  3. path 写入 Redis: seckill:path:{userId}:{goodsId} = path, TTL=60秒
  4. 返回 path 给前端

安全策略:
  - path 与用户+商品绑定，一用户一地址
  - 有效期60秒，超时自动失效
  - 下次秒杀需重新获取
```

#### 阶段二: `POST /api/seckill/{path}/execute` — 执行秒杀

```
目的: 核心秒杀逻辑，在Redis中原子完成库存扣减，快速响应用户
实现:
  1. 从 Redis 取出 path，与请求中的 path 比对
  2. 比对成功则删除该path (防止同一path复用)
  3. 执行 stock_deduction.lua 脚本，在Redis中原子完成:
     - 查重: SISMEMBER seckill:uid:{goodsId} userId
     - 查库存: GET seckill:stock:{goodsId}
     - 扣减: DECR + SADD
     - 返回值: 1=成功 / -1=重复 / -2=售罄
  4. Lua成功 → 发送MQ消息 (userId + goodsId) 到 seckill.order.queue
  5. 立即返回 0 给前端 (表示"排队中")
```

#### 阶段三: 异步 + 轮询

```
异步路径 (MQ → Consumer → MySQL):
  SeckillConsumer 收到消息:
    → SeckillService.createOrder(userId, goodsId) — @Transactional 事务保证
      → UPDATE t_seckill_goods SET stock_count = stock_count - 1 WHERE goods_id=? AND stock_count > 0
        (MySQL乐观锁, 二次防超卖)
      → INSERT t_order (雪花ID)
      → INSERT t_seckill_order (unique(uid, gid) 唯一约束防重)

轮询路径 (前端 → 后端):
  前端每1秒 GET /api/seckill/result/{goodsId}
    → SeckillService.getSeckillResult():
      → 查 t_seckill_order WHERE user_id=? AND goods_id=?
      → 有记录 → 返回 orderId (>0) → 前端显示"秒杀成功"
      → 无记录 + Redis库存<=0 + 用户不在Set中 → 返回 -1 → 前端显示"已售罄"
      → 否则 → 返回 0 → 前端继续轮询
```

### 4.2 Redis Key 使用全景

| Redis Key | 类型 | 生命周期 | 写入时机 | 读取时机 |
|-----------|------|---------|---------|---------|
| `seckill:stock:{goodsId}` | String (整数) | 活动期间 | 活动预热时初始化 | Lua脚本扣减、结果查询 |
| `seckill:uid:{goodsId}` | Set | 活动期间 | Lua脚本SADD | Lua去重、售罄判断 |
| `seckill:goods:{goodsId}` | String (JSON) | 10分钟过期 | 首次查询商品详情时缓存 | GoodsService.getDetail |
| `seckill:path:{uid}:{gid}` | String | 60秒过期 | getSeckillPath() | doSeckill()校验+删除 |
| `lock:order:{uid}:{gid}` | String | 30秒 | (预留分布式锁, 未启用) | — |

### 4.3 RabbitMQ 消息流

```
                       seckill.topic (Topic Exchange)
                              │
              ┌───────────────┴───────────────┐
              │                               │
   routing_key: seckill.order       routing_key: seckill.notify
              │                               │
              ▼                               ▼
   ┌────────────────────┐        ┌────────────────────┐
   │ seckill.order.queue│        │ seckill.notify.queue│
   │ (持久化)            │        │ (持久化, 预留)       │
   ├────────────────────┤        ├────────────────────┤
   │ Consumer:          │        │ Consumer:          │
   │ SeckillConsumer    │        │ Python Agent (未来) │
   │ → createOrder()    │        │ → 风控/推荐/通知    │
   │ 手动ACK + 失败重试  │        │                    │
   └────────────────────┘        └────────────────────┘
```

> **可靠性保证**: 消息/队列均持久化 + 生产者 Publisher Confirm + 消费者手动ACK + 本地重试3次

---

## 5. 数据库表关系

```
t_user ──1:N──→ t_order ──N:1──→ t_goods
  │                │                  │
  │                │                  │ 1:1
  │                │                  ▼
  │                │           t_seckill_goods
  │                │
  │                └──────────────┐
  │                               │
  └────1:N──→ t_seckill_order ←───┘
              UNIQUE(user_id, goods_id)
```

| 表 | 主键策略 | 核心索引 | 说明 |
|----|---------|---------|------|
| `t_user` | 雪花算法 | `uk_phone` | 唯一手机号 |
| `t_goods` | 自增 | — | 商品主表 |
| `t_seckill_goods` | 自增 | `idx_start_end` | 秒杀活动时间范围索引 |
| `t_order` | 雪花算法 | `idx_user_id` | 普通订单 |
| `t_seckill_order` | 雪花算法 | `uk_uid_gid` | 唯一约束防重复秒杀 |

---

## 6. 前端状态机

秒杀详情页 (`Detail.vue`) 维护一个 **5态状态机**：

```
                  ┌─────────┐
    用户点秒杀 →   │  ready   │  初始状态，"立即秒杀"按钮可见
                  └────┬─────┘
                       │ executeSeckill() 成功返回
                       ▼
                  ┌──────────┐
                  │ queuing  │  显示"排队中..."标签
                  └────┬─────┘
                       │ 轮询 getSeckillResult()
           ┌───────────┼───────────┐
           ▼           ▼           ▼
      ┌─────────┐ ┌──────────┐ ┌──────────┐
      │ success │ │ soldout  │ │  fail    │
      │ "恭喜!" │ │ "已售罄" │ │ "失败了" │
      └─────────┘ └──────────┘ └──────────┘
      orderId>0    orderId==-1   异常/超时
```

轮询逻辑: 每 1 秒调用 `/api/seckill/result/{goodsId}`，直到获得确定结果（成功或售罄）。

---

## 7. 关键设计决策

| 决策 | 为什么 |
|------|--------|
| **Lua 脚本扣库存而非 Java 代码** | 保证「查库存→扣减→标记用户」三个操作的原子性，避免网络往返带来的竞态条件 |
| **先扣 Redis 再异步写 MySQL** | Redis 单线程模型使库存扣减极快（单次操作微秒级），用户无需等待 DB 写入即可得到反馈 |
| **MySQL 做二次校验** | Redis 和 DB 之间无分布式事务，Consumer 中用 `WHERE stock_count > 0` 乐观锁防御极端情况下的超卖 |
| **动态秒杀路径** | 防止脚本直接调用秒杀接口；path 与 userId 绑定 + 60秒过期 + 用完即删 |
| **雪花算法ID** | 分布式唯一 ID，不依赖 MySQL 自增，支撑后续分库分表 |
| **JWT 拦截器而非 Spring Security** | 项目规模适中，JWT + 自定义拦截器更轻量，少一层抽象 |
| **手动ACK而非自动ACK** | 订单创建涉及多表写入，手动ACK确保消息处理成功后才会从队列移除 |

---

## 8. 后续迭代方向 (Phase 2+)

当前 Phase 1 已完成的核心链路覆盖了「能跑通」的完整秒杀场景。后续计划：

1. **库存预热**: 活动开始时自动将 MySQL 库存加载到 Redis (可加 Spring `@Scheduled` 定时任务)
2. **接口限流**: `Interceptor` 中增加 Redis 滑动窗口限流 (已有 `limit:user:{uid}:{api}` key 规范预留)
3. **验证码**: 秒杀前弹出数学验证码防机器刷单
4. **Python Agent**: 消费 `seckill.notify.queue` 实现风控 + 用户画像 + 推荐
5. **订单支付**: 支付回调 + 超时取消 (死信队列实现延迟消息)
6. **压力测试**: JMeter 脚本 + 性能调优
