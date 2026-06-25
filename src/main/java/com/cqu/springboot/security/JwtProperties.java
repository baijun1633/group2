package com.cqu.springboot.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /**
     * 签名密钥
     */
    private String secret;

    /**
     * accessToken 有效期（毫秒）
     */
    private long accessTokenExpiration = 7200000; // 2小时

    /**
     * refreshToken 有效期（毫秒）
     */
    private long refreshTokenExpiration = 604800000; // 7天
}
