package com.cqu.springboot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 文件资源配置类
 * <p>读取 application.yml 中的 file.* 配置项</p>
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "file")
public class FileConfig {

    /**
     * 资源访问前缀（前端访问静态资源的基础URL）
     */
    private String baseUrl = "http://localhost:8080";
}