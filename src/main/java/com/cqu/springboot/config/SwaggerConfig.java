package com.cqu.springboot.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j / Swagger API 文档配置类
 * <p>
 * 基于 OpenAPI 3 规范（Jakarta EE），通过自定义 OpenAPI Bean 配置 API 文档的基本信息。
 * </p>
 *
 * <p>项目启动后可通过以下地址访问接口文档：</p>
 * <ul>
 *     <li><b>Knife4j 增强文档：</b> http://localhost:8080/doc.html （推荐，界面更美观）</li>
 *     <li><b>原生 Swagger UI：</b> http://localhost:8080/swagger-ui.html</li>
 *     <li><b>API 文档 JSON：</b> http://localhost:8080/v3/api-docs</li>
 * </ul>
 *
 * <p>@Configuration 标注该类为 Spring 配置类，@Bean 方法返回的对象会被注册到 Spring 容器中</p>
 */
@Configuration
public class SwaggerConfig {

    /**
     * 自定义 OpenAPI 文档信息
     * <p>
     * OpenAPI 是 Swagger 的新版本规范（Swagger 改名为 OpenAPI 3），
     * Knife4j 4.x 基于 OpenAPI 3 + SpringDoc 实现，适配 Jakarta EE（Spring Boot 3）。
     * </p>
     *
     * <p>该方法配置了文档的标题、版本、描述、联系人、许可证等元信息，
     * 这些信息会显示在 Knife4j/Swagger 文档页面的顶部。</p>
     *
     * @return OpenAPI 配置对象
     */
    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "Bearer Token";
        return new OpenAPI()
                .info(new Info()
                        // 文档标题（显示在页面顶部）
                        .title("Spring Test 项目 API 文档")
                        // 文档版本号（每次接口升级时更新）
                        .version("1.0.0")
                        // 文档描述（简要介绍项目功能和技术栈）
                        .description("基于 Spring Boot 3.5 + MyBatis-Plus 3.5 + Druid + Knife4j 的后端接口文档。"
                                + "包含用户管理、数据 CRUD 等模块的 RESTful API 接口。")
                        // 联系人信息（开发团队/开发者名称和邮箱）
                        .contact(new Contact()
                                .name("MisterDong")
                                .email("admin@cqu.edu.cn"))
                        // 许可证信息（可选，标注项目的开源/商业许可协议）
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                // 添加全局安全要求
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                // 配置JWT Bearer认证组件
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .in(SecurityScheme.In.HEADER)));
    }
}
