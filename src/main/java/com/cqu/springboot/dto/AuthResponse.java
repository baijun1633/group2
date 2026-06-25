package com.cqu.springboot.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 认证响应（登录/注册/刷新Token）
 */
@Data
@Builder
public class AuthResponse {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 访问Token（有效期2小时）
     */
    private String accessToken;

    /**
     * 刷新Token（有效期7天）
     */
    private String refreshToken;
}
