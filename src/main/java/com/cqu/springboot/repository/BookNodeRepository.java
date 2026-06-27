package com.cqu.springboot.repository;

import com.cqu.springboot.entity.kg.BookNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;

public interface BookNodeRepository extends Neo4jRepository<BookNode, Long> {
}
