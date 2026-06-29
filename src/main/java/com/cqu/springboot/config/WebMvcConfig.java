package com.cqu.springboot.config;

import com.cqu.springboot.security.SecondFactorInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置
 * <p>映射本地文件目录到 URL 路径，并注册二级验证拦截器</p>
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final SecondFactorInterceptor secondFactorInterceptor;

    /**
     * 将 /uploads/ebooks/** 映射到本地目录 d:/code/library_sys/uploads/ebooks/
     * <p>用于电子书文件（EPUB/PDF）的静态访问（如前端 PDF.js 直接加载）</p>
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/ebooks/**")
                .addResourceLocations("file:d:/code/library_sys/uploads/ebooks/");
    }

    /**
     * 注册二级验证拦截器
     * <p>仅对管理后台接口生效，方法级通过 @RequireSecondFactor 注解控制是否启用</p>
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(secondFactorInterceptor)
                .addPathPatterns("/api/v1/admin/**");
    }
}
