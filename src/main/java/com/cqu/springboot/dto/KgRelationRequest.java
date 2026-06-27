package com.cqu.springboot.dto;

import lombok.Data;

/**
 * 知识图谱关系编辑请求
 */
@Data
public class KgRelationRequest {

    /**
     * 操作类型: add / delete
     */
    private String action;

    /**
     * 起始节点类型: Book / Author / Publisher / Category / Tag
     */
    private String sourceType;

    /**
     * 起始节点ID（Book用bookId，其他用name）
     */
    private String sourceId;

    /**
     * 目标节点类型
     */
    private String targetType;

    /**
     * 目标节点ID
     */
    private String targetId;

    /**
     * 关系类型: WRITTEN_BY / PUBLISHED_BY / BELONGS_TO / TAGGED_AS
     */
    private String relationType;
}
