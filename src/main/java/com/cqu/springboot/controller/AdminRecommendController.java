package com.cqu.springboot.controller;

import com.cqu.springboot.common.ApiResponse;
import com.cqu.springboot.service.RecommendService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 管理员推荐配置接口
 */
@RestController
@RequestMapping("/api/v1/admin/recommend")
@RequiredArgsConstructor
@Tag(name = "管理员-推荐配置", description = "推荐算法权重配置管理")
public class AdminRecommendController {

    private final RecommendService recommendService;

    @Operation(summary = "获取推荐权重配置")
    @GetMapping("/config")
    public ApiResponse<Map<String, Object>> getConfig() {
        return ApiResponse.success(recommendService.getConfig());
    }

    @Operation(summary = "更新推荐权重配置", description = "权重之和必须为1.0，key: kg_weight/itemcf_weight/hot_weight/new_weight")
    @PutMapping("/config")
    public ApiResponse<Void> updateConfig(@RequestBody Map<String, BigDecimal> weights) {
        recommendService.updateConfig(weights);
        return ApiResponse.success();
    }
}
