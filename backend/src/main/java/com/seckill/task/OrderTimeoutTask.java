package com.seckill.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.seckill.entity.Order;
import com.seckill.entity.SeckillGoods;
import com.seckill.enums.OrderStatus;
import com.seckill.mapper.OrderMapper;
import com.seckill.mapper.SeckillGoodsMapper;
import com.seckill.utils.RedisKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单超时自动取消 — 每分钟扫描一次，取消 15 分钟未支付的订单
 */
@Component
public class OrderTimeoutTask {

    private static final Logger log = LoggerFactory.getLogger(OrderTimeoutTask.class);
    private static final int TIMEOUT_MINUTES = 15;

    private final OrderMapper orderMapper;
    private final SeckillGoodsMapper seckillGoodsMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    public OrderTimeoutTask(OrderMapper orderMapper, SeckillGoodsMapper seckillGoodsMapper,
                            RedisTemplate<String, Object> redisTemplate) {
        this.orderMapper = orderMapper;
        this.seckillGoodsMapper = seckillGoodsMapper;
        this.redisTemplate = redisTemplate;
    }

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void cancelTimeoutOrders() {
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(TIMEOUT_MINUTES);
        List<Order> timeoutOrders = orderMapper.selectList(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getStatus, OrderStatus.PENDING.getCode())
                        .lt(Order::getCreateTime, deadline)
        );
        if (timeoutOrders.isEmpty()) return;

        for (Order order : timeoutOrders) {
            order.setStatus(OrderStatus.CANCELLED.getCode());
            orderMapper.updateById(order);

            // 恢复 MySQL 库存
            seckillGoodsMapper.update(null,
                    new LambdaUpdateWrapper<SeckillGoods>()
                            .eq(SeckillGoods::getGoodsId, order.getGoodsId())
                            .setSql("stock_count = stock_count + 1")
            );

            // 恢复 Redis 库存
            String stockKey = RedisKey.seckillStock(order.getGoodsId());
            redisTemplate.opsForValue().increment(stockKey, 1);
        }
        log.info("超时订单处理完成，共取消 {} 单", timeoutOrders.size());
    }
}
