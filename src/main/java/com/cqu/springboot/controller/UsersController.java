package com.cqu.springboot.controller;

import com.cqu.springboot.common.ApiResponse;
import com.cqu.springboot.common.BusinessException;
import com.cqu.springboot.common.ErrorCode;
import com.cqu.springboot.entity.Users;
import com.cqu.springboot.mapper.UsersMapper;
import com.cqu.springboot.security.SecurityUtils;
import com.cqu.springboot.service.UserProfileService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户控制器
 * 路径: /api/v1/users
 */
@Tag(name = "用户管理", description = "用户信息、偏好设置和画像管理")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UsersController {

    private final UsersMapper usersMapper;
    private final ObjectMapper objectMapper;
    private final UserProfileService userProfileService;

    /**
     * 获取当前用户信息
     * GET /api/v1/users/me
     */
    @GetMapping("/me")
    public ApiResponse<Map<String, Object>> getCurrentUser() {
        Long userId = SecurityUtils.getCurrentUserId();
        Users user = usersMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USERNAME_NOT_FOUND);
        }

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("userId", user.getUserId());
        userInfo.put("username", user.getUsername());
        userInfo.put("nickname", user.getNickname());
        userInfo.put("avatar", user.getAvatar());
        userInfo.put("role", user.getRole());
        userInfo.put("createdAt", user.getCreateTime());

        // 解析偏好标签
        if (StringUtils.hasText(user.getPreferenceTags())) {
            try {
                List<String> tags = objectMapper.readValue(user.getPreferenceTags(), List.class);
                userInfo.put("preferenceTags", tags);
            } catch (JsonProcessingException e) {
                userInfo.put("preferenceTags", List.of());
            }
        } else {
            userInfo.put("preferenceTags", List.of());
        }

        return ApiResponse.success(userInfo);
    }

    /**
     * 更新当前用户偏好标签
     * PUT /api/v1/users/me/preferences
     */
    @PutMapping("/me/preferences")
    public ApiResponse<String> updatePreferences(@RequestBody Map<String, List<String>> request) {
        Long userId = SecurityUtils.getCurrentUserId();
        Users user = usersMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USERNAME_NOT_FOUND);
        }

        List<String> preferenceTags = request.get("preferenceTags");
        if (preferenceTags != null) {
            try {
                user.setPreferenceTags(objectMapper.writeValueAsString(preferenceTags));
            } catch (JsonProcessingException e) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "偏好标签格式错误");
            }
        }

        user.setUpdateTime(LocalDateTime.now());
        usersMapper.updateById(user);

        return ApiResponse.success("更新成功", null);
    }

    /**
     * 获取用户画像
     * GET /api/v1/users/me/profile
     */
    @Operation(summary = "获取用户画像", description = "获取当前用户的兴趣画像，包括标签权重、偏好作者和分类")
    @GetMapping("/me/profile")
    public ApiResponse<Map<String, Object>> getUserProfile() {
        Long userId = SecurityUtils.getCurrentUserId();
        Map<String, Object> profile = userProfileService.getUserProfile(userId);
        return ApiResponse.success(profile);
    }

    /**
     * 刷新用户画像
     * POST /api/v1/users/me/profile/refresh
     */
    @Operation(summary = "刷新用户画像", description = "根据书架和评分数据重新计算用户画像")
    @PostMapping("/me/profile/refresh")
    public ApiResponse<String> refreshUserProfile() {
        Long userId = SecurityUtils.getCurrentUserId();
        userProfileService.refreshProfile(userId);
        return ApiResponse.success("画像刷新成功", null);
    }
}
