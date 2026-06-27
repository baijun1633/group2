package com.cqu.springboot.entity.kg;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import lombok.Data;

/**
 * Author 知识图谱节点
 */
@Node("Author")
@Data
public class AuthorNode {

    @Id
    private String name;
}
