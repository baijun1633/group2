package com.cqu.springboot.security;

import com.cqu.springboot.common.ApiResponse;
import com.cqu.springboot.common.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 配置
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;

    /**
     * 密码编码器
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 安全过滤器链配置
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 禁用CSRF（REST API不需要）
            .csrf(AbstractHttpConfigurer::disable)

            // 无状态Session（使用JWT）
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // 请求授权规则
            .authorizeHttpRequests(auth -> auth
                // 认证相关接口 - 无需登录
                .requestMatchers("/api/v1/auth/**").permitAll()

                // 图书浏览接口 - 无需登录
                .requestMatchers(HttpMethod.GET, "/api/v1/books/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/books").permitAll()

                // 分类查询接口 - 无需登录
                .requestMatchers(HttpMethod.GET, "/api/v1/categories/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/categories").permitAll()

                // 知识图谱可视化接口 - 无需登录
                .requestMatchers(HttpMethod.GET, "/api/v1/kg/graph/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/kg/stats").permitAll()

                // 推荐引擎接口 - 未登录返回热门/新书兜底（所有GET请求公开）
                .requestMatchers(HttpMethod.GET, "/api/v1/recommend/**").permitAll()

                // 管理员接口 - 需要ADMIN角色
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")

                // Swagger/Knife4j 文档 - 无需登录
                .requestMatchers("/doc.html", "/swagger-ui/**", "/v3/api-docs/**", "/swagger-resources/**", "/webjars/**").permitAll()

                // Druid 监控 - 无需登录
                .requestMatchers("/druid/**").permitAll()

                // 静态资源 - 无需登录
                .requestMatchers("/static/**", "/covers/**", "/css/**", "/js/**", "/images/**", "/uploads/**").permitAll()

                // 根路径和错误页面
                .requestMatchers("/", "/error").permitAll()

                // 其他所有请求需要认证
                .anyRequest().authenticated()
            )

            // 未认证处理（401）
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.setCharacterEncoding("UTF-8");
                    ApiResponse<?> apiResponse = ApiResponse.error(ErrorCode.UNAUTHORIZED);
                    response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
                })
                // 权限不足处理（403）
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.setCharacterEncoding("UTF-8");
                    ApiResponse<?> apiResponse = ApiResponse.error(ErrorCode.ACCESS_DENIED);
                    response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
                })
            )

            // 添加JWT过滤器
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
