# 项目缺失功能与优化分析

## 概览

对后端 45 个 Java 文件、前端 15 个 Vue/JS 文件、SQL/配置/Docker 文件进行了全面审查。

**状态说明**: ✅ 已修复 | ⚠️ 部分修复/可选 | ❌ 未开始

---

## 一、致命问题（系统无法正常工作）

### 1.1 Redis 库存未初始化 ✅

**文件**: `SeckillServiceImpl.java` / `stock_deduction.lua`

**修复**: 新建 `config/StockInitializer.java`，`@PostConstruct` 时从 `t_seckill_goods` 同步库存到 Redis `seckill:stock:{goodsId}`，且仅在 key 不存在时写入，避免覆盖运行时库存。

### 1.2 MySQL 库存扣减无行锁 ✅

**文件**: `SeckillServiceImpl.java:createOrder()`

**修复**: 
- `SeckillGoodsMapper` 新增 `selectForUpdate(goodsId)` 方法（SELECT ... FOR UPDATE）
- `createOrder()` 改为使用行锁查询后再扣减库存
- 在扣减前增加幂等性校验：检查 `t_seckill_order` 是否已存在相同 userId + goodsId 记录，防止 MQ 重复消费

---

## 二、安全问题

### 2.1 密钥明文暴露 ✅

`application.yml` 中所有密码和 JWT Secret 已改为 `${ENV_VAR:default}` 环境变量占位符，真实值通过 `.env` 文件管理（已加入 `.gitignore`）。

### 2.2 前端路由无守卫 ✅

`router/index.js` 已配置 `beforeEach` 守卫，未登录用户访问需登录页面时自动跳转 `/login`。

### 2.3 订单越权访问 ✅

**修复**: `OrderService.getDetail()` 签名改为 `getDetail(Long userId, Long orderId)`，校验订单是否属于当前用户。

### 2.4 输入校验缺失 ✅

| DTO | 修复 |
|-----|------|
| `RegisterDto` | 添加 `@Pattern(regexp = "^1[3-9]\\d{9}$")` 手机号校验、`@Size(min=6, max=20)` 密码长度 |
| `SeckillDto` | 添加 `@NotNull(message = "商品ID不能为空")` |
| `SeckillController.execute()` | 添加 `@Valid` 注解 |

### 2.5 错误信息泄漏 ⚠️

`GlobalExceptionHandler` 将 `RuntimeException.getMessage()` 直接返回客户端。对于练习项目当前可接受，生产环境建议区分内部错误和用户可见错误。

### 2.6 Token 存储在 localStorage ⚠️

存在 XSS 风险。练习项目可接受，生产环境应使用 httpOnly Cookie。

### 2.7 限流功能未实现 ❌

`RedisKey.java` 定义了 `LIMIT_USER_API` 和 `LOCK_ORDER`，但无实际限流代码。属于 P4 优先级功能。

---

## 三、功能缺失

### 3.1 购物车 — 无后端、无持久化 ❌

需要新增购物车表 + CRUD API + 前端持久化，工作量约 0.5 天。

### 3.2 用户信息接口缺失 ✅

**修复**: 新增 `GET /api/user/info` 端点，返回 `UserVo`（id/phone/nickname/registerTime/lastLoginTime）。前端 `Profile.vue` 改为从 API 获取用户信息。

### 3.3 消息通知 — 全部硬编码 ❌

`Messages.vue` 的通知是 HTML 写死的，`notifyQueue` 无消费者。需要新增通知表 + 消费者 + API，工作量约 0.5 天。

### 3.4 支付功能缺失 ❌

无 `POST /api/order/pay` 端点，也无支付回调。属于后期功能。

### 3.5 订单详情页缺失 ✅

**修复**: 新增 `OrderDetail.vue` + 路由 `/order/detail/:id`，展示订单信息、状态、支持取消操作。

### 3.6 重置密码缺失 ❌

无修改密码、忘记密码功能。属于后期功能。

---

## 四、数据完整性

### 4.1 取消订单恢复库存 ✅

**修复**: `OrderServiceImpl.cancel()` 现在同时恢复 MySQL 库存（`stock_count + 1`）和 Redis `seckill:stock:{goodsId}`（INCR）。

### 4.2 无订单超时自动取消 ❌

需要定时任务扫描超时待支付订单。

### 4.3 MQ 消费者幂等性 ✅

**修复**: `createOrder()` 在扣库存前先查 `t_seckill_order` 是否已有记录，重复消息直接跳过。

---

