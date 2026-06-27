package com.cqu.springboot.repository;

import com.cqu.springboot.entity.kg.CategoryNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;

public interface CategoryNodeRepository extends Neo4jRepository<CategoryNode, Long> {
}
