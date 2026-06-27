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
     * 查询构建任务状态
     */
    Map<String, Object> getBuildStatus(String taskId);

    /**
     * 获取图谱统计信息
     */
    Map<String, Object> getGraphStats();
}
