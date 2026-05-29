package com.seckill.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seckill.constant.AppConstants;
import com.seckill.interceptor.JwtInterceptor;
import com.seckill.interceptor.RateLimitInterceptor;
import com.seckill.utils.JwtUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class InterceptorConfig implements WebMvcConfigurer {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    public InterceptorConfig(JwtUtil jwtUtil, ObjectMapper objectMapper,
                             RedisTemplate<String, Object> redisTemplate) {
        this.jwtUtil = jwtUtil;
        this.objectMapper = objectMapper;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // JWT 拦截器
        registry.addInterceptor(new JwtInterceptor(jwtUtil, objectMapper))
                .addPathPatterns(AppConstants.INTERCEPTOR_PATH_PATTERN)
                .excludePathPatterns(AppConstants.INTERCEPTOR_EXCLUDE_PATHS);
        // 限流拦截器
        registry.addInterceptor(new RateLimitInterceptor(redisTemplate, objectMapper))
                .addPathPatterns("/api/seckill/**");
    }
}
