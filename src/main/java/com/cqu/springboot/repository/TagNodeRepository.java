package com.cqu.springboot.repository;

import com.cqu.springboot.entity.kg.TagNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;

public interface TagNodeRepository extends Neo4jRepository<TagNode, String> {
}
