package com.cqu.springboot.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.time.LocalDateTime;

/**
 * MyBatis-Plus 配置类
 * <p>
 * 配置内容：
 * 1. 分页插件（PaginationInnerInterceptor）
 * 2. 自动填充处理器（MetaObjectHandler）—— 自动填充 createTime、updateTime 等公共字段
 * </p>
 *
 * <p>@Configuration 标注该类为 Spring 配置类，内部 @Bean 方法会被 Spring 容器管理</p>
 */
@Configuration
public class MyBatisPlusConfig {

    /**
     * 主事务管理器（JDBC）
     * <p>
     * 当项目中同时存在 Spring Data Neo4j（reactive 事务管理器）时，
     * 需要指定 @Primary 让 @Transactional 默认使用 JDBC 事务管理器。
     * </p>
     */
    @Bean
    @Primary
    public PlatformTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    /**
     * MyBatis-Plus 插件配置
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 添加分页插件（指定数据库类型为 MySQL）
        // DbType 用于适配不同数据库的分页语法（MySQL、Oracle、PostgreSQL 等各不相同）
        PaginationInnerInterceptor paginationInterceptor = new PaginationInnerInterceptor(DbType.MYSQL);
        // 设置单页最大数据条数限制（防止一次查询过多导致内存溢出），默认无限制
        paginationInterceptor.setMaxLimit(500L);
        // 是否溢出处理：当请求页码超出总页数时，是否自动回到第一页
        paginationInterceptor.setOverflow(false);

        interceptor.addInnerInterceptor(paginationInterceptor);
        return interceptor;
    }

    /**
     * 自动填充处理器
     * <p>
     * 当实体类字段使用 @TableField(fill = FieldFill.INSERT) 或
     * @TableField(fill = FieldFill.INSERT_UPDATE) 注解时，
     * 会在插入/更新时自动调用此处理器填充字段值。
     * </p>
     *
     * <p>常见使用场景：自动填充 createTime（创建时间）、updateTime（更新时间）、
     * createBy（创建人）等审计字段</p>
     *
     * @return MetaObjectHandler 自动填充处理器实例
     */
    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new MetaObjectHandler() {

            /**
             * 插入时自动填充
             * <p>
             * 执行 INSERT 操作时触发，自动为标注了 @TableField(fill = FieldFill.INSERT)
             * 或 FieldFill.INSERT_UPDATE 的字段赋值。
             * </p>
             *
             * @param metaObject 元对象（封装了实体类的字段信息）
             */
            @Override
            public void insertFill(MetaObject metaObject) {
                // strictInsertFill：仅在字段值为 null 时才填充（避免覆盖手动设置的值）
                this.strictInsertFill(metaObject, "createTime", LocalDateTime::now, LocalDateTime.class);
                this.strictInsertFill(metaObject, "updateTime", LocalDateTime::now, LocalDateTime.class);
                // 如需自动填充其他字段（如创建人），可在此处添加：
                // this.strictInsertFill(metaObject, "createBy", () -> getCurrentUser(), String.class);
            }

            /**
             * 更新时自动填充
             * <p>
             * 执行 UPDATE 操作时触发，自动为标注了 @TableField(fill = FieldFill.UPDATE)
             * 或 FieldFill.INSERT_UPDATE 的字段赋值。
             * </p>
             *
             * @param metaObject 元对象（封装了实体类的字段信息）
             */
            @Override
            public void updateFill(MetaObject metaObject) {
                // strictUpdateFill：仅在字段值为 null 时才填充
                this.strictUpdateFill(metaObject, "updateTime", LocalDateTime::now, LocalDateTime.class);
                // 如需自动填充其他字段（如更新人），可在此处添加：
                // this.strictUpdateFill(metaObject, "updateBy", () -> getCurrentUser(), String.class);
            }
        };
    }
}
