package com.cqu.springboot.controller;

import com.cqu.springboot.common.ApiResponse;
import com.cqu.springboot.common.PageResponse;
import com.cqu.springboot.entity.Recommendation;
import com.cqu.springboot.security.SecurityUtils;
import com.cqu.springboot.service.RecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 推荐历史控制器
 * 路径: /api/v1/users/me/recommendations
 */
@Tag(name = "推荐历史", description = "查询当前登录用户的推荐历史记录")
@RestController
@RequestMapping("/api/v1/users/me/recommendations")
@RequiredArgsConstructor
public class RecommendationsController {

    private final RecommendationService recommendationService;

    /**
     * 查询推荐历史
     * GET /api/v1/users/me/recommendations?clicked=&collected=&page=&size=
     */
    @Operation(summary = "查询推荐历史", description = "分页查询当前登录用户的推荐历史，可按点击/收藏过滤")
    @GetMapping
    public ApiResponse<PageResponse<Recommendation>> listMyRecommendations(
            @Parameter(description = "是否只看已点击的：true=只看点击过的，false/不传=全部")
            @RequestParam(required = false) Boolean clicked,
            @Parameter(description = "是否只看已收藏的：true=只看收藏过的，false/不传=全部")
            @RequestParam(required = false) Boolean collected,
            @Parameter(description = "页码（从1开始）", example = "1")
            @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量", example = "20")
            @RequestParam(defaultValue = "20") int size) {

        Long userId = SecurityUtils.getCurrentUserId();
        PageResponse<Recommendation> response = recommendationService.getRecommendHistory(userId, clicked, collected, page, size);
        return ApiResponse.success(response);
    }
}
