package com.seckill.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.seckill.entity.SeckillGoods;
import com.seckill.mapper.GoodsMapper;
import com.seckill.mapper.SeckillGoodsMapper;
import com.seckill.service.GoodsService;
import com.seckill.utils.RedisKey;
import com.seckill.vo.GoodsVo;
import com.seckill.vo.SeckillGoodsVo;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class GoodsServiceImpl implements GoodsService {

    private final GoodsMapper goodsMapper;
    private final SeckillGoodsMapper seckillGoodsMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    public GoodsServiceImpl(GoodsMapper goodsMapper, SeckillGoodsMapper seckillGoodsMapper,
                            RedisTemplate<String, Object> redisTemplate) {
        this.goodsMapper = goodsMapper;
        this.seckillGoodsMapper = seckillGoodsMapper;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public List<GoodsVo> listGoods() {
        return goodsMapper.listWithSeckill();
    }

    @Override
    public GoodsVo getDetail(Long goodsId) {
        // 先查 Redis
        String key = RedisKey.seckillGoods(goodsId);
        GoodsVo cached = (GoodsVo) redisTemplate.opsForValue().get(key);
        if (cached != null) {
            return cached;
        }
        GoodsVo detail = goodsMapper.getDetailById(goodsId);
        if (detail != null) {
            redisTemplate.opsForValue().set(key, detail, 10, TimeUnit.MINUTES);
        }
        return detail;
    }

    @Override
    public List<SeckillGoodsVo> listSeckillGoods() {
        List<SeckillGoods> list = seckillGoodsMapper.selectList(
                new LambdaQueryWrapper<SeckillGoods>()
                        .ge(SeckillGoods::getEndTime, LocalDateTime.now())
                        .orderByAsc(SeckillGoods::getStartTime)
        );
        List<SeckillGoodsVo> vos = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (SeckillGoods sg : list) {
            SeckillGoodsVo vo = new SeckillGoodsVo();
            vo.setId(sg.getId());
            vo.setGoodsId(sg.getGoodsId());
            vo.setSeckillPrice(sg.getSeckillPrice());
            vo.setStockCount(sg.getStockCount());
            vo.setStartTime(sg.getStartTime());
            vo.setEndTime(sg.getEndTime());
            // 状态: 0-未开始 1-进行中 2-已结束
            if (now.isBefore(sg.getStartTime())) {
                vo.setStatus(0);
            } else if (now.isAfter(sg.getEndTime())) {
                vo.setStatus(2);
            } else {
                vo.setStatus(1);
            }
            // 填充商品基础信息
            GoodsVo goodsVo = goodsMapper.getDetailById(sg.getGoodsId());
            if (goodsVo != null) {
                vo.setGoodsName(goodsVo.getGoodsName());
                vo.setGoodsImg(goodsVo.getGoodsImg());
                vo.setGoodsPrice(goodsVo.getGoodsPrice());
            }
            vos.add(vo);
        }
        return vos;
    }
}
