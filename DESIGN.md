# 高并发秒杀系统设计文档

## 1. 项目概述

### 1.1 项目目标
构建一个支持高并发的秒杀电商系统，核心技术挑战包括：
- 高并发下的库存扣减（防止超卖）
- 接口防刷与限流
- 流量削峰填谷
- 分布式事务一致性
- 系统高可用与可扩展

### 1.2 核心功能
- 用户注册/登录（JWT 认证）
- 商品列表与详情展示
- 秒杀活动管理（后台）
- 秒杀抢购（核心链路）
- 订单查询与支付
- 秒杀结果异步通知

---

## 2. 技术选型

| 层次 | 技术 | 说明 |
|------|------|------|
| **后端框架** | Spring Boot 3.x + JDK 17 | 主应用框架 |
| **前端框架** | Vue 3 + Vite + Element Plus | 前后端分离 |
| **数据库** | MySQL 8.0 | 持久化存储 |
| **缓存** | Redis 7.x (Lettuce 客户端) | 库存预热、分布式锁、限流 |
| **消息队列** | RabbitMQ 3.x | 异步下单、流量削峰 |
| **ORM** | MyBatis-Plus | 数据库访问 |
| **连接池** | HikariCP (Spring Boot 默认) | 数据库连接池 |
| **API文档** | Knife4j / SpringDoc | Swagger 文档 |
| **压力测试** | JMeter | 性能压测 |
| **Python Agent** | FastAPI + Redis + RabbitMQ | 计划阶段：智能风控/推荐/数据分析 |

---

## 3. 系统架构

```
┌─────────────────────────────────────────────────────────┐
│                       Nginx (反向代理 + 静态资源)          │
└────────┬──────────────────────────────────────┬─────────┘
         │                                      │
         ▼                                      ▼
┌─────────────────┐                   ┌──────────────────┐
│   Vue 3 前端     │                   │  Spring Boot 后端 │
│   (Vite 构建)    │                   │  (API 服务)       │
│   端口: 5173     │                   │  端口: 8080       │
└─────────────────┘                   └────────┬─────────┘
                                               │
                    ┌──────────────────────────┼──────────────────────────┐
                    │                          │                          │
                    ▼                          ▼                          ▼
            ┌──────────────┐          ┌──────────────┐          ┌──────────────┐
            │    MySQL      │          │    Redis      │          │   RabbitMQ   │
            │  (持久化存储)   │          │  (缓存/锁/限流) │          │  (消息队列)   │
            │  端口: 3306    │          │  端口: 6379    │          │  端口: 5672   │
            └──────────────┘          └──────────────┘          └──────┬───────┘
                                                                      │
                                                                      ▼
                                                              ┌──────────────┐
                                                              │  Python Agent │
                                                              │  (未来组件)    │
                                                              │  端口: 8000   │
                                                              └──────────────┘
```

---

## 4. 模块划分

```
my-web-2-high/
├── frontend/                    # 前端项目 (Vue 3)
│   ├── src/
│   │   ├── views/               # 页面组件
│   │   │   ├── Home.vue         # 首页/商品列表
│   │   │   ├── Seckill.vue      # 秒杀活动页
│   │   │   ├── Detail.vue       # 商品详情
│   │   │   ├── Order.vue        # 订单管理
│   │   │   ├── Login.vue        # 登录
│   │   │   └── Register.vue     # 注册
│   │   ├── components/          # 公共组件
│   │   ├── api/                 # API 请求封装
│   │   ├── router/              # 路由配置
│   │   ├── store/               # Pinia 状态管理
│   │   └── utils/               # 工具函数
│   └── package.json
│
├── backend/                     # 后端项目 (Spring Boot)
│   ├── src/main/java/com/seckill/
│   │   ├── controller/          # 控制器层
│   │   │   ├── UserController.java
│   │   │   ├── GoodsController.java
│   │   │   ├── SeckillController.java
│   │   │   └── OrderController.java
│   │   ├── service/             # 业务逻辑层
│   │   │   ├── UserService.java
│   │   │   ├── GoodsService.java
│   │   │   ├── SeckillService.java
│   │   │   └── OrderService.java
│   │   ├── mapper/              # MyBatis-Plus Mapper
│   │   ├── entity/              # 实体类
│   │   ├── dto/                 # 数据传输对象
│   │   ├── vo/                  # 视图对象
│   │   ├── config/              # 配置类
│   │   │   ├── RedisConfig.java
│   │   │   ├── RabbitMQConfig.java
│   │   │   ├── WebConfig.java
│   │   │   └── SwaggerConfig.java
│   │   ├── mq/                  # RabbitMQ 相关
│   │   │   ├── SeckillMessageProducer.java
│   │   │   └── SeckillMessageConsumer.java
│   │   ├── utils/               # 工具类
│   │   ├── interceptor/         # 拦截器 (JWT 认证、限流)
│   │   └── SeckillApplication.java
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   ├── application-dev.yml
│   │   └── application-prod.yml
│   └── pom.xml
│
├── agent/                       # Python Agent (计划阶段)
│   ├── main.py                  # FastAPI 入口
│   ├── consumer.py              # RabbitMQ 消费者
│   ├── models/                  # ML 模型
│   └── requirements.txt
│
├── sql/                         # 数据库初始化脚本
│   └── init.sql
│
├── docker-compose.yml           # 中间件容器编排
├── DESIGN.md                    # 本设计文档
└── README.md
```

