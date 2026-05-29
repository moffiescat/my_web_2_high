# CLAUDE.md - 高并发秒杀系统

## 项目概述

Spring Boot 3.2 + Vue 3 秒杀商城，核心技术栈：MyBatis-Plus / Redis / RabbitMQ / JWT / Element Plus。

## 目录结构

```
├── backend/                  # Spring Boot 后端 (端口 8080)
│   └── src/main/java/com/seckill/
│       ├── config/           # 全局异常处理、JWT拦截器、Redis/RabbitMQ配置、CORS
│       ├── controller/       # User/Goods/Seckill/Order
│       ├── dto/              # LoginDto, RegisterDto, SeckillDto
│       ├── entity/           # User, Goods, SeckillGoods, Order, SeckillOrder
│       ├── interceptor/      # JwtInterceptor (从Header取Bearer token)
│       ├── mapper/           # MyBatis-Plus BaseMapper
│       ├── mq/               # SeckillConsumer (RabbitMQ下单消费者)
│       ├── service/          # 接口 + impl 实现
│       ├── utils/            # JwtUtil, RedisKey, Result, SnowflakeUtil
│       └── vo/               # GoodsVo, OrderVo, SeckillGoodsVo
├── frontend/                 # Vue 3 + Vite 前端 (端口 5173)
│   └── src/
│       ├── api/              # axios 封装 + 各模块 API
│       ├── assets/styles/    # global.css 全局样式
│       ├── components/       # Countdown.vue 倒计时组件
│       ├── router/           # 路由配置
│       ├── store/            # Pinia 用户状态
│       └── views/            # Login, Register, Home, Detail, OrderList
├── sql/
│   └── init.sql              # MySQL 建表 + 测试数据
├── docker-compose.yml        # MySQL/Redis/RabbitMQ 容器
├── start.bat                 # 一键启动
└── stop.bat                  # 一键关闭
```

## 启动命令

```bash
# 启动 Docker 容器
docker compose up -d

# 后端
cd backend && mvn clean spring-boot:run

# 前端
cd frontend && npm run dev
```

> 批处理 `start.bat` / `stop.bat` 一键启停所有服务。

## 容器端口映射

| 服务 | 宿主机端口 | 容器端口 | 账号/密码 |
|------|-----------|---------|----------|
| MySQL 8.0 | **3307** | 3306 | root / root123 |
| Redis 7 | **6380** | 6379 | 密码 redis123 |
| RabbitMQ 3 | 5672, **15672** | 5672, 15672 | admin / admin123 |

> 注：MySQL 和 Redis 使用了非默认端口，因为本地已有实例占用 3306 和 6379。

## 数据库

- **连接**: `jdbc:mysql://localhost:3307/seckill`
- **数据库名**: `seckill`
- **用户名/密码**: `root` / `root123`
- **表**: `t_user`, `t_goods`, `t_seckill_goods`, `t_order`, `t_seckill_order`
- **ID 策略**: User 和 Order 用 Snowflake，Goods 和 SeckillGoods 用 AUTO_INCREMENT
- **init.sql**: 容器首次启动自动执行，已添加 `SET NAMES utf8mb4` 防中文乱码

## 已有依赖（pom.xml 关键项）

- `spring-boot-starter-web` — 含 Jackson JSON + 内嵌 Tomcat
- `jackson-datatype-jsr310` — Java 8 时间(LocalDateTime)序列化支持
- `spring-security-crypto` — BCrypt 密码加密
- `mybatis-plus-spring-boot3-starter:3.5.6` — 注意 `LambdaQueryWrapper` 无 `setSql()`，要用 `LambdaUpdateWrapper`
- `knife4j-openapi3-jakarta-starter:4.4.0` — API 文档 http://localhost:8080/doc.html
- `jjwt:0.12.5` — JWT 认证
- `hutool-all:5.8.27` — Snowflake ID、MD5 等工具

## API 路由

```
POST /api/user/register      # 注册
POST /api/user/login          # 登录，返回 JWT token
GET  /api/goods/list          # 全部商品
GET  /api/goods/detail/{id}   # 商品详情
GET  /api/goods/seckill       # 秒杀商品列表
GET  /api/seckill/path        # 获取秒杀路径（需登录）
POST /api/seckill/{path}/execute  # 执行秒杀（需登录）
GET  /api/seckill/result/{id} # 查询秒杀结果（需登录）
GET  /api/order/list          # 我的订单（需登录）
POST /api/order/cancel/{id}   # 取消订单（需登录）
```

> JWT 拦截：`/api/**` 除 register/login 外均需 Authorization: Bearer <token>

## 秒杀流程

```
前端 获取秒杀路径 → 执行秒杀 → Redis Lua 扣库存 → MQ 异步创建订单 → 轮询结果
```

- Lua 脚本 `stock_deduction.lua` 在 Redis 中原子扣减库存，防止超卖和重复抢购
- MQ 消费者 `SeckillConsumer` 异步写入 MySQL 订单表

## 常见问题

1. **`LocalDateTime` 序列化报错** — 确保 `jackson-datatype-jsr310` 在 pom.xml，`RedisConfig` 中 `GenericJackson2JsonRedisSerializer` 传入了含 `JavaTimeModule` 的 ObjectMapper
2. **中文乱码** — init.sql 已加 `SET NAMES utf8mb4`
3. **端口冲突** — MySQL 用 3307，Redis 用 6380，如果本机已有服务占用这些端口可继续修改
4. **`setSql` 编译错误** — MyBatis-Plus 3.5.6 的 `LambdaQueryWrapper` 没有此方法，必须用 `LambdaUpdateWrapper`
