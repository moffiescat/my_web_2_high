package com.seckill.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserVo {
    private Long id;
    private String phone;
    private String nickname;
    private LocalDateTime registerTime;
    private LocalDateTime lastLoginTime;
}