---

## 5. 数据库设计

### 5.1 ER 图概要

```
t_user (用户表)
  └──1:N── t_order (订单表)
              └──N:1── t_goods (商品表)
                          └──1:1── t_seckill_goods (秒杀商品表)
t_seckill_order (秒杀订单表, 与 t_order 1:1)
```

### 5.2 核心表结构

#### t_user — 用户表
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 用户ID (雪花算法) |
| nickname | VARCHAR(32) | 昵称 |
| password | VARCHAR(128) | 密码 (BCrypt) |
| salt | VARCHAR(32) | 盐值 |
| phone | VARCHAR(20) | 手机号 |
| register_time | DATETIME | 注册时间 |
| last_login_time | DATETIME | 最后登录时间 |

#### t_goods — 商品表
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 商品ID |
| goods_name | VARCHAR(64) | 商品名称 |
| goods_title | VARCHAR(128) | 商品标题 |
| goods_img | VARCHAR(255) | 商品图片URL |
| goods_price | DECIMAL(10,2) | 原价 |
| goods_stock | INT | 库存数量 |
| goods_detail | TEXT | 商品详情 |

#### t_seckill_goods — 秒杀商品表
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 秒杀商品ID |
| goods_id | BIGINT FK | 商品ID |
| seckill_price | DECIMAL(10,2) | 秒杀价 |
| stock_count | INT | 秒杀库存 |
| start_time | DATETIME | 秒杀开始时间 |
| end_time | DATETIME | 秒杀结束时间 |

#### t_order — 订单表
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 订单ID (雪花算法) |
| user_id | BIGINT FK | 用户ID |
| goods_id | BIGINT FK | 商品ID |
| goods_name | VARCHAR(64) | 商品名 (冗余) |
| goods_price | DECIMAL(10,2) | 成交价 |
| status | TINYINT | 0:待支付 1:已支付 2:已取消 |
| create_time | DATETIME | 创建时间 |
| pay_time | DATETIME | 支付时间 |

#### t_seckill_order — 秒杀订单表
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 秒杀订单ID |
| user_id | BIGINT | 用户ID |
| order_id | BIGINT FK | 关联普通订单ID |
| goods_id | BIGINT FK | 商品ID |

---

## 6. 核心业务流程

### 6.1 秒杀抢购完整链路

```
     用户点击秒杀
          │
          ▼
┌─────────────────┐
│ 1. 限流校验       │ ← 令牌桶/滑动窗口 (Redis)
│    (接口防刷)     │
└────────┬────────┘
         │ 通过
         ▼
┌─────────────────┐
│ 2. 秒杀资格校验   │ ← Redis 判断: 是否已抢过? 活动是否开始/结束?
│    (Redis 预检)  │
└────────┬────────┘
         │ 通过
         ▼
┌─────────────────┐
│ 3. Redis 预减库存 │ ← Lua 脚本原子操作: DECR stock
│    (核心! 防超卖) │   库存 < 0 则直接返回"已售罄"
└────────┬────────┘
         │ 成功
         ▼
┌─────────────────┐
│ 4. 发送MQ消息    │ ← 将 (userId, goodsId) 投递到 RabbitMQ
│    (异步下单)    │   快速响应用户"排队中"
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ 5. MQ消费者      │ ← 从队列拉取消息
│    异步创建订单   │   - 再次校验库存 (MySQL)
│                  │   - 写入订单表 → 秒杀订单表
│                  │   - 扣减MySQL库存
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ 6. 通知用户      │ ← WebSocket / SSE / 轮询
│    秒杀结果      │   告知用户秒杀成功或失败
└─────────────────┘
```

### 6.2 库存扣减 Lua 脚本（核心）

