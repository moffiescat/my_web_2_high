package com.seckill.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.seckill.entity.Goods;
import com.seckill.vo.GoodsVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface GoodsMapper extends BaseMapper<Goods> {

    /** 商品列表（关联秒杀信息） — SQL 见 mapper/GoodsMapper.xml */
    List<GoodsVo> listWithSeckill();

    /** 商品详情 — SQL 见 mapper/GoodsMapper.xml */
    GoodsVo getDetailById(Long id);

    /** 批量查询商品详情 — SQL 见 mapper/GoodsMapper.xml */
    List<GoodsVo> getDetailByIds(@Param("ids") List<Long> ids);
}
