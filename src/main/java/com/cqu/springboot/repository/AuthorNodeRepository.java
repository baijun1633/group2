package com.cqu.springboot.repository;

import com.cqu.springboot.entity.kg.AuthorNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;

public interface AuthorNodeRepository extends Neo4jRepository<AuthorNode, String> {
}
