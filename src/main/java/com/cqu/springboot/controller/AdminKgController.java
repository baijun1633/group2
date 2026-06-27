package com.cqu.springboot.controller;

import com.cqu.springboot.common.ApiResponse;
import com.cqu.springboot.dto.KgBuildRequest;
import com.cqu.springboot.dto.KgRelationRequest;
import com.cqu.springboot.service.KnowledgeGraphService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/kg")
@RequiredArgsConstructor
@Tag(name = "知识图谱管理", description = "管理员图谱构建和关系编辑接口")
public class AdminKgController {

    private final KnowledgeGraphService knowledgeGraphService;

    @PostMapping("/build")
    @Operation(summary = "触发图谱构建", description = "从MySQL书籍数据构建知识图谱到Neo4j，支持增量构建和全量重建（forceRebuild）")
    public ApiResponse<Map<String, Object>> buildGraph(@RequestBody(required = false) KgBuildRequest request) {
        if (request == null) {
            request = new KgBuildRequest();
        }
        return ApiResponse.success(knowledgeGraphService.buildGraph(request));
    }

    @GetMapping("/build/status/{taskId}")
    @Operation(summary = "查询构建状态", description = "查询图谱构建任务的执行状态和进度")
    public ApiResponse<Map<String, Object>> getBuildStatus(@PathVariable String taskId) {
        return ApiResponse.success(knowledgeGraphService.getBuildStatus(taskId));
    }

    @PostMapping("/relations")
    @Operation(summary = "编辑图谱关系", description = "管理员手动添加/删除图谱关系")
    public ApiResponse<Void> editRelation(@RequestBody KgRelationRequest request) {
        knowledgeGraphService.editRelation(request);
        return ApiResponse.success(null);
    }
}
