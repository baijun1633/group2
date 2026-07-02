package com.cqu.springboot.service;

import com.cqu.springboot.dto.KgBuildRequest;
import com.cqu.springboot.dto.KgQueryRequest;
import com.cqu.springboot.entity.kg.GraphData;
import java.util.List;
import java.util.Map;

/**
 * 知识图谱服务接口
 */
public interface KnowledgeGraphService {

    /**
     * 触发当前用户的图谱构建（异步执行）
     * @return 包含 taskId 和预估时间
     */
    Map<String, Object> buildGraphForUser(Long userId, KgBuildRequest request);

    /**
     * 管理员触发图谱构建（支持指定用户列表或全量）
     * @return 包含 taskId 和预估时间
     */
    Map<String, Object> buildGraph(KgBuildRequest request);

    /**
     * Cypher 查询（仅允许只读查询，按用户过滤）
     */
    List<Map<String, Object>> query(Long userId, KgQueryRequest request);

    /**
     * 获取指定用户的图谱可视化数据
     */
    GraphData getGraph(Long userId, String entityType, String entityId, Integer depth);

    /**
     * 获取指定用户的全图概览
     */
    GraphData getGraphOverview(Long userId);

    /**
     * 获取指定用户的图谱统计信息
     */
    Map<String, Object> getGraphStats(Long userId);

    /**
     * 分页查询用户图谱实体列表
     */
    Map<String, Object> listEntities(Long userId, String type, int page, int size);

    /**
     * 获取用户图谱实体详情
     */
    Map<String, Object> getEntity(Long userId, String type, String name);

    /**
     * 移除用户图谱中的实体
     * 不允许移除 Category 类型
     */
    void removeEntity(Long userId, String type, String name);

    /**
     * 查询构建任务状态
     */
    Map<String, Object> getBuildStatus(String taskId);

    /**
     * 获取用户图谱元数据
     */
    Map<String, Object> getMetadata(Long userId);

    /**
     * 将书籍及其关联实体增量添加到用户图谱
     */
    void addBookToUserGraph(Long userId, Long bookId);

    /**
     * 从用户图谱中移除书籍及孤立实体
     */
    void removeBookFromUserGraph(Long userId, Long bookId);

    /**
     * 获取所有用户图谱统计（管理员用）
     */
    Map<String, Object> getAllUserKgStats();
}
