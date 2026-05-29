package com.seckill.dto;

import com.seckill.constant.AppConstants;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegisterDto {
    @NotBlank(message = AppConstants.VALID_PHONE_NOT_BLANK)
    private String phone;

    @NotBlank(message = AppConstants.VALID_PASSWORD_NOT_BLANK)
    private String password;

    @NotBlank(message = AppConstants.VALID_NICKNAME_NOT_BLANK)
    private String nickname;
}
