package com.seckill.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.seckill.entity.Goods;
import com.seckill.vo.GoodsVo;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface GoodsMapper extends BaseMapper<Goods> {

    @Select("SELECT g.*, sg.seckill_price, sg.stock_count, sg.start_time, sg.end_time " +
            "FROM t_goods g LEFT JOIN t_seckill_goods sg ON g.id = sg.goods_id " +
            "ORDER BY g.id")
    List<GoodsVo> listWithSeckill();

    @Select("SELECT g.*, sg.seckill_price, sg.stock_count, sg.start_time, sg.end_time " +
            "FROM t_goods g LEFT JOIN t_seckill_goods sg ON g.id = sg.goods_id " +
            "WHERE g.id = #{id}")
    GoodsVo getDetailById(Long id);
}
