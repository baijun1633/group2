package com.cqu.springboot.controller;

import com.cqu.springboot.common.ApiResponse;
import com.cqu.springboot.common.PageResponse;
import com.cqu.springboot.dto.ReadingProgressRequest;
import com.cqu.springboot.entity.ReadingProgress;
import com.cqu.springboot.security.SecurityUtils;
import com.cqu.springboot.service.ReadingProgressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 阅读进度控制器
 * 路径: /api/v1/reading
 */
@Tag(name = "阅读管理", description = "阅读进度同步、阅读历史、阅读数据统计")
@RestController
@RequestMapping("/api/v1/reading")
@RequiredArgsConstructor
public class ReadingController {

    private final ReadingProgressService readingProgressService;

    /**
     * 同步阅读进度（含本次阅读时长累加、章节更新）
     * PUT /api/v1/reading/progress
     */
    @Operation(summary = "同步阅读进度", description = "上报阅读进度与本次时长，后端 upsert reading_progress 并累加 readDuration，同时记录 read 行为用于周/月统计")
    @PutMapping("/progress")
    public ApiResponse<Map<String, Object>> syncProgress(@RequestBody ReadingProgressRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        Long id = readingProgressService.recordReading(userId, request);
        return ApiResponse.success("同步成功", Map.of("progressId", id));
    }

    /**
     * 获取阅读进度
     * GET /api/v1/reading/progress/{bookId}
     */
    @Operation(summary = "获取阅读进度", description = "获取指定图书的当前阅读进度")
    @GetMapping("/progress/{bookId}")
    public ApiResponse<ReadingProgress> getProgress(
            @Parameter(description = "图书ID", example = "1") @PathVariable Long bookId) {
        Long userId = SecurityUtils.getCurrentUserId();
        ReadingProgress progress = readingProgressService.getReadingProgress(userId, bookId);
        return ApiResponse.success(progress);
    }

    /**
     * 获取阅读历史
     * GET /api/v1/reading/history
     */
    @Operation(summary = "获取阅读历史", description = "分页获取阅读历史，支持状态过滤：reading(在读)/finished(已读)/不传=全部")
    @GetMapping("/history")
    public ApiResponse<PageResponse<ReadingProgress>> getReadingHistory(
            @Parameter(description = "状态：reading=在读, finished=已读, 不传=全部") @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = SecurityUtils.getCurrentUserId();
        PageResponse<ReadingProgress> response = readingProgressService.getReadingHistory(userId, status, page, size);
        return ApiResponse.success(response);
    }

    /**
     * 阅读数据统计
     * GET /api/v1/reading/stats
     */
    @Operation(summary = "阅读统计", description = "返回周/月阅读时长（秒）、已读完数量、在读数量")
    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> getReadingStats() {
        Long userId = SecurityUtils.getCurrentUserId();
        Map<String, Object> stats = readingProgressService.getReadingStats(userId);
        return ApiResponse.success(stats);
    }
}
