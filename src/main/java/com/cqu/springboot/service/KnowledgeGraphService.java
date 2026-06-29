package com.cqu.springboot.service;

import com.cqu.springboot.dto.KgBuildRequest;
import com.cqu.springboot.dto.KgQueryRequest;
import com.cqu.springboot.dto.KgRelationRequest;
import com.cqu.springboot.entity.kg.GraphData;
import java.util.List;
import java.util.Map;

/**
 * 知识图谱服务接口
 */
public interface KnowledgeGraphService {

    /**
     * 触发图谱构建（异步执行）
     * @return 包含 taskId 和预估时间
     */
    Map<String, Object> buildGraph(KgBuildRequest request);

    /**
     * Cypher 查询（仅允许只读查询）
     */
    List<Map<String, Object>> query(KgQueryRequest request);

    /**
     * 获取图谱可视化数据
     */
    GraphData getGraph(String entityType, String entityId, Integer depth);

    /**
     * 管理员编辑图谱关系（添加/删除）
     */
    void editRelation(KgRelationRequest request);

    /**
     * 批量导入图谱关系
     * @param rows CSV 行数据，每行包含 sourceType/sourceId/targetType/targetId/relationType/action(可选)
     * @return 导入统计 {total, success, failed, errors:[{row, message}]}
     */
    Map<String, Object> batchImportRelations(List<Map<String, String>> rows);

    /**
     * 管理员 Cypher 调试（不限制关键字，支持读写操作）
     * @param cypher Cypher 语句
     * @param params 绑定参数
     * @param limit  返回行数限制
     * @return 查询结果（写操作返回空列表）
     */
    List<Map<String, Object>> executeCypher(String cypher, Map<String, Object> params, Integer limit);

    /**
     * 新增图谱实体（作者/出版社/标签/系列）
     *
     * @param type 实体类型：author/publisher/tag/series
     * @param name 实体名称
     * @return 创建结果
     */
    Map<String, Object> createEntity(String type, String name);

    /**
     * 删除图谱实体（含关系）
     *
     * @param type 实体类型
     * @param name 实体名称
     */
    void deleteEntity(String type, String name);

    /**
     * 分页查询图谱实体列表（含关联图书数）
     *
     * @param type 实体类型
     * @param page 页码（从1开始）
     * @param size 每页数量
     * @return 实体列表
     */
    Map<String, Object> listEntities(String type, int page, int size);

    /**
     * 获取图谱实体详情（含关联图书列表）
     *
     * @param type 实体类型
     * @param name 实体名称
     * @return 实体详情
     */
    Map<String, Object> getEntity(String type, String name);

    /**
     * 重命名图谱实体（保留关系）
     *
     * @param type    实体类型
     * @param oldName 旧名称
     * @param newName 新名称
     */
    void renameEntity(String type, String oldName, String newName);

    /**
     * 查询构建任务状态
     */
    Map<String, Object> getBuildStatus(String taskId);

    /**
     * 获取图谱统计信息
     */
    Map<String, Object> getGraphStats();
}
