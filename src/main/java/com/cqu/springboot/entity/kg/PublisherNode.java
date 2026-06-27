package com.cqu.springboot.entity.kg;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import lombok.Data;

/**
 * Publisher 知识图谱节点
 */
@Node("Publisher")
@Data
public class PublisherNode {

    @Id
    private String name;
}
