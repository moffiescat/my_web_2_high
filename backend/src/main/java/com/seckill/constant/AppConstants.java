package com.seckill.constant;

/**
 * 应用全局常量 — 集中管理所有硬编码字符串和魔法值
 */
public final class AppConstants {

    private AppConstants() {}

    // ==================== 业务提示消息 ====================
    public static final String MSG_SECKILL_GOODS_NOT_FOUND = "秒杀商品不存在";
    public static final String MSG_SECKILL_NOT_STARTED = "秒杀尚未开始";
    public static final String MSG_SECKILL_ENDED = "秒杀已结束";
    public static final String MSG_SECKILL_PATH_INVALID = "秒杀路径无效，请重新获取";
    public static final String MSG_SECKILL_DUPLICATE = "请勿重复抢购";
    public static final String MSG_SECKILL_SOLD_OUT = "商品已售罄";
    public static final String MSG_SYSTEM_BUSY = "系统繁忙，请稍后再试";
    public static final String MSG_PHONE_REGISTERED = "手机号已注册";
    public static final String MSG_PHONE_OR_PASSWORD_ERROR = "手机号或密码错误";
    public static final String MSG_ORDER_NOT_FOUND = "订单不存在";
    public static final String MSG_ORDER_STATUS_DENIED = "订单状态不允许取消";
    public static final String MSG_NOT_LOGIN = "未登录或Token无效";
    public static final String MSG_TOKEN_EXPIRED = "Token已过期，请重新登录";

    // ==================== 校验消息 ====================
    public static final String VALID_PHONE_NOT_BLANK = "手机号不能为空";
    public static final String VALID_PASSWORD_NOT_BLANK = "密码不能为空";
    public static final String VALID_NICKNAME_NOT_BLANK = "昵称不能为空";

    // ==================== 结果码 ====================
    public static final int RESULT_CODE_SUCCESS = 200;
    public static final int RESULT_CODE_BAD_REQUEST = 400;
    public static final int RESULT_CODE_UNAUTHORIZED = 401;
    public static final int RESULT_CODE_ERROR = 500;
    public static final String RESULT_MSG_SUCCESS = "success";

    // ==================== 秒杀返回值 ====================
    /** 排队中 */
    public static final long SECKILL_RESULT_QUEUING = 0L;
    /** 已售罄 */
    public static final long SECKILL_RESULT_SOLD_OUT = -1L;

    // ==================== Redis TTL ====================
    /** 秒杀路径有效期 (秒) */
    public static final long SECKILL_PATH_TTL_SECONDS = 60;
    /** 商品详情缓存有效期 (分钟) */
    public static final long GOODS_CACHE_TTL_MINUTES = 10;

    // ==================== JWT / 认证 ====================
    public static final String AUTH_HEADER = "Authorization";
    public static final String AUTH_TOKEN_PREFIX = "Bearer ";
    public static final int AUTH_TOKEN_PREFIX_LENGTH = 7;

    // ==================== CORS ====================
    public static final String CORS_ALLOWED_ORIGIN = "http://localhost:5173";
    public static final long CORS_MAX_AGE = 3600;

    // ==================== 拦截器路径 ====================
    public static final String INTERCEPTOR_PATH_PATTERN = "/api/**";
    public static final String[] INTERCEPTOR_EXCLUDE_PATHS = {
            "/api/user/register",
            "/api/user/login",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/doc.html"
    };

    // ==================== 布隆过滤器 ====================
    public static final int BLOOM_FILTER_EXPECTED_INSERTIONS = 10000;
    public static final double BLOOM_FILTER_FPP = 0.001;

    // ==================== MQ 消息字段 ====================
    public static final String MQ_MSG_KEY_USER_ID = "userId";
    public static final String MQ_MSG_KEY_GOODS_ID = "goodsId";

    // ==================== Lua 脚本返回值 ====================
    public static final long LUA_RESULT_DUPLICATE = -1L;
    public static final long LUA_RESULT_SOLD_OUT = -2L;

    // ==================== 分页默认值 ====================
    public static final int PAGE_DEFAULT_CURRENT = 1;
    public static final int PAGE_DEFAULT_SIZE = 12;
}
