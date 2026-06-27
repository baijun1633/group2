package com.cqu.springboot.repository;

import com.cqu.springboot.entity.kg.PublisherNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;

public interface PublisherNodeRepository extends Neo4jRepository<PublisherNode, String> {
}