## 五、性能问题

### 5.1 N+1 查询 ✅

**修复**: `GoodsMapper` 新增 `getDetailByIds(List<Long> ids)` 批量查询方法，`listSeckillGoods()` 改为先批量查出所有 GoodsVo 再组装结果，从 N 次 DB 查询降为 2 次。

### 5.2 全量返回无分页 ⚠️

`/api/goods/list`、`/api/goods/seckill`、`/api/order/list` 仍无分页。练习项目数据量小可接受，生产需添加。

### 5.3 SQL 日志输出到 stdout ✅

**修复**: `application.yml` 中 `mybatis-plus.log-impl` 从 `StdOutImpl` 改为 `Slf4jImpl`，日志走 SLF4J 可按级别控制。

### 5.4 Redis 序列化冗余 ⚠️

`GenericJackson2JsonRedisSerializer` 存储完整 Java 类元数据，数据膨胀约一倍。生产环境建议用 `StringRedisSerializer` + 手动序列化。

### 5.5 前端轮询无退避 ✅

**修复**: `Detail.vue` 秒杀结果轮询从固定 1 秒改为指数退避（1s → 2s → 4s → ... → 10s 上限），使用 `setTimeout` 递归替代 `setInterval`。

### 5.6 倒计时组件重复计算 ✅

**修复**: `Countdown.vue` 将 3 个独立 computed（h/m/s）合并为 1 个 computed 返回 `{ status, h, m, s }`，同时预计算 `startTs`/`endTs` 避免每秒构造 `new Date()`。

---

## 六、运维与可观测性

| 缺失项 | 状态 | 说明 |
|--------|------|------|
| Spring Boot Actuator | ❌ | 无 `/actuator/health`、`/actuator/metrics` |
| 结构化日志 | ❌ | 无 Trace ID、MDC 上下文、JSON 日志格式 |
| 死信队列 | ❌ | MQ 配置中未实现 |
| Dockerfile | ❌ | 前后端无容器化构建文件 |
| CI/CD | ❌ | 无 GitHub Actions / Jenkins |
| 优雅停机 | ❌ | 未配置 `server.shutdown=graceful` |
| 测试 | ❌ | 零测试文件 |

---

## 七、本次修复汇总

### 已修复（14 项）

| # | 问题 | 涉及文件 |
|---|------|---------|
| 1 | Redis 库存预热 | 新增 `StockInitializer.java` |
| 2 | MySQL 库存扣减加锁 + 幂等 | `SeckillServiceImpl`, `SeckillGoodsMapper` |
| 3 | 前端路由守卫 | `router/index.js`（已存在） |
| 4 | 订单越权校验 | `OrderService`, `OrderServiceImpl`, `OrderController` |
| 5 | 取消订单恢复库存 | `OrderServiceImpl` |
| 6 | DTO 校验 | `RegisterDto`, `SeckillDto`, `SeckillController` |
| 7 | 敏感配置环境变量化 | `application.yml`, `.env`, `SeckillApplication` |
| 8 | GET /api/user/info | `UserVo`, `UserService`, `UserController`, `Profile.vue`, `user.js` |
| 9 | 订单详情页 | 新增 `OrderDetail.vue` + 路由 |
| 10 | MQ 幂等 | `SeckillServiceImpl.createOrder()` |
| 11 | N+1 查询 → 批量 JOIN | `GoodsMapper`, `GoodsServiceImpl` |
| 12 | SQL 日志 Slf4jImpl | `application.yml` |
| 13 | 轮询指数退避 | `Detail.vue` |
| 14 | 倒计时性能优化 | `Countdown.vue` |

### 未修复（13 项）

| # | 问题 | 原因 |
|---|------|------|
| 1 | 购物车后端 | 需新增表 + 整套 CRUD，P4 优先级 |
| 2 | 消息通知 API | 需新增表 + 消费者 + API |
| 3 | 支付功能 | 完整支付接入，超出当前范围 |
| 4 | 重置密码 | 后期功能 |
| 5 | 订单超时取消 | 需定时任务 |
| 6 | 限流功能 | 需新增拦截器 |
| 7 | 列表分页 | 练习项目数据量小可接受 |
| 8 | 错误信息泄漏 | 练习项目可接受 |
| 9 | Token localStorage | 练习项目可接受 |
| 10 | Redis 序列化冗余 | 练习项目可接受 |
| 11 | Actuator / 健康检查 | 基础设施 |
| 12 | Dockerfile | 基础设施 |
| 13 | 测试 | 后期 |
