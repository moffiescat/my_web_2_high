# 高并发秒杀系统

## 技术栈

| 层次 | 技术 |
|------|------|
| 后端 | Spring Boot 3.2 + JDK 17 + MyBatis-Plus |
| 前端 | Vue 3 + Vite + Element Plus |
| 数据库 | MySQL 8.0 |
| 缓存 | Redis 7.x |
| 消息队列 | RabbitMQ 3.x |
| Python Agent | FastAPI (计划) |

## 快速启动

### 1. 启动中间件
```bash
docker-compose up -d
```

### 2. 启动后端
```bash
cd backend
mvn spring-boot:run
```

### 3. 启动前端
```bash
cd frontend
npm install
npm run dev
```

### 4. 访问
- 前端: http://localhost:5173
- 后端API文档: http://localhost:8080/doc.html
- RabbitMQ管理: http://localhost:15672 (admin/admin123)

## 核心设计

**防超卖**: Redis Lua 脚本原子扣减库存
**流量削峰**: RabbitMQ 异步下单
**接口防刷**: 动态秒杀路径 + JWT认证 + 限流

详见 [DESIGN.md](./DESIGN.md)
