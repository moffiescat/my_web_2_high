package com.seckill.service.impl;

import cn.hutool.crypto.digest.MD5;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.seckill.config.RabbitMQConfig;
import com.seckill.entity.Order;
import com.seckill.entity.SeckillGoods;
import com.seckill.entity.SeckillOrder;
import com.seckill.mapper.OrderMapper;
import com.seckill.mapper.SeckillGoodsMapper;
import com.seckill.mapper.SeckillOrderMapper;
import com.seckill.service.SeckillService;
import com.seckill.utils.RedisKey;
import com.seckill.utils.SnowflakeUtil;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class SeckillServiceImpl implements SeckillService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final DefaultRedisScript<Long> stockDeductionScript;
    private final RabbitTemplate rabbitTemplate;
    private final SeckillGoodsMapper seckillGoodsMapper;
    private final OrderMapper orderMapper;
    private final SeckillOrderMapper seckillOrderMapper;
    private final SnowflakeUtil snowflakeUtil;

    public SeckillServiceImpl(RedisTemplate<String, Object> redisTemplate,
                              DefaultRedisScript<Long> stockDeductionScript,
                              RabbitTemplate rabbitTemplate,
                              SeckillGoodsMapper seckillGoodsMapper,
                              OrderMapper orderMapper,
                              SeckillOrderMapper seckillOrderMapper,
                              SnowflakeUtil snowflakeUtil) {
        this.redisTemplate = redisTemplate;
        this.stockDeductionScript = stockDeductionScript;
        this.rabbitTemplate = rabbitTemplate;
        this.seckillGoodsMapper = seckillGoodsMapper;
        this.orderMapper = orderMapper;
        this.seckillOrderMapper = seckillOrderMapper;
        this.snowflakeUtil = snowflakeUtil;
    }

    @Override
    public String getSeckillPath(Long userId, Long goodsId) {
        // 校验秒杀商品是否存在且有效
        SeckillGoods sg = seckillGoodsMapper.selectOne(
                new LambdaQueryWrapper<SeckillGoods>()
                        .eq(SeckillGoods::getGoodsId, goodsId)
        );
        if (sg == null) {
            throw new RuntimeException("秒杀商品不存在");
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(sg.getStartTime())) {
            throw new RuntimeException("秒杀尚未开始");
        }
        if (now.isAfter(sg.getEndTime())) {
            throw new RuntimeException("秒杀已结束");
        }

        // 生成随机秒杀路径
        String path = MD5.create().digestHex(UUID.randomUUID().toString());
        redisTemplate.opsForValue().set(RedisKey.seckillPath(userId, goodsId), path, 60, TimeUnit.SECONDS);
        return path;
    }

    @Override
    public Long doSeckill(Long userId, Long goodsId, String path) {
        // 1. 校验秒杀路径
        String correctPath = (String) redisTemplate.opsForValue().get(RedisKey.seckillPath(userId, goodsId));
        if (correctPath == null || !correctPath.equals(path)) {
            throw new RuntimeException("秒杀路径无效，请重新获取");
        }
        // 校验通过后删除路径，防止重复使用
        redisTemplate.delete(RedisKey.seckillPath(userId, goodsId));

        // 2. Redis Lua 原子扣减库存
        String stockKey = RedisKey.seckillStock(goodsId);
        String uidKey = RedisKey.seckillUid(goodsId);
        Long result = redisTemplate.execute(
                stockDeductionScript,
                Collections.singletonList(stockKey),
                stockKey, uidKey, String.valueOf(userId)
        );

        if (result == null) {
            throw new RuntimeException("系统繁忙，请稍后再试");
        }
        if (result == -1) {
            throw new RuntimeException("请勿重复抢购");
        }
        if (result == -2) {
            throw new RuntimeException("商品已售罄");
        }

        // 3. 发送 MQ 消息异步创建订单
        Map<String, Object> msg = new HashMap<>();
        msg.put("userId", userId);
        msg.put("goodsId", goodsId);
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_SECKILL, RabbitMQConfig.RK_ORDER, msg);

        // 4. 返回 0 表示排队中
        return 0L;
    }

    @Override
    public Long getSeckillResult(Long userId, Long goodsId) {
        // 查秒杀订单表，看是否已生成订单
        SeckillOrder so = seckillOrderMapper.selectOne(
                new LambdaQueryWrapper<SeckillOrder>()
                        .eq(SeckillOrder::getUserId, userId)
                        .eq(SeckillOrder::getGoodsId, goodsId)
        );
        if (so != null) {
            return so.getOrderId(); // 秒杀成功，返回订单ID
        }
        // 检查库存，判断是否已售罄
        String stockKey = RedisKey.seckillStock(goodsId);
        Object stockObj = redisTemplate.opsForValue().get(stockKey);
        if (stockObj != null && Integer.parseInt(stockObj.toString()) <= 0) {
            Boolean isMember = redisTemplate.opsForSet().isMember(RedisKey.seckillUid(goodsId), userId.toString());
            if (isMember == null || !isMember) {
                return -1L; // 已售罄
            }
        }
        return 0L; // 还在排队中
    }

    /**
     * MQ 消费者调用: 异步创建订单
     */
    @Transactional
    public void createOrder(Long userId, Long goodsId) {
        SeckillGoods sg = seckillGoodsMapper.selectOne(
                new LambdaQueryWrapper<SeckillGoods>().eq(SeckillGoods::getGoodsId, goodsId)
        );
        if (sg == null) {
            return;
        }

        // 再次校验 MySQL 库存
        if (sg.getStockCount() <= 0) {
            return;
        }

        // 扣减 MySQL 库存
        int rows = seckillGoodsMapper.update(null,
                new LambdaQueryWrapper<SeckillGoods>()
                        .eq(SeckillGoods::getGoodsId, goodsId)
                        .gt(SeckillGoods::getStockCount, 0)
                        .setSql("stock_count = stock_count - 1")
        );
        if (rows <= 0) {
            return; // 库存不足
        }

        // 创建订单
        long orderId = snowflakeUtil.nextId();
        Order order = new Order();
        order.setId(orderId);
        order.setUserId(userId);
        order.setGoodsId(goodsId);
        order.setGoodsPrice(sg.getSeckillPrice());
        order.setStatus(0);
        order.setCreateTime(LocalDateTime.now());
        orderMapper.insert(order);

        // 创建秒杀订单
        SeckillOrder so = new SeckillOrder();
        so.setId(snowflakeUtil.nextId());
        so.setUserId(userId);
        so.setOrderId(orderId);
        so.setGoodsId(goodsId);
        seckillOrderMapper.insert(so);
    }
}
