package com.cqu.springboot.entity.kg;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import lombok.Data;

/**
 * Category 知识图谱节点
 */
@Node("Category")
@Data
public class CategoryNode {

    @Id
    private Long categoryId;

    private String name;
}
