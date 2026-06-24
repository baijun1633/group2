package com.cqu.springboot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * 跨域资源共享（CORS）配置类
 * <p>
 * <b>什么是跨域？</b><br>
 * 浏览器出于安全考虑，默认禁止前端页面（如 http://localhost:5173）向不同源的
 * 后端服务器（如 http://localhost:8080）发送 AJAX 请求，这叫"同源策略"。
 * 当协议、域名、端口三者中任一不同，即视为"跨域"，请求会被浏览器拦截。
 * </p>
 *
 * <p>
 * <b>本配置的作用：</b><br>
 * 通过注册 CorsFilter 过滤器，在 HTTP 响应头中添加 CORS 相关字段（如 Access-Control-Allow-Origin），
 * 告知浏览器"后端允许跨域访问"，从而让前端开发服务器、Postman、移动端等能正常调用后端接口。
 * </p>
 *
 * <p>@Configuration 标注该类为 Spring 配置类</p>
 */
@Configuration
public class CorsConfig {

    /**
     * 注册 CORS 过滤器
     * <p>
     * CorsFilter 是 Spring Web 提供的跨域过滤器，会拦截所有请求，
     * 并在响应头中自动添加 CORS 相关字段。
     * </p>
     *
     * <p>配置项说明：</p>
     * <ul>
     *     <li><b>AllowedOriginPattern：</b> 允许哪些来源访问，"*" 表示允许所有来源</li>
     *     <li><b>AllowCredentials：</b> 是否允许携带 Cookie/认证信息（如 Token）</li>
     *     <li><b>AllowedHeader：</b> 允许请求中携带哪些请求头，"*" 表示全部允许</li>
     *     <li><b>AllowedMethod：</b> 允许哪些 HTTP 方法（GET/POST/PUT/DELETE 等），"*" 表示全部允许</li>
     * </ul>
     *
     * @return CorsFilter 跨域过滤器实例
     */
    @Bean
    public CorsFilter corsFilter() {
        // 1. 创建 CORS 配置对象
        CorsConfiguration config = new CorsConfiguration();

        // 允许的前端地址（"*" 表示允许任意来源，包括 localhost:5173、localhost:3000 等）
        // 生产环境建议指定具体地址，如：config.addAllowedOriginPattern("http://localhost:*")
        config.addAllowedOriginPattern("*");

        // 允许携带 Cookie 和认证信息（如 Authorization 请求头中的 Token）
        // 设为 true 时，AllowedOriginPattern 不能为 "*"，否则浏览器会拒绝
        // 但 addAllowedOriginPattern("*") 配合 setAllowCredentials(true) 是可以的（Spring 5.3+）
        config.setAllowCredentials(true);

        // 允许请求中携带任意请求头（如 Content-Type、Authorization、X-Token 等）
        config.addAllowedHeader("*");

        // 允许使用任意 HTTP 请求方法（GET、POST、PUT、DELETE、OPTIONS 等）
        config.addAllowedMethod("*");

        // 2. 将 CORS 配置注册到 URL 映射中
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // "/**" 表示对所有请求路径都应用上述 CORS 配置
        source.registerCorsConfiguration("/**", config);

        // 3. 返回 CorsFilter（Spring 会在请求处理前自动执行该过滤器）
        return new CorsFilter(source);
    }
}
