package com.seckill.config;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import com.seckill.constant.AppConstants;
import com.seckill.entity.SeckillGoods;
import com.seckill.mapper.SeckillGoodsMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class BloomFilterConfig {

    private final SeckillGoodsMapper seckillGoodsMapper;

    public BloomFilterConfig(SeckillGoodsMapper seckillGoodsMapper) {
        this.seckillGoodsMapper = seckillGoodsMapper;
    }

    /**
     * 商品ID布隆过滤器，用于防止缓存穿透
     * 预期插入量 10000，误判率 0.001
     */
    @Bean
    public BloomFilter<Long> goodsBloomFilter() {
        BloomFilter<Long> filter = BloomFilter.create(
                Funnels.longFunnel(),
                AppConstants.BLOOM_FILTER_EXPECTED_INSERTIONS,
                AppConstants.BLOOM_FILTER_FPP
        );

        List<SeckillGoods> list = seckillGoodsMapper.selectList(null);
        for (SeckillGoods sg : list) {
            if (sg.getGoodsId() != null) {
                filter.put(sg.getGoodsId());
            }
        }
        return filter;
    }
}
