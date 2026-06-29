package com.cqu.springboot.generator;

import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.engine.FreemarkerTemplateEngine;

/**
 * MyBatis-Plus 代码生成器
 * <p>
 * 运行 main() 方法即可自动生成 Entity、Mapper、Service、Controller 等代码文件。
 * 使用方式：在 IDE 中右键运行 main 方法
 * </p>
 *
 * <p>适配版本：MyBatis-Plus 3.5.6 + Spring Boot 3.x（Jakarta）</p>
 */
public class MyBatisPlusGenerator {

    // ============== 数据库连接配置（与 application.yaml 保持一致）==============
    /** 数据库连接URL */
    private static final String DB_URL = "jdbc:mysql://localhost:3306/library_db?serverTimezone=Asia/Shanghai&useUnicode=true&characterEncoding=utf-8&useSSL=false&allowPublicKeyRetrieval=true";
    /** 数据库用户名 */
    private static final String DB_USERNAME = "manager";
    /** 数据库密码 */
    private static final String DB_PASSWORD = System.getenv().getOrDefault("GEN_DB_PASSWORD", "CHANGE_ME");

    // ============== 生成器配置 ==============
    /** 生成代码的作者（会显示在类注释中） */
    private static final String AUTHOR = "MisterDong";
    /** 父包名（生成的代码会放在此包下的 entity/mapper/service/controller 子包中） */
    private static final String PARENT_PACKAGE = "com.cqu.springboot";
    /** 表前缀（生成实体类时会去掉此前缀，例如 sys_dept → Dept） */
    private static final String[] TABLE_PREFIX = {"sys_"};

    /**
     * 代码生成入口
     * <p>
     * 该方法为 main 入口，可以在 IDE 中直接右键运行。
     * 运行后会在项目中自动生成以下文件：
     * <ul>
     *     <li>Entity 实体类（使用 Lombok 注解）</li>
     *     <li>Mapper 接口（继承 BaseMapper）</li>
     *     <li>Mapper XML 文件（输出到 resources/mapper/ 目录）</li>
     *     <li>Service 接口</li>
     *     <li>Service 实现类</li>
     *     <li>Controller 控制器（使用 @RestController 注解）</li>
     * </ul>
     * </p>
     */
    public static void main(String[] args) {
        // 获取项目根路径（即 user.dir，IDE 中运行时为项目所在目录）
        String projectPath = System.getProperty("user.dir");

        FastAutoGenerator.create(DB_URL, DB_USERNAME, DB_PASSWORD)
                // ==================== 全局配置 ====================
                .globalConfig(builder -> builder
                        .author(AUTHOR)                           // 设置作者（类注释中显示）
                        .outputDir(projectPath + "/src/main/java") // 设置输出目录为项目 src/main/java
                        .commentDate("yyyy-MM-dd")                // 注释中的日期格式
                        .disableOpenDir()                         // 生成完毕后不自动打开输出目录
                )
                // ==================== 包配置 ====================
                .packageConfig(builder -> builder
                        .parent(PARENT_PACKAGE)                    // 父包名（所有生成文件的顶层包）
                        .entity("entity")                          // 实体类包名：com.cqu.spring_test.entity
                        .mapper("mapper")                          // Mapper接口包名：com.cqu.spring_test.mapper
                        .service("service")                        // Service接口包名：com.cqu.spring_test.service
                        .serviceImpl("service.impl")               // Service实现包名：com.cqu.spring_test.service.impl
                        .controller("controller")                  // Controller包名：com.cqu.spring_test.controller
                        .xml("mapper")                             // Mapper XML 文件输出到 resources/mapper/ 目录
                        .pathInfo(
                                // 自定义 Mapper XML 文件的存放路径（放在 resources/mapper/ 下）
                                java.util.Collections.singletonMap(
                                        com.baomidou.mybatisplus.generator.config.OutputFile.xml,
                                        projectPath + "/src/main/resources/mapper"
                                )
                        )
                )
                // ==================== 策略配置 ====================
                .strategyConfig(builder -> builder
                        .addTablePrefix(TABLE_PREFIX)              // 设置表前缀（生成实体时去掉前缀，如 sys_dept → Dept）

                        // ----- Entity 实体策略 -----
                        .entityBuilder()
                        .enableLombok()                            // 启用 Lombok（自动生成 getter/setter/toString 等）
                        .enableTableFieldAnnotation()              // 在字段上添加 @TableField 注解（映射数据库字段名）
                        .naming(com.baomidou.mybatisplus.generator.config.rules.NamingStrategy.underline_to_camel) // 表名 → 类名：下划线转驼峰
                        .columnNaming(com.baomidou.mybatisplus.generator.config.rules.NamingStrategy.underline_to_camel) // 字段名 → 属性名：下划线转驼峰
                        .build()

                        // ----- Controller 策略 -----
                        .controllerBuilder()
                        .enableRestStyle()                         // 使用 @RestController 注解（而非 @Controller）
                        .enableHyphenStyle()                       // URL 路径使用连字符风格（如 /user-info）
                        .build()

                        // ----- Mapper 策略 -----
                        .mapperBuilder()
                        .enableBaseColumnList()                    // 在 Mapper.xml 中生成公用的 SQL 字段列表
                        .enableBaseResultMap()                     // 在 Mapper.xml 中生成公用的 ResultMap 映射
                        .build()

                        // ----- Service 策略 -----
                        .serviceBuilder()
                        .formatServiceFileName("%sService")        // Service 接口文件名格式（如 UserService.java）
                        .formatServiceImplFileName("%sServiceImpl") // Service 实现类文件名格式（如 UserServiceImpl.java）
                        .build()
                )
                // ==================== 模板引擎 ====================
                // 使用 Freemarker 模板引擎生成代码（也可选择 Velocity）
                .templateEngine(new FreemarkerTemplateEngine())
                // ==================== 执行生成 ====================
                .execute();
    }
}
