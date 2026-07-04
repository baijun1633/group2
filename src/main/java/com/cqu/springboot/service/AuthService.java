package com.cqu.springboot.service;

import com.cqu.springboot.dto.AuthResponse;
import com.cqu.springboot.dto.LoginRequest;
import com.cqu.springboot.dto.RefreshRequest;
import com.cqu.springboot.dto.RegisterRequest;

/**
 * 认证服务接口
 */
public interface AuthService {

    /**
     * 用户注册
     */
    AuthResponse register(RegisterRequest request);

    /**
     * 用户登录
     */
    AuthResponse login(LoginRequest request);

    /**
     * 刷新Token
     */
    AuthResponse refreshToken(RefreshRequest request);

    /**
     * 设置/更新当前登录用户的二级操作密码
     * <p>需先验证登录密码以确认身份，再用 BCrypt 加密新二级密码存入 users.second_password</p>
     *
     * @param userId          当前登录用户ID
     * @param currentPassword 登录密码（用于身份验证）
     * @param newPassword     新的二级操作密码
     */
    void setSecondPassword(Long userId, String currentPassword, String newPassword);

    /**
     * 修改当前登录用户的登录密码
     * <p>需先验证旧密码，然后更新为新密码（BCrypt加密）</p>
     *
     * @param userId      当前登录用户ID
     * @param oldPassword 旧密码（用于验证身份）
     * @param newPassword 新密码
     */
    void changePassword(Long userId, String oldPassword, String newPassword);
}
