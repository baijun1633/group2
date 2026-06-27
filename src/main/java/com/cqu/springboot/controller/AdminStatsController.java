package com.cqu.springboot.controller;

import com.cqu.springboot.common.ApiResponse;
import com.cqu.springboot.service.StatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 管理员系统统计控制器
 * 路径: /api/v1/admin/stats
 */
@Tag(name = "系统统计", description = "管理员统计数据查询")
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
}