```lua
-- Redis Lua 脚本: 原子性库存扣减
local key = KEYS[1]          -- seckill:stock:{goodsId}
local uidKey = KEYS[2]       -- seckill:uid:{goodsId}
local userId = ARGV[1]

-- 检查是否已抢过
if redis.call('SISMEMBER', uidKey, userId) == 1 then
    return -1  -- 重复抢购
end

-- 检查库存
local stock = tonumber(redis.call('GET', key) or "0")
if stock <= 0 then
    return -2  -- 库存不足
end

-- 扣减库存 + 标记用户
redis.call('DECR', key)
redis.call('SADD', uidKey, userId)
return 1  -- 抢购成功
```

### 6.3 缓存策略

```
┌───────────────────────────────────────┐
│              缓存分层策略               │
├───────────┬───────────────┬───────────┤
│   层级     │    存储内容    │   过期策略  │
├───────────┼───────────────┼───────────┤
│ 页面缓存   │ 商品列表HTML   │ 1分钟     │
│ 对象缓存   │ 商品详情(JSON) │ 10分钟    │
│ 热点数据   │ 秒杀库存数     │ 不过期    │
│ 用户标记   │ 已抢购用户集合  │ 不过期    │
│ 分布式锁   │ 创建订单锁     │ 30秒      │
└───────────┴───────────────┴───────────┘

缓存穿透: 布隆过滤器预判商品是否存在
缓存击穿: 热点数据永不过期 + 互斥锁
缓存雪崩: 过期时间加随机值 + 多级缓存
```

### 6.4 Redis Key 设计规范

```
seckill:stock:{goodsId}              → 秒杀商品库存 (预热时写入)
seckill:uid:{goodsId}                → 已抢购用户集合 (Set)
seckill:goods:{goodsId}              → 秒杀商品详情缓存
goods:list:{page}                    → 商品列表分页缓存
user:token:{token}                   → JWT token 黑名单
limit:user:{userId}:{api}            → 接口限流计数器
lock:order:{userId}:{goodsId}        → 下单分布式锁
```

---

## 7. RabbitMQ 设计

### 7.1 交换机与队列

```
┌──────────────────────────────────────────────────────┐
│                   Exchange: seckill.topic             │
│                      (topic 模式)                     │
└─────────┬────────────────────────┬──────────────────┘
          │                        │
   routing_key:               routing_key:
   seckill.order              seckill.notify
          │                        │
          ▼                        ▼
┌──────────────────┐     ┌──────────────────┐
│ Queue:           │     │ Queue:           │
│ seckill.order    │     │ seckill.notify   │
│ .queue           │     │ .queue           │
├──────────────────┤     ├──────────────────┤
│ Consumer:        │     │ Consumer:        │
│ 创建订单(Java)    │     │ 推送通知(Java/   │
│ 持久化到MySQL     │     │ Python Agent)    │
└──────────────────┘     └──────────────────┘
```

### 7.2 消息可靠性保证

| 机制 | 说明 |
|------|------|
| **生产者确认** | Publisher Confirm 模式，确保消息到达 Broker |
| **消费者手动ACK** | `spring.rabbitmq.listener.simple.acknowledge-mode: manual` |
| **消息持久化** | Queue + Message 均持久化到磁盘 |
| **失败重试** | 本地重试3次 → 死信队列 → 人工处理 |

---

## 8. API 设计

### 8.1 接口概览

```
├── /api/user
│   ├── POST   /register          # 注册
│   └── POST   /login             # 登录
│
├── /api/goods
│   ├── GET    /list              # 商品列表 (分页)
│   └── GET    /detail/{id}       # 商品详情
│
├── /api/seckill
│   ├── GET    /path              # 获取秒杀地址 (动态URL隐藏)
│   ├── POST   /{path}/execute    # 执行秒杀
│   └── GET    /result/{goodsId}  # 查询秒杀结果 (轮询)
│
├── /api/order
│   ├── GET    /list              # 我的订单列表
│   ├── GET    /detail/{orderId}  # 订单详情
│   └── POST   /cancel/{orderId}  # 取消订单
```

### 8.2 关键接口详情

#### GET /api/seckill/path
获取隐藏的秒杀地址，防止脚本刷单。
```
Request Header: Authorization: Bearer {jwt}
Request Param:  goodsId
Response:       { "code": 0, "data": { "path": "abc123def456" } }
```
→ 地址由服务端根据 goodsId + 用户ID + 随机盐 生成MD5，60秒有效，存入 Redis

#### POST /api/seckill/{path}/execute
执行秒杀（path 由上一接口获得）。
```
Request Header: Authorization: Bearer {jwt}
Request Body:   { "goodsId": 123 }
Response:       { "code": 0, "message": "排队中" }
             或 { "code": 403, "message": "商品已售罄" }
             或 { "code": 403, "message": "请勿重复抢购" }
```

---

## 9. 安全与限流

### 9.1 多层限流策略

