package com.cqu.springboot.controller;

import com.cqu.springboot.common.ApiResponse;
import com.cqu.springboot.dto.KgBuildRequest;
import com.cqu.springboot.dto.KgQueryRequest;
import com.cqu.springboot.entity.kg.GraphData;
import com.cqu.springboot.security.SecurityUtils;
import com.cqu.springboot.service.KnowledgeGraphService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/kg")
@RequiredArgsConstructor
@Tag(name = "知识图谱", description = "图谱查询、可视化和用户管理接口")
public class KgController {

    private final KnowledgeGraphService knowledgeGraphService;

    @PostMapping("/build")
    @Operation(summary = "触发当前用户图谱构建", description = "根据用户行为历史构建知识图谱，支持增量构建和全量重建")
    public ApiResponse<Map<String, Object>> buildGraph(@RequestBody(required = false) KgBuildRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (request == null) {
            request = new KgBuildRequest();
        }
        return ApiResponse.success(knowledgeGraphService.buildGraphForUser(userId, request));
    }

    @GetMapping("/build/status/{taskId}")
    @Operation(summary = "查询构建状态", description = "查询图谱构建任务的执行状态和进度")
    public ApiResponse<Map<String, Object>> getBuildStatus(@PathVariable String taskId) {
        return ApiResponse.success(knowledgeGraphService.getBuildStatus(taskId));
    }

    @PostMapping("/query")
    @Operation(summary = "Cypher查询", description = "执行Cypher查询语句（仅允许只读查询，按当前用户过滤）")
    public ApiResponse<List<Map<String, Object>>> query(@RequestBody KgQueryRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ApiResponse.success(knowledgeGraphService.query(userId, request));
    }

    @GetMapping("/graph/{entityType}/{entityId}")
    @Operation(summary = "图谱可视化", description = "获取指定实体的图谱可视化数据（nodes和edges），兼容D3.js/vis.js")
    public ApiResponse<GraphData> getGraph(
            @PathVariable String entityType,
            @PathVariable String entityId,
            @RequestParam(defaultValue = "2") Integer depth) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ApiResponse.success(knowledgeGraphService.getGraph(userId, entityType, entityId, depth));
    }

    @GetMapping("/overview")
    @Operation(summary = "全图概览", description = "获取当前用户完整知识图谱数据（所有节点和直接关系）")
    public ApiResponse<GraphData> getOverview() {
        Long userId = SecurityUtils.getCurrentUserId();
        return ApiResponse.success(knowledgeGraphService.getGraphOverview(userId));
    }

    @GetMapping("/stats")
    @Operation(summary = "图谱统计", description = "获取当前用户图谱中各类型节点和关系的数量统计")
    public ApiResponse<Map<String, Object>> getStats() {
        Long userId = SecurityUtils.getCurrentUserId();
        return ApiResponse.success(knowledgeGraphService.getGraphStats(userId));
    }

    @GetMapping("/metadata")
    @Operation(summary = "图谱元数据", description = "获取当前用户图谱的元数据（状态、实体数、构建时间等）")
    public ApiResponse<Map<String, Object>> getMetadata() {
        Long userId = SecurityUtils.getCurrentUserId();
        return ApiResponse.success(knowledgeGraphService.getMetadata(userId));
    }

    @GetMapping("/entities/{type}")
    @Operation(summary = "分页查询图谱实体", description = "分页查询当前用户指定类型的实体列表（含关联图书数）")
    public ApiResponse<Map<String, Object>> listEntities(
            @PathVariable String type,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ApiResponse.success(knowledgeGraphService.listEntities(userId, type, page, size));
    }

    @GetMapping("/entities/{type}/{name}")
    @Operation(summary = "查询实体详情", description = "获取当前用户单个实体详情（含关联图书列表）")
    public ApiResponse<Map<String, Object>> getEntity(
            @PathVariable String type,
            @PathVariable String name) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ApiResponse.success(knowledgeGraphService.getEntity(userId, type, name));
    }

    @DeleteMapping("/entities/{type}/{name}")
    @Operation(summary = "移除实体", description = "从当前用户图谱中移除指定实体（不允许移除Category），移除后自动重建")
    public ApiResponse<Void> removeEntity(
            @PathVariable String type,
            @PathVariable String name) {
        Long userId = SecurityUtils.getCurrentUserId();
        knowledgeGraphService.removeEntity(userId, type, name);
        return ApiResponse.success(null);
    }
}
