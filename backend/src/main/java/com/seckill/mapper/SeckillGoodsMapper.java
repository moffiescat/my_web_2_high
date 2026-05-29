package com.seckill.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.seckill.entity.SeckillGoods;
import org.apache.ibatis.annotations.Param;

public interface SeckillGoodsMapper extends BaseMapper<SeckillGoods> {

    /** 行锁查询 — SQL 见 mapper/SeckillGoodsMapper.xml */
    SeckillGoods selectForUpdate(@Param("goodsId") Long goodsId);
}
