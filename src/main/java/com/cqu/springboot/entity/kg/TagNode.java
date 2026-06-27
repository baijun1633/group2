package com.cqu.springboot.entity.kg;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import lombok.Data;

/**
 * Tag 知识图谱节点
 */
@Node("Tag")
@Data
public class TagNode {

    @Id
    private String name;
}
