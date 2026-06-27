package com.cqu.springboot.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;

/**
 * Neo4j 知识图谱配置类
 */
@Configuration
@EnableNeo4jRepositories(basePackages = "com.cqu.springboot.repository")
public class Neo4jConfig {
}
