package com.cqu.springboot.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 高危操作二级验证注解
 * <p>
 * 概要设计文档 line 308：图谱数据删除、推荐算法参数修改、用户权限变更、
 * 批量数据导入等高危操作，需输入二级操作密码完成二次校验。
 * </p>
 * <p>
 * 标注于 Controller 方法上，{@link com.cqu.springboot.security.SecondFactorInterceptor}
 * 会拦截并要求请求头携带 {@code X-Second-Password}，与当前登录用户的 second_password 字段进行 BCrypt 比对。
 * </p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireSecondFactor {

    /**
     * 操作描述（用于日志记录与错误提示）
     */
    String value() default "高危操作";
}
