package com.cqu.springboot.dto;

import lombok.Data;

/**
 * 知识图谱 Cypher 查询请求
 */
@Data
public class KgQueryRequest {

    /**
     * Cypher 查询语句
     */
    private String cypher;

    /**
     * 返回结果限制条数（默认100）
     */
    private Integer limit = 100;
}
