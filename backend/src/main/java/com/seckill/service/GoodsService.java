package com.seckill.service;

import com.seckill.utils.PageResult;
import com.seckill.vo.GoodsVo;
import com.seckill.vo.SeckillGoodsVo;

public interface GoodsService {

    PageResult<GoodsVo> listGoods(int page, int size);

    GoodsVo getDetail(Long goodsId);

    PageResult<SeckillGoodsVo> listSeckillGoods(int page, int size);
}
