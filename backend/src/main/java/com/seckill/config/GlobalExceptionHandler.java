package com.seckill.config;

import com.seckill.constant.AppConstants;
import com.seckill.utils.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @Value("${app.error.detail-enabled:true}")
    private boolean detailEnabled;

    @ExceptionHandler(RuntimeException.class)
    public Result<?> handleRuntime(RuntimeException e) {
        log.error("业务异常: {}", e.getMessage());
        String msg = detailEnabled ? e.getMessage() : AppConstants.MSG_SYSTEM_BUSY;
        return Result.error(AppConstants.RESULT_CODE_BAD_REQUEST, msg);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<?> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return Result.error(AppConstants.RESULT_CODE_BAD_REQUEST, msg);
    }

    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        log.error("系统异常: ", e);
        String msg = detailEnabled ? e.getMessage() : AppConstants.MSG_SYSTEM_BUSY;
        return Result.error(AppConstants.RESULT_CODE_ERROR, msg);
    }
}
