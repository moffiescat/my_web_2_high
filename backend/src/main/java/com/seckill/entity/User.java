package com.seckill.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_user")
public class User {
    @TableId
    private Long id;
    private String nickname;
    private String password;
    private String phone;
    private LocalDateTime registerTime;
    private LocalDateTime lastLoginTime;
}
