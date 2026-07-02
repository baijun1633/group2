package com.cqu.springboot.controller;

import com.cqu.springboot.common.ApiResponse;
import com.cqu.springboot.dto.KgBuildRequest;
import com.cqu.springboot.service.KnowledgeGraphService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/kg")
@RequiredArgsConstructor
@Tag(name = "知识图谱管理", description = "管理员图谱构建参数配置和统计查看")
public class AdminKgController {

    private final KnowledgeGraphService knowledgeGraphService;

    @PostMapping("/build")
    @Operation(summary = "触发图谱构建", description = "管理员触发构建，支持指定目标用户列表或全量构建")
    public ApiResponse<Map<String, Object>> buildGraph(@RequestBody(required = false) KgBuildRequest request) {
        return ApiResponse.success(knowledgeGraphService.buildGraph(request));
    }

    @GetMapping("/build/status/{taskId}")
    @Operation(summary = "查询构建状态", description = "查询图谱构建任务的执行状态和进度")
    public ApiResponse<Map<String, Object>> getBuildStatus(@PathVariable String taskId) {
        return ApiResponse.success(knowledgeGraphService.getBuildStatus(taskId));
    }

    @GetMapping("/stats")
    @Operation(summary = "图谱统计", description = "查看所有已创建的用户知识图谱数量及每个图谱的实体数量")
    public ApiResponse<Map<String, Object>> getAllUserKgStats() {
        return ApiResponse.success(knowledgeGraphService.getAllUserKgStats());
    }
}
