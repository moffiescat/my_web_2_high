package com.seckill.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.seckill.constant.AppConstants;
import com.seckill.dto.ChangePasswordDto;
import com.seckill.dto.LoginDto;
import com.seckill.dto.RegisterDto;
import com.seckill.dto.ResetPasswordDto;
import com.seckill.entity.User;
import com.seckill.mapper.UserMapper;
import com.seckill.service.UserService;
import com.seckill.utils.JwtUtil;
import com.seckill.utils.SnowflakeUtil;
import com.seckill.vo.UserVo;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final SnowflakeUtil snowflakeUtil;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public UserServiceImpl(UserMapper userMapper, JwtUtil jwtUtil, SnowflakeUtil snowflakeUtil) {
        this.userMapper = userMapper;
        this.jwtUtil = jwtUtil;
        this.snowflakeUtil = snowflakeUtil;
    }

    @Override
    public void register(RegisterDto dto) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getPhone, dto.getPhone());
        if (userMapper.selectCount(wrapper) > 0) {
            throw new RuntimeException(AppConstants.MSG_PHONE_REGISTERED);
        }
        User user = new User();
        user.setId(snowflakeUtil.nextId());
        user.setPhone(dto.getPhone());
        user.setNickname(dto.getNickname());
        user.setPassword(encoder.encode(dto.getPassword()));
        user.setRegisterTime(LocalDateTime.now());
        userMapper.insert(user);
    }

    @Override
    public String login(LoginDto dto) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getPhone, dto.getPhone());
        User user = userMapper.selectOne(wrapper);
        if (user == null || !encoder.matches(dto.getPassword(), user.getPassword())) {
            throw new RuntimeException(AppConstants.MSG_PHONE_OR_PASSWORD_ERROR);
        }
        user.setLastLoginTime(LocalDateTime.now());
        userMapper.updateById(user);
        return jwtUtil.generateToken(user.getId());
    }

    @Override
    public User getById(Long userId) {
        return userMapper.selectById(userId);
    }

    @Override
    public UserVo getInfo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        UserVo vo = new UserVo();
        vo.setId(user.getId());
        vo.setPhone(user.getPhone());
        vo.setNickname(user.getNickname());
        vo.setRegisterTime(user.getRegisterTime());
        vo.setLastLoginTime(user.getLastLoginTime());
        return vo;
    }

    @Override
    public void changePassword(Long userId, ChangePasswordDto dto) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        if (!encoder.matches(dto.getOldPassword(), user.getPassword())) {
            throw new RuntimeException("旧密码不正确");
        }
        user.setPassword(encoder.encode(dto.getNewPassword()));
        userMapper.updateById(user);
    }

    @Override
    public void resetPassword(ResetPasswordDto dto) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getPhone, dto.getPhone()));
        if (user == null) {
            throw new RuntimeException("该手机号未注册");
        }
        user.setPassword(encoder.encode(dto.getNewPassword()));
        userMapper.updateById(user);
    }
}
