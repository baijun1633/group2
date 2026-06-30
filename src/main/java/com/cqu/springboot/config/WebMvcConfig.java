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
     * 将 /uploads/** 映射到本地目录 d:/code/library_sys/uploads/
     * <p>/uploads/ebooks/** - 电子书文件（EPUB/PDF）</p>
     * <p>/uploads/covers/** - 图书封面图片</p>
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/ebooks/**")
                .addResourceLocations("file:d:/code/library_sys/uploads/ebooks/");
        registry.addResourceHandler("/covers/**")
                .addResourceLocations("file:d:/code/library_sys/uploads/covers/", "classpath:/static/covers/");
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
