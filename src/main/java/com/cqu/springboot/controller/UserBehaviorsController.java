package com.cqu.springboot.controller;

import com.cqu.springboot.common.ApiResponse;
import com.cqu.springboot.common.PageResponse;
import com.cqu.springboot.entity.UserBehavior;
import com.cqu.springboot.security.SecurityUtils;
import com.cqu.springboot.service.UserBehaviorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 用户行为历史控制器
 * 路径: /api/v1/users/me/behaviors
 */
@Tag(name = "用户行为历史", description = "查询当前登录用户的行为历史记录")
@RestController
@RequestMapping("/api/v1/users/me/behaviors")
@RequiredArgsConstructor
public class UserBehaviorsController {

    private final UserBehaviorService userBehaviorService;

    /**
     * 查询当前登录用户行为历史
     * GET /api/v1/users/me/behaviors?page=&size=&type=
     */
    @Operation(summary = "查询行为历史", description = "分页查询当前登录用户的行为历史，可按行为类型过滤")
    @GetMapping
    public ApiResponse<PageResponse<UserBehavior>> listMyBehaviors(
            @Parameter(description = "行为类型：view/search/rate/collect/review，为空查全部", example = "view")
            @RequestParam(required = false) String type,
            @Parameter(description = "页码（从1开始）", example = "1")
            @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量", example = "20")
            @RequestParam(defaultValue = "20") int size) {

        Long userId = SecurityUtils.getCurrentUserId();
        PageResponse<UserBehavior> response = userBehaviorService.listUserBehaviors(userId, type, page, size);
        return ApiResponse.success(response);
    }
}
