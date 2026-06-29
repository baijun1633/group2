package com.cqu.springboot.controller;

import com.cqu.springboot.common.ApiResponse;
import com.cqu.springboot.dto.RecommendItem;
import com.cqu.springboot.service.RecommendService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 轮播图/首页聚合接口
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "首页聚合", description = "轮播图、首页推荐汇总")
public class BannersController {

    private final RecommendService recommendService;

    /**
     * 轮播图数据 - 返回热门图书作为轮播内容
     * 未登录也能访问，返回热门+新书
     */
    @Operation(summary = "轮播图数据", description = "返回热门图书作为轮播内容，无需登录")
    @GetMapping("/banners")
    public ApiResponse<List<RecommendItem>> getBanners(
            @RequestParam(defaultValue = "6") int limit) {
        // 用热门图书作为轮播内容
        List<RecommendItem> items = recommendService.getHotBooks(90, limit);
        return ApiResponse.success(items);
    }
}
