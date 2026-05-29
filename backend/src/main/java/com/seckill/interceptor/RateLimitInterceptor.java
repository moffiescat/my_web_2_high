package com.seckill.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seckill.annotation.RateLimit;
import com.seckill.constant.AppConstants;
import com.seckill.utils.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.TimeUnit;

/**
 * 基于 Redis 的接口限流拦截器
 */
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public RateLimitInterceptor(RedisTemplate<String, Object> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod hm)) return true;

        RateLimit rl = hm.getMethodAnnotation(RateLimit.class);
        if (rl == null) return true;

        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) return true;

        String key = "limit:user:" + userId + ":" + request.getRequestURI();
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            redisTemplate.expire(key, rl.window(), TimeUnit.SECONDS);
        }
        if (count != null && count > rl.maxRequests()) {
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(429);
            response.getWriter().write(objectMapper.writeValueAsString(
                    Result.error(429, rl.message())));
            return false;
        }
        return true;
    }
}
