package com.cqu.springboot.controller;

import com.cqu.springboot.common.ApiResponse;
import com.cqu.springboot.dto.AuthResponse;
import com.cqu.springboot.dto.LoginRequest;
import com.cqu.springboot.dto.RefreshRequest;
import com.cqu.springboot.dto.RegisterRequest;
import com.cqu.springboot.security.SecurityUtils;
import com.cqu.springboot.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 认证控制器
 * 路径: /api/v1/auth
 */
@Tag(name = "认证管理", description = "用户注册、登录、Token刷新、二级密码")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 用户注册
     * POST /api/v1/auth/register
     */
    @Operation(summary = "用户注册", description = "用户注册，校验用户名唯一性，密码加密存储，返回用户信息和Token")
    @PostMapping("/register")
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ApiResponse.success("注册成功", response);
    }

    /**
     * 用户登录
     * POST /api/v1/auth/login
     */
    @Operation(summary = "用户登录", description = "用户登录，验证用户名密码，返回accessToken和refreshToken")
    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ApiResponse.success("登录成功", response);
    }

    /**
     * 刷新Token
     * POST /api/v1/auth/refresh
     */
    @Operation(summary = "刷新Token", description = "刷新Token，验证refreshToken有效性，返回新Token对")
    @PostMapping("/refresh")
    public ApiResponse<AuthResponse> refreshToken(@Valid @RequestBody RefreshRequest request) {
        AuthResponse response = authService.refreshToken(request);
        return ApiResponse.success(response);
    }

    /**
     * 退出登录
     * POST /api/v1/auth/logout
     */
    @Operation(summary = "退出登录", description = "退出登录，前端删除Token即可")
    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        return ApiResponse.success("退出成功", null);
    }

    /**
     * 设置/更新当前登录用户的二级操作密码
     * POST /api/v1/auth/second-password
     * <p>请求体：{ "currentPassword": "登录密码", "newPassword": "新二级密码" }</p>
     * <p>需先验证登录密码以确认身份，再用 BCrypt 加密新二级密码存入 users.second_password</p>
     */
    @Operation(summary = "设置二级操作密码", description = "设置/更新当前登录用户的二级操作密码，需先验证登录密码以确认身份")
    @PostMapping("/second-password")
    public ApiResponse<Void> setSecondPassword(@RequestBody Map<String, String> request) {
        Long userId = SecurityUtils.getCurrentUserId();
        String currentPassword = request.get("currentPassword");
        String newPassword = request.get("newPassword");
        authService.setSecondPassword(userId, currentPassword, newPassword);
        return ApiResponse.success("二级操作密码设置成功", null);
    }

    /**
     * 修改当前登录用户的登录密码
     * POST /api/v1/auth/change-password
     * <p>请求体：{ "oldPassword": "旧密码", "newPassword": "新密码" }</p>
     * <p>需先验证旧密码以确认身份，再用 BCrypt 加密新密码存入 users.password</p>
     */
    @Operation(summary = "修改登录密码", description = "修改当前登录用户的登录密码，需先验证旧密码以确认身份")
    @PostMapping("/change-password")
    public ApiResponse<Void> changePassword(@RequestBody Map<String, String> request) {
        Long userId = SecurityUtils.getCurrentUserId();
        String oldPassword = request.get("oldPassword");
        String newPassword = request.get("newPassword");
        authService.changePassword(userId, oldPassword, newPassword);
        return ApiResponse.success("密码修改成功", null);
    }
}