```
第一层: Nginx    → 限制单IP QPS (limit_req_zone)
第二层: 网关     → 令牌桶限流 (如需要可引入 Gateway)
第三层: 接口层   → Redis 滑动窗口 + 注解 (@RateLimiter)
第四层: 业务层   → 用户维度限制 (同一秒杀商品每人限购1次)
```

### 9.2 防刷机制
- **隐藏秒杀地址**: 不暴露真实秒杀URL，动态生成随机path
- **数学验证码**: 秒杀前弹出简单算术验证码（生成与校验均在Redis中）
- **JWT认证**: 所有秒杀接口需携带有效Token
- **请求频率限制**: 单个用户每秒钟杀接口最多调用1次

---

## 10. 页面设计

### 10.1 页面清单

| 页面 | 路由 | 功能 |
|------|------|------|
| 登录页 | /login | 用户登录 |
| 注册页 | /register | 用户注册 |
| 首页 | / | 展示秒杀活动商品列表 + 倒计时 |
| 商品详情 | /detail/:id | 商品详情 + 秒杀按钮 |
| 秒杀结果 | /seckill/result/:id | 秒杀排队中/结果展示 |
| 订单列表 | /order/list | 我的订单 |
| 订单详情 | /order/detail/:id | 订单详情 + 支付 |

### 10.2 秒杀页面关键交互

```
秒杀倒计时 → 按钮状态变化:
  "即将开始" (灰色禁用)
     ↓ (倒计时归零)
  "立即秒杀" (红色高亮)
     ↓ (点击)
  验证码弹窗 (输入算式结果)
     ↓ (验证通过)
  "排队中..." (加载动画)
     ↓ (轮询结果)
  "恭喜抢到!" / "很遗憾,已售罄"
```

---

## 11. 项目启动顺序

### 11.1 Docker Compose 中间件

```yaml
# docker-compose.yml
services:
  mysql:
    image: mysql:8.0
    ports: ["3306:3306"]
    environment:
      MYSQL_ROOT_PASSWORD: root123
      MYSQL_DATABASE: seckill

  redis:
    image: redis:7-alpine
    ports: ["6379:6379"]
    command: redis-server --requirepass redis123

  rabbitmq:
    image: rabbitmq:3-management
    ports: ["5672:5672", "15672:15672"]
    environment:
      RABBITMQ_DEFAULT_USER: admin
      RABBITMQ_DEFAULT_PASS: admin123
```

### 11.2 启动步骤
```
1. docker-compose up -d                      # 启动中间件
2. 执行 sql/init.sql 初始化数据库表
3. 启动 Spring Boot (backend)                 # 端口 8080
4. 启动 Vue Dev Server (frontend)             # 端口 5173
5. (未来) 启动 Python Agent                   # 端口 8000
```

---

## 12. Python Agent 计划 (Phase 2)

### 12.1 定位
Python Agent 作为独立微服务运行，通过 RabbitMQ 消费秒杀事件，实现：
- **智能风控**: 基于用户行为特征的异常检测（刷单、机器人）
- **用户画像**: 分析用户购买偏好
- **推荐引擎**: 基于协同过滤的商品推荐
- **数据分析**: 秒杀活动效果实时分析

### 12.2 技术栈
- FastAPI (Web 框架)
- scikit-learn / XGBoost (模型)
- Redis (特征存储)
- RabbitMQ (事件消费: pika 库)

### 12.3 通信方式
```
Java 后端 → RabbitMQ → Python Agent
                          ↓
                     分析结果写入 Redis
                          ↓
Java 后端 ← Redis ← Python Agent
```

---

## 13. 性能优化要点

1. **读多写少 → 缓存为主**: 商品信息、库存数量全部通过 Redis 读取
2. **库存扣减 → Lua 原子操作**: 避免网络往返，单次操作在Redis中完成
3. **下单异步化 → MQ削峰**: 秒杀成功只做 Redis 扣减，订单创建异步完成
4. **连接池优化**: HikariCP 合理配置最大连接数；Redis 使用 Lettuce 连接池
5. **库存预热**: 秒杀活动开始前，将库存数据加载到 Redis
6. **动静分离**: 前端静态资源 CDN / Nginx 直接返回

---

## 14. 开发计划

| 阶段 | 内容 | 预计 |
|------|------|------|
| Phase 0 | 项目初始化、依赖配置、Docker中间件搭建 | — |
| Phase 1 | 用户模块 (注册/登录/JWT) + 商品模块 (CRUD) | — |
| Phase 2 | 秒杀核心链路 (Redis库存扣减 + MQ异步下单) | — |
| Phase 3 | 订单模块 + 限流防刷 | — |
| Phase 4 | 前端页面开发 | — |
| Phase 5 | Python Agent 集成 | — |
| Phase 6 | 压力测试 (JMeter) + 性能调优 | — |
