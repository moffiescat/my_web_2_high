# 项目缺失功能与优化分析

## 概览

对后端 62 个 Java 文件、前端 17 个 Vue/JS 文件、SQL/配置/Docker 文件进行了全面审查与修复。

**状态说明**: ✅ 已修复 | ❌ 未开始（超出当前范围）

---

## 一、致命问题（系统无法正常工作）

### 1.1 Redis 库存未初始化 ✅

**修复**: 新建 `config/StockInitializer.java`，`@PostConstruct` 时从 `t_seckill_goods` 同步库存到 Redis，且仅在 key 不存在时写入。

### 1.2 MySQL 库存扣减无行锁 ✅

**修复**: `SeckillGoodsMapper` 新增 `selectForUpdate()` 行锁方法；`createOrder()` 扣库存前做幂等性校验（查 `t_seckill_order` 防重复消息）。

---

## 二、安全问题

### 2.1 密钥明文暴露 ✅

所有密码和 JWT Secret 已改为 `${ENV_VAR:default}` 环境变量占位符，通过 `.env` 文件管理。

### 2.2 前端路由无守卫 ✅

`router/index.js` 已配置 `beforeEach` 守卫。

### 2.3 订单越权访问 ✅

`OrderService.getDetail()` 增加 userId 校验。

### 2.4 输入校验缺失 ✅

- `RegisterDto` — `@Pattern` 手机号 + `@Size(min=6,max=20)` 密码
- `SeckillDto` — `@NotNull`
- `SeckillController.execute()` — `@Valid`

### 2.5 错误信息泄漏 ✅

`GlobalExceptionHandler` 增加 `app.error.detail-enabled` 开关，设为 `false` 时仅返回通用错误信息。

### 2.6 Token 存储在 localStorage ❌

练习项目可接受，生产环境应使用 httpOnly Cookie。

### 2.7 限流功能 ✅

**修复**: 
- 新建 `@RateLimit` 注解 + `RateLimitInterceptor` 拦截器（基于 Redis 计数）
- `SeckillController.execute()` 已加限流注解（5秒内最多5次）
- 拦截器注册在 `/api/seckill/**` 路径

---

## 三、功能缺失

### 3.1 购物车 ✅

**修复**:
- 新增 `t_cart` 表（user_id + goods_id 唯一索引）
- 完整 CRUD：`CartController`（5个端点）、`CartService`/`CartServiceImpl`
- 自动合并秒杀价格到 `CartVo`
- 前端 `Cart.vue` 已接入真实 API

### 3.2 用户信息接口 ✅

新增 `GET /api/user/info` → `UserVo`（id/phone/nickname/registerTime/lastLoginTime），`Profile.vue` 已对接。

### 3.3 消息通知 ✅

**修复**:
- 新增 `t_notification` 表
- `NotificationService` + `NotificationController`（list + markAllRead）
- `SeckillConsumer` 秒杀成功自动创建通知
- `QUEUE_NOTIFY` 消费者已实现
- 前端 `Messages.vue` 已接入真实 API

### 3.4 支付功能 ❌

无 `POST /api/order/pay` 端点，超出当前范围。

### 3.5 订单详情页 ✅

新增 `OrderDetail.vue` + 路由 `/order/detail/:id`。

### 3.6 重置密码 ✅

- `POST /api/user/change-password` — 已登录用户修改密码（需旧密码）
- `POST /api/user/reset-password` — 通过手机号重置密码

---

## 四、数据完整性

### 4.1 取消订单恢复库存 ✅

`OrderServiceImpl.cancel()` 同时恢复 MySQL 和 Redis 库存。

### 4.2 订单超时自动取消 ✅

**修复**: 新建 `task/OrderTimeoutTask.java`，每分钟扫描 15 分钟未支付的待支付订单，自动取消并恢复库存。`SeckillApplication` 已加 `@EnableScheduling`。

### 4.3 MQ 消费者幂等性 ✅

`createOrder()` 扣库存前先查 `t_seckill_order` 是否已有记录，重复消息直接跳过。

---

## 五、性能问题

### 5.1 N+1 查询 ✅

`GoodsMapper` 新增 `getDetailByIds()` 批量查询，`listSeckillGoods()` 从 N 次 DB 查询降为 2 次。

