package com.seckill.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.seckill.entity.SeckillGoods;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface SeckillGoodsMapper extends BaseMapper<SeckillGoods> {

    /** 行锁查询，用于并发安全的库存扣减 */
    @Select("SELECT * FROM t_seckill_goods WHERE goods_id = #{goodsId} FOR UPDATE")
    SeckillGoods selectForUpdate(@Param("goodsId") Long goodsId);
}
