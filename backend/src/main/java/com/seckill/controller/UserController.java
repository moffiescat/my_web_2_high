package com.seckill.controller;

import com.seckill.dto.ChangePasswordDto;
import com.seckill.dto.LoginDto;
import com.seckill.dto.RegisterDto;
import com.seckill.dto.ResetPasswordDto;
import com.seckill.service.UserService;
import com.seckill.utils.Result;
import com.seckill.vo.UserVo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public Result<?> register(@Valid @RequestBody RegisterDto dto) {
        userService.register(dto);
        return Result.ok();
    }

    @PostMapping("/login")
    public Result<String> login(@Valid @RequestBody LoginDto dto) {
        String token = userService.login(dto);
        return Result.ok(token);
    }

    @GetMapping("/info")
    public Result<UserVo> info(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.ok(userService.getInfo(userId));
    }

    @PostMapping("/change-password")
    public Result<?> changePassword(@Valid @RequestBody ChangePasswordDto dto, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        userService.changePassword(userId, dto);
        return Result.ok();
    }

    @PostMapping("/reset-password")
    public Result<?> resetPassword(@Valid @RequestBody ResetPasswordDto dto) {
        userService.resetPassword(dto);
        return Result.ok();
    }
}
