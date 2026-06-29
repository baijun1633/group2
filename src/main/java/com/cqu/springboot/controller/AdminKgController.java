package com.cqu.springboot.controller;

import com.cqu.springboot.common.ApiResponse;
import com.cqu.springboot.common.BusinessException;
import com.cqu.springboot.common.ErrorCode;
import com.cqu.springboot.common.RequireSecondFactor;
import com.cqu.springboot.dto.KgBuildRequest;
import com.cqu.springboot.dto.KgRelationRequest;
import com.cqu.springboot.service.KnowledgeGraphService;
import com.cqu.springboot.util.CsvUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/admin/kg")
@RequiredArgsConstructor
@Tag(name = "知识图谱管理", description = "管理员图谱构建、关系编辑和实体维护接口")
public class AdminKgController {

    private final KnowledgeGraphService knowledgeGraphService;

    @PostMapping("/build")
    @Operation(summary = "触发图谱构建", description = "从MySQL书籍数据构建知识图谱到Neo4j，支持增量构建和全量重建（forceRebuild）")
    @RequireSecondFactor("图谱构建/重建")
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
    @RequireSecondFactor("编辑图谱关系")
    public ApiResponse<Void> editRelation(@RequestBody KgRelationRequest request) {
        knowledgeGraphService.editRelation(request);
        return ApiResponse.success(null);
    }

    @PostMapping("/relations/batch")
    @Operation(summary = "批量导入图谱关系", description = "上传 CSV 文件批量导入/删除关系。表头：sourceType, sourceId, targetType, targetId, relationType, action(可选，默认 add)")
    @RequireSecondFactor("图谱关系批量导入")
    public ApiResponse<Map<String, Object>> batchImportRelations(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "文件不能为空");
        }
        try {
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);
            List<Map<String, String>> rows = CsvUtil.parse(content);
            Map<String, Object> result = knowledgeGraphService.batchImportRelations(rows);
            return ApiResponse.success("批量导入完成", result);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.CSV_PARSE_ERROR, e.getMessage());
        }
    }

    @PostMapping("/cypher")
    @Operation(summary = "Cypher 调试", description = "管理员执行任意 Cypher 语句（支持读写操作）。请求体：{cypher, params?, limit?}")
    @RequireSecondFactor("Cypher 调试")
    public ApiResponse<List<Map<String, Object>>> executeCypher(@RequestBody Map<String, Object> body) {
        String cypher = body == null ? null : (String) body.get("cypher");
        if (cypher == null || cypher.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "cypher 语句不能为空");
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> params = (Map<String, Object>) body.get("params");
        Integer limit = body.get("limit") instanceof Number
                ? ((Number) body.get("limit")).intValue() : null;
        return ApiResponse.success(knowledgeGraphService.executeCypher(cypher, params, limit));
    }

    // ==================== 图谱实体 CRUD（P3） ====================

    @PostMapping("/entities/{type}")
    @Operation(summary = "新增图谱实体", description = "新增作者/出版社/标签/系列实体（幂等：已存在则返回 existed=true）")
    @RequireSecondFactor("新增图谱实体")
    public ApiResponse<Map<String, Object>> createEntity(
            @PathVariable String type,
            @RequestBody Map<String, String> body) {
        String name = body == null ? null : body.get("name");
        if (name == null || name.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "实体名称(name)不能为空");
        }
        return ApiResponse.success(knowledgeGraphService.createEntity(type, name));
    }

    @GetMapping("/entities/{type}")
    @Operation(summary = "分页查询图谱实体", description = "分页查询指定类型的实体列表（含关联图书数）")
    public ApiResponse<Map<String, Object>> listEntities(
            @PathVariable String type,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(knowledgeGraphService.listEntities(type, page, size));
    }

    @GetMapping("/entities/{type}/{name}")
    @Operation(summary = "查询实体详情", description = "获取单个实体详情（含关联图书列表）")
    public ApiResponse<Map<String, Object>> getEntity(
            @PathVariable String type,
            @PathVariable String name) {
        return ApiResponse.success(knowledgeGraphService.getEntity(type, name));
    }

    @PutMapping("/entities/{type}/{name}")
    @Operation(summary = "重命名实体", description = "重命名图谱实体（保留所有关系），请求体需包含 newName")
    @RequireSecondFactor("重命名图谱实体")
    public ApiResponse<Void> renameEntity(
            @PathVariable String type,
            @PathVariable String name,
            @RequestBody Map<String, String> body) {
        String newName = body == null ? null : body.get("newName");
        if (newName == null || newName.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "新名称(newName)不能为空");
        }
        knowledgeGraphService.renameEntity(type, name, newName);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/entities/{type}/{name}")
    @Operation(summary = "删除实体", description = "删除图谱实体（含关联关系）")
    @RequireSecondFactor("删除图谱实体")
    public ApiResponse<Void> deleteEntity(
            @PathVariable String type,
            @PathVariable String name) {
        knowledgeGraphService.deleteEntity(type, name);
        return ApiResponse.success(null);
    }
}
