package com.seckill.utils;

import com.seckill.constant.AppConstants;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {

    private int code;
    private String message;
    private T data;

    public static <T> Result<T> ok(T data) {
        return new Result<>(AppConstants.RESULT_CODE_SUCCESS, AppConstants.RESULT_MSG_SUCCESS, data);
    }

    public static <T> Result<T> ok() {
        return new Result<>(AppConstants.RESULT_CODE_SUCCESS, AppConstants.RESULT_MSG_SUCCESS, null);
    }

    public static <T> Result<T> error(int code, String message) {
        return new Result<>(code, message, null);
    }

    public static <T> Result<T> error(String message) {
        return new Result<>(AppConstants.RESULT_CODE_ERROR, message, null);
    }
}
