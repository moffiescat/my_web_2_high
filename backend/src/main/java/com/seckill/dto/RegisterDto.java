package com.seckill.dto;

import com.seckill.constant.AppConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterDto {
    @NotBlank(message = AppConstants.VALID_PHONE_NOT_BLANK)
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @NotBlank(message = AppConstants.VALID_PASSWORD_NOT_BLANK)
    @Size(min = 6, max = 20, message = "密码长度需在6-20位之间")
    private String password;

    @NotBlank(message = AppConstants.VALID_NICKNAME_NOT_BLANK)
    private String nickname;
}
