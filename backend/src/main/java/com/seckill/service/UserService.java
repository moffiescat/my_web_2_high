package com.seckill.service;

import com.seckill.dto.LoginDto;
import com.seckill.dto.RegisterDto;
import com.seckill.entity.User;
import com.seckill.vo.UserVo;

public interface UserService {

    void register(RegisterDto dto);

    String login(LoginDto dto);

    User getById(Long userId);

    UserVo getInfo(Long userId);
}
