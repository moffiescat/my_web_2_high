package com.seckill.utils;

public class RedisKey {

    public static final String SECKILL_STOCK = "seckill:stock:%d";
    public static final String SECKILL_UID = "seckill:uid:%d";
    public static final String SECKILL_GOODS = "seckill:goods:%d";
    public static final String SECKILL_PATH = "seckill:path:%d:%d";
    public static final String LIMIT_USER_API = "limit:user:%d:%s";
    public static final String LOCK_ORDER = "lock:order:%d:%d";

    public static String seckillStock(Long goodsId) {
        return String.format(SECKILL_STOCK, goodsId);
    }

    public static String seckillUid(Long goodsId) {
        return String.format(SECKILL_UID, goodsId);
    }

    public static String seckillGoods(Long goodsId) {
        return String.format(SECKILL_GOODS, goodsId);
    }

    public static String seckillPath(Long userId, Long goodsId) {
        return String.format(SECKILL_PATH, userId, goodsId);
    }

    public static String limitUserApi(Long userId, String api) {
        return String.format(LIMIT_USER_API, userId, api);
    }

    public static String lockOrder(Long userId, Long goodsId) {
        return String.format(LOCK_ORDER, userId, goodsId);
    }
}
