package com.cqu.springboot.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置
 * <p>映射本地文件目录到 URL 路径，使上传的电子书文件可通过 HTTP 访问</p>
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /**
     * 将 /uploads/ebooks/** 映射到本地目录 d:/code/library_sys/uploads/ebooks/
     * <p>用于电子书文件（EPUB/PDF）的静态访问（如前端 PDF.js 直接加载）</p>
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/ebooks/**")
                .addResourceLocations("file:d:/code/library_sys/uploads/ebooks/");
    }
}
