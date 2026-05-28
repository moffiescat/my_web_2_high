package com.seckill.service;

import com.seckill.vo.GoodsVo;
import com.seckill.vo.SeckillGoodsVo;

import java.util.List;

public interface GoodsService {

    List<GoodsVo> listGoods();

    GoodsVo getDetail(Long goodsId);

    List<SeckillGoodsVo> listSeckillGoods();
}
