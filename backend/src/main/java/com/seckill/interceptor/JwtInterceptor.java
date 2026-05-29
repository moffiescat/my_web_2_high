package com.seckill.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seckill.constant.AppConstants;
import com.seckill.utils.JwtUtil;
import com.seckill.utils.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.HashMap;
import java.util.Map;

public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    public JwtInterceptor(JwtUtil jwtUtil, ObjectMapper objectMapper) {
        this.jwtUtil = jwtUtil;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token = request.getHeader(AppConstants.AUTH_HEADER);
        if (token == null || !token.startsWith(AppConstants.AUTH_TOKEN_PREFIX)) {
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(AppConstants.RESULT_CODE_UNAUTHORIZED);
            response.getWriter().write(objectMapper.writeValueAsString(Result.error(AppConstants.RESULT_CODE_UNAUTHORIZED, AppConstants.MSG_NOT_LOGIN)));
            return false;
        }
        token = token.substring(AppConstants.AUTH_TOKEN_PREFIX_LENGTH);
        if (!jwtUtil.validateToken(token)) {
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(AppConstants.RESULT_CODE_UNAUTHORIZED);
            response.getWriter().write(objectMapper.writeValueAsString(Result.error(AppConstants.RESULT_CODE_UNAUTHORIZED, AppConstants.MSG_TOKEN_EXPIRED)));
            return false;
        }
        // 将 userId 存到 request attribute
        Long userId = jwtUtil.getUserIdFromToken(token);
        request.setAttribute("userId", userId);
        return true;
    }
}
