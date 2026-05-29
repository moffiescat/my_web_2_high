package com.seckill.config;

import com.seckill.entity.SeckillGoods;
import com.seckill.mapper.SeckillGoodsMapper;
import com.seckill.utils.RedisKey;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 启动时将 MySQL 秒杀库存预热到 Redis，避免 Lua 脚本读到空 key
 */
@Component
public class StockInitializer {

    private static final Logger log = LoggerFactory.getLogger(StockInitializer.class);

    private final SeckillGoodsMapper seckillGoodsMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    public StockInitializer(SeckillGoodsMapper seckillGoodsMapper, RedisTemplate<String, Object> redisTemplate) {
        this.seckillGoodsMapper = seckillGoodsMapper;
        this.redisTemplate = redisTemplate;
    }

    @PostConstruct
    public void initStock() {
        List<SeckillGoods> list = seckillGoodsMapper.selectList(null);
        for (SeckillGoods sg : list) {
            String stockKey = RedisKey.seckillStock(sg.getGoodsId());
            // 仅在 Redis 中没有该 key 时才写入，避免覆盖运行时的库存数据
            Boolean exists = redisTemplate.hasKey(stockKey);
            if (Boolean.FALSE.equals(exists)) {
                redisTemplate.opsForValue().set(stockKey, sg.getStockCount());
                log.info("Redis 库存预热: goodsId={}, stock={}", sg.getGoodsId(), sg.getStockCount());
            }
        }
        log.info("Redis 库存预热完成，共 {} 个秒杀商品", list.size());
    }
}
