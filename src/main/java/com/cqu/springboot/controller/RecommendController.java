package com.cqu.springboot.controller;

import com.cqu.springboot.common.ApiResponse;
import com.cqu.springboot.dto.RecommendItem;
import com.cqu.springboot.security.SecurityUtils;
import com.cqu.springboot.service.KgRecommendService;
import com.cqu.springboot.service.RecommendService;
import com.cqu.springboot.service.RecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 推荐引擎接口
 */
@RestController
@RequestMapping("/api/v1/recommend")
@RequiredArgsConstructor
@Tag(name = "推荐引擎", description = "个性化图书推荐、相似读物、延伸阅读")
public class RecommendController {

    private final RecommendService recommendService;
    private final KgRecommendService kgRecommendService;
    private final RecommendationService recommendationService;

    @Operation(summary = "首页推荐", description = "登录用户返回个性化推荐（KG+ItemCF+热门+新书加权），未登录返回热门/新书兜底")
    @GetMapping("/home")
    public ApiResponse<List<RecommendItem>> getHomeRecommend(
            @RequestParam(defaultValue = "10") int limit) {
        Long userId = SecurityUtils.getCurrentUserIdOrNull();
        List<RecommendItem> items = recommendService.getHomeRecommend(userId, limit);
        // 登录用户落库推荐记录（未登录不落库）
        recommendationService.generateRecommendation(userId, items, "home");
        return ApiResponse.success(items);
    }

    @Operation(summary = "热门图书", description = "按评分人数和评分排序，无需登录")
    @GetMapping("/hot")
    public ApiResponse<List<RecommendItem>> getHotBooks(
            @RequestParam(defaultValue = "90") int days,
            @RequestParam(defaultValue = "10") int limit) {
        List<RecommendItem> items = recommendService.getHotBooks(days, limit);
        return ApiResponse.success(items);
    }

    @Operation(summary = "新书推荐", description = "按出版日期排序，无需登录")
    @GetMapping("/new")
    public ApiResponse<List<RecommendItem>> getNewBooks(
            @RequestParam(defaultValue = "6") int months,
            @RequestParam(defaultValue = "10") int limit) {
        List<RecommendItem> items = recommendService.getNewBooks(months, limit);
        return ApiResponse.success(items);
    }

    @Operation(summary = "相似读物", description = "基于知识图谱1-2跳推理（同作者/同分类/同标签），无需登录")
    @GetMapping("/{bookId}/similar")
    public ApiResponse<List<RecommendItem>> getSimilarBooks(
            @PathVariable Long bookId,
            @RequestParam(defaultValue = "10") int limit) {
        List<RecommendItem> items = kgRecommendService.getSimilarBooks(bookId, limit);
        // 登录用户落库推荐记录（未登录不落库）
        Long userId = SecurityUtils.getCurrentUserIdOrNull();
        recommendationService.generateRecommendation(userId, items, "similar");
        return ApiResponse.success(items);
    }

    @Operation(summary = "延伸阅读", description = "基于知识图谱3跳以上路径推理，发现间接关联的高质量图书，无需登录")
    @GetMapping("/{bookId}/extended")
    public ApiResponse<List<RecommendItem>> getExtendedBooks(
            @PathVariable Long bookId,
            @RequestParam(defaultValue = "10") int limit) {
        List<RecommendItem> items = kgRecommendService.getExtendedBooks(bookId, limit);
        // 登录用户落库推荐记录（未登录不落库）
        Long userId = SecurityUtils.getCurrentUserIdOrNull();
        recommendationService.generateRecommendation(userId, items, "extended");
        return ApiResponse.success(items);
    }

    @Operation(summary = "推荐点击回调", description = "用户点击推荐项时调用，标记 is_clicked=1。recommendId 由推荐列表返回")
    @PostMapping("/{recommendId}/click")
    public ApiResponse<Boolean> recordClick(
            @Parameter(description = "推荐记录ID", example = "1") @PathVariable Long recommendId) {
        Long userId = SecurityUtils.getCurrentUserId();
        boolean ok = recommendationService.recordClick(recommendId, userId);
        return ApiResponse.success(ok ? "点击已记录" : "记录不存在或无权操作", ok);
    }

    @Operation(summary = "推荐解释详情", description = "对当前登录用户，解释为什么推荐某本书（含推理路径、推荐来源、综合理由）")
    @GetMapping("/{bookId}/explain")
    public ApiResponse<RecommendItem> explainRecommendation(@PathVariable Long bookId) {
        Long userId = SecurityUtils.getCurrentUserIdOrNull();
        RecommendItem item = recommendService.explainRecommendation(userId, bookId);
        return ApiResponse.success(item);
    }
}
