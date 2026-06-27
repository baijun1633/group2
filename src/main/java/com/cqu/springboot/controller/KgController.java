package com.cqu.springboot.controller;

import com.cqu.springboot.common.ApiResponse;
import com.cqu.springboot.dto.KgQueryRequest;
import com.cqu.springboot.entity.kg.GraphData;
import com.cqu.springboot.service.KnowledgeGraphService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/kg")
@RequiredArgsConstructor
@Tag(name = "知识图谱", description = "图谱查询和可视化接口")
public class KgController {

    private final KnowledgeGraphService knowledgeGraphService;

    @PostMapping("/query")
    @Operation(summary = "Cypher查询", description = "执行Cypher查询语句（仅允许只读查询，禁止修改操作）")
    public ApiResponse<List<Map<String, Object>>> query(@RequestBody KgQueryRequest request) {
        return ApiResponse.success(knowledgeGraphService.query(request));
    }

    @GetMapping("/graph/{entityType}/{entityId}")
    @Operation(summary = "图谱可视化", description = "获取指定实体的图谱可视化数据（nodes和edges），兼容D3.js/vis.js")
    public ApiResponse<GraphData> getGraph(
            @PathVariable String entityType,
            @PathVariable String entityId,
            @RequestParam(defaultValue = "2") Integer depth) {
        return ApiResponse.success(knowledgeGraphService.getGraph(entityType, entityId, depth));
    }

    @GetMapping("/stats")
    @Operation(summary = "图谱统计", description = "获取图谱中各类型节点和关系的数量统计")
    public ApiResponse<Map<String, Object>> getStats() {
        return ApiResponse.success(knowledgeGraphService.getGraphStats());
    }
}
