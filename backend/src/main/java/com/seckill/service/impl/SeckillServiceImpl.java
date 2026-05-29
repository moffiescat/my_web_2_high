package com.seckill.service.impl;

import cn.hutool.crypto.digest.MD5;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.google.common.hash.BloomFilter;
import com.seckill.config.RabbitMQConfig;
import com.seckill.constant.AppConstants;
import com.seckill.entity.Goods;
import com.seckill.enums.OrderStatus;
import com.seckill.entity.Order;
import com.seckill.entity.SeckillGoods;
import com.seckill.entity.SeckillOrder;
import com.seckill.mapper.GoodsMapper;
import com.seckill.mapper.OrderMapper;
import com.seckill.mapper.SeckillGoodsMapper;
import com.seckill.mapper.SeckillOrderMapper;
import com.seckill.service.SeckillService;
import com.seckill.utils.RedisKey;
import com.seckill.utils.SnowflakeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class SeckillServiceImpl implements SeckillService {

    private static final Logger log = LoggerFactory.getLogger(SeckillServiceImpl.class);

    private final RedisTemplate<String, Object> redisTemplate;
    private final DefaultRedisScript<Long> stockDeductionScript;
    private final RabbitTemplate rabbitTemplate;
    private final SeckillGoodsMapper seckillGoodsMapper;
    private final OrderMapper orderMapper;
    private final SeckillOrderMapper seckillOrderMapper;
    private final GoodsMapper goodsMapper;
    private final SnowflakeUtil snowflakeUtil;
    private final BloomFilter<Long> bloomFilter;

    public SeckillServiceImpl(RedisTemplate<String, Object> redisTemplate,
                              DefaultRedisScript<Long> stockDeductionScript,
                              RabbitTemplate rabbitTemplate,
                              SeckillGoodsMapper seckillGoodsMapper,
                              OrderMapper orderMapper,
                              SeckillOrderMapper seckillOrderMapper,
                              GoodsMapper goodsMapper,
                              SnowflakeUtil snowflakeUtil,
                              BloomFilter<Long> bloomFilter) {
        this.redisTemplate = redisTemplate;
        this.stockDeductionScript = stockDeductionScript;
        this.rabbitTemplate = rabbitTemplate;
        this.seckillGoodsMapper = seckillGoodsMapper;
        this.orderMapper = orderMapper;
        this.seckillOrderMapper = seckillOrderMapper;
        this.goodsMapper = goodsMapper;
        this.snowflakeUtil = snowflakeUtil;
        this.bloomFilter = bloomFilter;
    }

    @Override
    public String getSeckillPath(Long userId, Long goodsId) {
        // 布隆过滤器预判，拦截不存在的商品ID，防止缓存穿透
        if (!bloomFilter.mightContain(goodsId)) {
            throw new RuntimeException(AppConstants.MSG_SECKILL_GOODS_NOT_FOUND);
        }
        // 校验秒杀商品是否存在且有效
        SeckillGoods sg = seckillGoodsMapper.selectOne(
                new LambdaQueryWrapper<SeckillGoods>()
                        .eq(SeckillGoods::getGoodsId, goodsId)
        );
        if (sg == null) {
            throw new RuntimeException(AppConstants.MSG_SECKILL_GOODS_NOT_FOUND);
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(sg.getStartTime())) {
            throw new RuntimeException(AppConstants.MSG_SECKILL_NOT_STARTED);
        }
        if (now.isAfter(sg.getEndTime())) {
            throw new RuntimeException(AppConstants.MSG_SECKILL_ENDED);
        }

        // 生成随机秒杀路径
        String path = MD5.create().digestHex(UUID.randomUUID().toString());
        redisTemplate.opsForValue().set(RedisKey.seckillPath(userId, goodsId), path, AppConstants.SECKILL_PATH_TTL_SECONDS, TimeUnit.SECONDS);
        return path;
    }

    @Override
    public Long doSeckill(Long userId, Long goodsId, String path) {
        // 1. 校验秒杀路径
        String correctPath = (String) redisTemplate.opsForValue().get(RedisKey.seckillPath(userId, goodsId));
        if (correctPath == null || !correctPath.equals(path)) {
            throw new RuntimeException(AppConstants.MSG_SECKILL_PATH_INVALID);
        }
        // 校验通过后删除路径，防止重复使用
        redisTemplate.delete(RedisKey.seckillPath(userId, goodsId));

        // 2. Redis Lua 原子扣减库存
        String stockKey = RedisKey.seckillStock(goodsId);
        String uidKey = RedisKey.seckillUid(goodsId);
        Long result = redisTemplate.execute(
                stockDeductionScript,
                Arrays.asList(stockKey, uidKey),
                String.valueOf(userId)
        );

        if (result == null) {
            throw new RuntimeException(AppConstants.MSG_SYSTEM_BUSY);
        }
        if (result == AppConstants.LUA_RESULT_DUPLICATE) {
            throw new RuntimeException(AppConstants.MSG_SECKILL_DUPLICATE);
        }
        if (result == AppConstants.LUA_RESULT_SOLD_OUT) {
            throw new RuntimeException(AppConstants.MSG_SECKILL_SOLD_OUT);
        }

        // 3. 发送 MQ 消息异步创建订单
        Map<String, Object> msg = new HashMap<>();
        msg.put(AppConstants.MQ_MSG_KEY_USER_ID, userId);
        msg.put(AppConstants.MQ_MSG_KEY_GOODS_ID, goodsId);
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_SECKILL, RabbitMQConfig.RK_ORDER, msg);

        // 4. 返回 0 表示排队中
        return AppConstants.SECKILL_RESULT_QUEUING;
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
                return AppConstants.SECKILL_RESULT_SOLD_OUT; // 已售罄
            }
        }
        return AppConstants.SECKILL_RESULT_QUEUING; // 还在排队中
    }

    /**
     * MQ 消费者调用: 异步创建订单
     */
    @Transactional
    public void createOrder(Long userId, Long goodsId) {
        // 幂等性校验: 检查是否已为该用户生成过秒杀订单
        Long existsOrder = seckillOrderMapper.selectCount(
                new LambdaQueryWrapper<SeckillOrder>()
                        .eq(SeckillOrder::getUserId, userId)
                        .eq(SeckillOrder::getGoodsId, goodsId)
        );
        if (existsOrder != null && existsOrder > 0) {
            log.warn("重复消息，已忽略: userId={}, goodsId={}", userId, goodsId);
            return;
        }

        // SELECT ... FOR UPDATE 锁住秒杀商品行，防止并发超卖
        SeckillGoods sg = seckillGoodsMapper.selectForUpdate(goodsId);
        if (sg == null || sg.getStockCount() <= 0) {
            return;
        }

        // 扣减 MySQL 库存
        int rows = seckillGoodsMapper.update(null,
                new LambdaUpdateWrapper<SeckillGoods>()
                        .eq(SeckillGoods::getGoodsId, goodsId)
                        .gt(SeckillGoods::getStockCount, 0)
                        .setSql("stock_count = stock_count - 1")
        );
        if (rows <= 0) {
            return; // 库存不足
        }

        // 查询商品名称
        String goodsName = "";
        Goods goods = goodsMapper.selectById(goodsId);
        if (goods != null) {
            goodsName = goods.getGoodsName();
        }

        // 创建订单
        long orderId = snowflakeUtil.nextId();
        Order order = new Order();
        order.setId(orderId);
        order.setUserId(userId);
        order.setGoodsId(goodsId);
        order.setGoodsName(goodsName);
        order.setGoodsPrice(sg.getSeckillPrice());
        order.setStatus(OrderStatus.PENDING.getCode());
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
