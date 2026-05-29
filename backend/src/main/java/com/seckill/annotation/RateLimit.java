package com.seckill.annotation;

import java.lang.annotation.*;

/**
 * 接口限流注解 — 基于 Redis 的滑动窗口计数
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {
    /** 限流时间窗口 (秒) */
    int window() default 5;
    /** 窗口内最大请求次数 */
    int maxRequests() default 10;
    /** 限流提示信息 */
    String message() default "请求过于频繁，请稍后再试";
}
