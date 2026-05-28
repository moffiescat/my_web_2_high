package com.seckill.service;

public interface SeckillService {

    String getSeckillPath(Long userId, Long goodsId);

    Long doSeckill(Long userId, Long goodsId, String path);

    Long getSeckillResult(Long userId, Long goodsId);
}
