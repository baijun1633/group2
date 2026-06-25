package com.cqu.springboot.service.impl;

import com.cqu.springboot.common.BusinessException;
import com.cqu.springboot.common.ErrorCode;
import com.cqu.springboot.dto.AuthResponse;
import com.cqu.springboot.dto.LoginRequest;
import com.cqu.springboot.dto.RefreshRequest;
import com.cqu.springboot.dto.RegisterRequest;
import com.cqu.springboot.entity.Users;
import com.cqu.springboot.mapper.UsersMapper;
import com.cqu.springboot.security.JwtTokenProvider;
import com.cqu.springboot.service.AuthService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 认证服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UsersMapper usersMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // 1. 校验密码确认
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "两次输入的密码不一致");
        }

        // 2. 检查用户名是否已存在
        Users existingUser = usersMapper.selectByUsername(request.getUsername());
        if (existingUser != null) {
            throw new BusinessException(ErrorCode.USERNAME_EXISTS);
        }

        // 3. 创建用户
        Users user = new Users();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(StringUtils.hasText(request.getNickname()) ? request.getNickname() : request.getUsername());
        user.setRole("USER"); // 默认普通用户
        user.setStatus((byte) 1); // 启用状态
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());

        // 处理偏好标签
        if (request.getPreferenceTags() != null && !request.getPreferenceTags().isEmpty()) {
            try {
                user.setPreferenceTags(objectMapper.writeValueAsString(request.getPreferenceTags()));
            } catch (JsonProcessingException e) {
                log.warn("序列化偏好标签失败", e);
            }
        }

        usersMapper.insert(user);

        // 4. 生成Token
        String accessToken = jwtTokenProvider.generateAccessToken(user.getUserId(), user.getUsername(), user.getRole());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getUserId());

        log.info("用户注册成功: username={}", user.getUsername());

        return AuthResponse.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        // 1. 查询用户
        Users user = usersMapper.selectByUsername(request.getUsername());
        if (user == null) {
            throw new BusinessException(ErrorCode.PASSWORD_ERROR);
        }

        // 2. 检查用户状态
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException(ErrorCode.USER_DISABLED);
        }

        // 3. 验证密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_ERROR);
        }

        // 4. 生成Token
        String accessToken = jwtTokenProvider.generateAccessToken(user.getUserId(), user.getUsername(), user.getRole());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getUserId());

        log.info("用户登录成功: username={}", user.getUsername());

        return AuthResponse.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    @Override
    public AuthResponse refreshToken(RefreshRequest request) {
        String refreshToken = request.getRefreshToken();

        // 1. 验证 refreshToken
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID, "refreshToken无效或已过期");
        }

        // 2. 检查Token类型
        String tokenType = jwtTokenProvider.getTokenType(refreshToken);
        if (!"refresh".equals(tokenType)) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID, "Token类型错误");
        }

        // 3. 获取用户信息
        Long userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
        Users user = usersMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USERNAME_NOT_FOUND);
        }

        // 4. 检查用户状态
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException(ErrorCode.USER_DISABLED);
        }

        // 5. 生成新Token对
        String newAccessToken = jwtTokenProvider.generateAccessToken(user.getUserId(), user.getUsername(), user.getRole());
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(user.getUserId());

        log.info("Token刷新成功: userId={}", userId);

        return AuthResponse.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .build();
    }
}
