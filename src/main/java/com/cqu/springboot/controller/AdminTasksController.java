package com.cqu.springboot.controller;

import com.cqu.springboot.common.ApiResponse;
import com.cqu.springboot.entity.KgSyncLog;
import com.cqu.springboot.service.ScheduledTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理员定时任务控制器
 * 路径: /api/v1/admin/tasks
 * 权限: ADMIN（由 SecurityConfig /api/v1/admin/** 兜底规则控制）
 */
@Tag(name = "定时任务管理", description = "手动触发定时任务、查看同步日志")
@RestController
@RequestMapping("/api/v1/admin/tasks")
@RequiredArgsConstructor
public class AdminTasksController {

    private final ScheduledTaskService scheduledTaskService;

    /**
     * 手动触发相似度矩阵计算
     * POST /api/v1/admin/tasks/compute-similarity
     */
    @Operation(summary = "手动触发相似度矩阵计算", description = "立即执行一次全量相似度矩阵计算")
    @PostMapping("/compute-similarity")
    public ApiResponse<Map<String, Object>> computeSimilarity() {
        int count = scheduledTaskService.computeSimilarityMatrix();
        Map<String, Object> result = new HashMap<>();
        result.put("similarityPairs", count);
        result.put("message", "相似度矩阵计算完成");
        return ApiResponse.success(result);
    }

    /**
     * 手动触发知识图谱数据同步
     * POST /api/v1/admin/tasks/sync-kg
     */
    @Operation(summary = "手动触发知识图谱同步", description = "立即执行一次 Neo4j 数据导出")
    @PostMapping("/sync-kg")
    public ApiResponse<Map<String, Object>> syncKg() {
        int count = scheduledTaskService.syncKgData();
        Map<String, Object> result = new HashMap<>();
        result.put("recordsSynced", count);
        result.put("message", "知识图谱数据同步完成");
        return ApiResponse.success(result);
    }

    /**
     * 查看同步日志
     * GET /api/v1/admin/tasks/logs?limit=20
     */
    @Operation(summary = "查看同步日志", description = "按时间倒序查询同步日志记录")
    @GetMapping("/logs")
    public ApiResponse<List<KgSyncLog>> getSyncLogs(
            @Parameter(description = "返回条数", example = "20")
            @RequestParam(defaultValue = "20") int limit) {
        return ApiResponse.success(scheduledTaskService.getSyncLogs(limit));
    }
}
