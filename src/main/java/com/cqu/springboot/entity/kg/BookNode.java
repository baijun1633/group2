package com.cqu.springboot.entity.kg;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import lombok.Data;

/**
 * Book 知识图谱节点
 */
@Node("Book")
@Data
public class BookNode {

    @Id
    private Long bookId;

    private String title;

    private String isbn;

    private String description;

    private Double avgRating;

    private String coverImage;
}
