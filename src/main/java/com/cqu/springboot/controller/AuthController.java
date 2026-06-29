package com.cqu.springboot.controller;

import com.cqu.springboot.common.ApiResponse;
import com.cqu.springboot.dto.AuthResponse;
import com.cqu.springboot.dto.LoginRequest;
import com.cqu.springboot.dto.RefreshRequest;
import com.cqu.springboot.dto.RegisterRequest;
import com.cqu.springboot.security.SecurityUtils;
import com.cqu.springboot.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 认证控制器
 * 路径: /api/v1/auth
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 用户注册
     * POST /api/v1/auth/register
     */
    @PostMapping("/register")
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ApiResponse.success("注册成功", response);
    }

    /**
     * 用户登录
     * POST /api/v1/auth/login
     */
    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ApiResponse.success("登录成功", response);
    }

    /**
     * 刷新Token
     * POST /api/v1/auth/refresh
     */
    @PostMapping("/refresh")
    public ApiResponse<AuthResponse> refreshToken(@Valid @RequestBody RefreshRequest request) {
        AuthResponse response = authService.refreshToken(request);
        return ApiResponse.success(response);
    }

    /**
     * 设置/更新当前登录用户的二级操作密码
     * POST /api/v1/auth/second-password
     * <p>请求体：{ "currentPassword": "登录密码", "newPassword": "新二级密码" }</p>
     * <p>需先验证登录密码以确认身份，再用 BCrypt 加密新二级密码</p>
     */
    @PostMapping("/second-password")
    public ApiResponse<Void> setSecondPassword(@RequestBody Map<String, String> request) {
        Long userId = SecurityUtils.getCurrentUserId();
        String currentPassword = request.get("currentPassword");
        String newPassword = request.get("newPassword");
        authService.setSecondPassword(userId, currentPassword, newPassword);
        return ApiResponse.success("二级操作密码设置成功", null);
    }
}