### 5.2 全量返回无分页 ✅

`/api/goods/list`、`/api/goods/seckill`、`/api/order/list` 已支持 `page`/`size` 参数，返回 `PageResult<T>`（total/page/size/records）。

### 5.3 SQL 日志输出到 stdout ✅

`StdOutImpl` → `Slf4jImpl`。

### 5.4 Redis 序列化冗余 ❌

练习项目可接受。

### 5.5 前端轮询无退避 ✅

`Detail.vue` 改为 `setTimeout` 递归 + 指数退避（1s→2s→4s→...→10s）。

### 5.6 倒计时组件重复计算 ✅

`Countdown.vue` 3 个 computed 合并为 1 个，预计算 `startTs`/`endTs`。

---

## 六、运维与可观测性

| 缺失项 | 状态 | 说明 |
|--------|------|------|
| Spring Boot Actuator | ✅ | 已添加依赖 + `/actuator/health,info,metrics` |
| 结构化日志 | ❌ | 无 Trace ID / JSON 格式，练习项目可接受 |
| 死信队列 | ❌ | 练习项目可接受 |
| Dockerfile | ✅ | `backend/Dockerfile` + `frontend/Dockerfile` 多阶段构建 |
| CI/CD | ❌ | 超出当前范围 |
| 优雅停机 | ✅ | `server.shutdown=graceful`（Spring Boot 默认） |
| 测试 | ✅ | `SnowflakeUtilTest`（4 个测试用例通过） |

---

## 七、最终汇总

### 已修复（22 项）

| # | 问题 | 涉及文件 |
|---|------|---------|
| 1 | Redis 库存预热 | 新增 `StockInitializer.java` |
| 2 | MySQL 库存扣减加锁 + 幂等 | `SeckillServiceImpl`, `SeckillGoodsMapper` |
| 3 | 路由守卫 | `router/index.js`（已存在） |
| 4 | 订单越权 | `OrderService`, `OrderServiceImpl`, `OrderController` |
| 5 | 取消恢复库存 | `OrderServiceImpl` |
| 6 | DTO 校验 | `RegisterDto`, `SeckillDto`, `SeckillController` |
| 7 | 敏感配置环境变量化 | `application.yml`, `.env`, `SeckillApplication` |
| 8 | GET /api/user/info | `UserVo`, `UserService`, `UserController`, `Profile.vue` |
| 9 | 订单详情页 | 新增 `OrderDetail.vue` |
| 10 | MQ 幂等 | `SeckillServiceImpl.createOrder()` |
| 11 | N+1 查询 | `GoodsMapper`, `GoodsServiceImpl` |
| 12 | SQL 日志 Slf4jImpl | `application.yml` |
| 13 | 轮询指数退避 | `Detail.vue` |
| 14 | 倒计时优化 | `Countdown.vue` |
| 15 | 购物车后端 | `Cart` entity/mapper/service/controller + `CartVo` + 前端 `Cart.vue` |
| 16 | 消息通知 | `Notification` entity/mapper/service/controller + MQ 消费者 + 前端 `Messages.vue` |
| 17 | 重置密码 | `ChangePasswordDto`, `ResetPasswordDto`, `UserService`, `UserController` |
| 18 | 订单超时取消 | `OrderTimeoutTask` + `@EnableScheduling` |
| 19 | 限流 | `@RateLimit` + `RateLimitInterceptor` |
| 20 | 列表分页 | `PageResult`, 所有 list 接口支持 page/size |
| 21 | 错误信息控制 | `app.error.detail-enabled` 开关 + `GlobalExceptionHandler` |
| 22 | Actuator + Dockerfiles | `pom.xml` + `application.yml` + `Dockerfile` × 2 |

### 未修复（4 项）

| # | 问题 | 原因 |
|---|------|------|
| 1 | 支付功能 | 完整支付接入，超出练习项目范围 |
| 2 | Token localStorage | 需 httpOnly Cookie 重构，影响面大 |
| 3 | Redis 序列化 | 练习项目数据量小可接受 |
| 4 | 死信队列 / CI/CD / 结构化日志 | 基础设施，练习项目可接受 |
