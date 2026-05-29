# 日志系统文档

## 概览

| 项目 | 说明 |
|------|------|
| 日志门面 | **SLF4J** 2.x（`org.slf4j.Logger` / `LoggerFactory`） |
| 日志实现 | **Logback**（Spring Boot Starter Web 内置 `spring-boot-starter-logging`） |
| 配置文件 | 无自定义 `logback-spring.xml`，使用 Spring Boot 默认行为 |
| 输出位置 | 控制台 stdout（未配置文件输出） |
| 默认级别 | INFO |
| MyBatis SQL | `Slf4jImpl` → 走 SLF4J 通道 |

---

## 日志流向

```
代码调用                    门面               实现               输出
─────────                  ────               ────               ────
log.info(...)    ──→    SLF4J API    ──→    Logback    ──→    控制台 stdout
log.warn(...)                                        ──→    （无文件输出）
log.error(...)
```

目前没有配置文件输出 appender。如果需要输出到文件，有两种方式：

**方式一 — application.yml 配置**（适合简单场景）：
```yaml
logging:
  level:
    root: INFO
    com.seckill: DEBUG
  file:
    path: logs/
    name: logs/seckill.log
```

**方式二 — logback-spring.xml**（适合需要归档、按级别分文件等复杂场景）：
在 `src/main/resources/` 下创建 `logback-spring.xml`，利用 Spring Boot 扩展（如 `springProfile`）。

---

## 日志级别约定

| 级别 | 使用场景 | 示例 |
|------|---------|------|
| `ERROR` | 业务异常、系统异常，需要人工关注 | 秒杀下单失败、MQ 消费异常、未知异常 |
| `WARN` | 可恢复的异常，不影响主流程 | 重复 MQ 消息被忽略 |
| `INFO` | 关键业务流程节点、启动/关闭事件 | 库存预热完成、订单超时处理、MQ 消息接收 |
| `DEBUG` | 开发调试信息（当前未使用） | — |

---

## 模块日志清单

共 **5 个类** 使用 Logger，**16 处**日志调用：

### GlobalExceptionHandler
```
log.error("业务异常: {}", e.getMessage())
log.error("系统异常: ", e)
```
- **触发**: 每次抛出 RuntimeException 或未捕获 Exception
- **用途**: 排查接口报错原因

### StockInitializer（启动时执行一次）
```
log.info("Redis 库存预热: goodsId={}, stock={}")
log.info("Redis 库存预热完成，共 {} 个秒杀商品")
```
- **触发**: Spring 容器启动时 `@PostConstruct`
- **用途**: 验证库存预热是否正常

### SeckillConsumer（MQ 消费者）
```
log.info("收到秒杀下单消息: userId={}, goodsId={}")
log.error("秒杀下单失败: {}", e.getMessage())
log.error("通知创建失败: {}", e.getMessage())
```
- **触发**: 每次消费 RabbitMQ 消息
- **用途**: 追踪下单流程、排查 MQ 异常

### OrderTimeoutTask（定时任务，每分钟）
```
log.info("超时订单处理完成，共取消 {} 单")
```
- **触发**: `@Scheduled(fixedRate = 60000)`
- **用途**: 监控超时订单自动取消数量

### SeckillServiceImpl（秒杀核心逻辑）
```
log.warn("重复消息，已忽略: userId={}, goodsId={}")
```
- **触发**: MQ 幂等性校验拦截到重复消息
- **用途**: 监控 MQ 重试/重复投递

### MyBatis-Plus SQL 日志

配置项 `mybatis-plus.configuration.log-impl: Slf4jImpl`，SQL 日志通过 **DEBUG** 级别输出到 SLF4J。由于默认级别是 INFO，**控制台不会打印 SQL**。如需查看 SQL：

```yaml
logging:
  level:
    com.seckill.mapper: DEBUG    # 开启 Mapper SQL 日志
```

---

## 如何查看日志

```
场景                      查看方式
──────────────────────────────────────────────
mvn spring-boot:run      终端直接输出
IDE 中运行                 IDE Run/Debug 控制台
docker compose up         docker logs seckill-backend (如有容器)
jar 包运行                 java -jar seckill.jar 的输出流
```

---

## 当前缺失的能力

| 能力 | 状态 | 影响 |
|------|------|------|
| 文件输出 | ❌ 未配置 | 日志不持久化，容器重启后丢失 |
| 日志归档 | ❌ 未配置 | 无按天/按大小滚动策略 |
| Trace ID | ❌ 未实现 | 无法串联同一请求的多条日志 |
| JSON 格式 | ❌ 未实现 | 不适合接入 ELK/Loki 等日志平台 |
| 自定义 logback-spring.xml | ❌ 未创建 | 全部依赖 Spring Boot 默认值 |
| 按环境分级别 | ❌ 未配置 | dev/prod 共用同一级别 |

对于练习项目当前状态足够；生产环境建议至少添加文件输出和 Trace ID。
