package com.cqu.springboot.controller;

import com.cqu.springboot.common.ApiResponse;
import com.cqu.springboot.service.StatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 管理员系统统计控制器
 * 路径: /api/v1/admin/stats
 */
@Tag(name = "系统统计", description = "管理员统计数据查询、图表化数据接口")
@RestController
@RequestMapping("/api/v1/admin/stats")
@RequiredArgsConstructor
public class AdminStatsController {

    private final StatsService statsService;

    /**
     * 系统统计数据
     * GET /api/v1/admin/stats
     */
    @Operation(summary = "系统统计数据", description = "总用户/图书/书评/评分/分类数、待审核书评、近7天活跃用户")
    @GetMapping
    public ApiResponse<Map<String, Object>> getStats() {
        return ApiResponse.success(statsService.getSystemStats());
    }

    /**
     * 用户增长趋势（近30天每日新增用户）
     * GET /api/v1/admin/stats/user-growth
     */
    @Operation(summary = "用户增长趋势", description = "近30天每日新增用户数，适合折线图展示")
    @GetMapping("/user-growth")
    public ApiResponse<List<Map<String, Object>>> getUserGrowthTrend() {
        return ApiResponse.success(statsService.getUserGrowthTrend());
    }

    /**
     * 图书评分分布
     * GET /api/v1/admin/stats/rating-distribution
     */
    @Operation(summary = "图书评分分布", description = "各评分段(0-1,1-2,2-3,3-4,4-5)的图书数量，适合柱状图展示")
    @GetMapping("/rating-distribution")
    public ApiResponse<List<Map<String, Object>>> getRatingDistribution() {
        return ApiResponse.success(statsService.getRatingDistribution());
    }

    /**
     * 热门图书 Top N
     * GET /api/v1/admin/stats/top-books?limit=10
     */
    @Operation(summary = "热门图书Top N", description = "按浏览量排序的热门图书列表，适合横向柱状图展示")
    @GetMapping("/top-books")
    public ApiResponse<List<Map<String, Object>>> getTopBooks(
            @Parameter(description = "返回数量", example = "10") @RequestParam(defaultValue = "10") int limit) {
        return ApiResponse.success(statsService.getTopBooks(limit));
    }

    /**
     * 行为类型分布
     * GET /api/v1/admin/stats/behavior-distribution
     */
    @Operation(summary = "行为类型分布", description = "各行为类型(view/collect/rate/review/read/search)的数量，适合饼图展示")
    @GetMapping("/behavior-distribution")
    public ApiResponse<List<Map<String, Object>>> getBehaviorDistribution() {
        return ApiResponse.success(statsService.getBehaviorDistribution());
    }
}
