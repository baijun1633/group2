package com.cqu.springboot.common;

import lombok.Getter;

/**
 * 错误码枚举
 *
 * 错误码规则：
 * - 0: 成功
 * - 1xxx: 认证/授权相关错误
 * - 2xxx: 用户相关错误
 * - 3xxx: 图书相关错误
 * - 4xxx: 业务逻辑错误
 * - 5xxx: 系统错误
 */
@Getter
public enum ErrorCode {

    // 成功
    SUCCESS(0, "success"),

    // 认证相关 (1xxx)
    UNAUTHORIZED(1001, "未认证，请先登录"),
    TOKEN_EXPIRED(1002, "Token已过期，请重新登录"),
    TOKEN_INVALID(1003, "Token无效"),
    ACCESS_DENIED(1004, "权限不足"),

    // 用户相关 (2xxx)
    USERNAME_EXISTS(2001, "用户名已存在"),
    USERNAME_NOT_FOUND(2002, "用户不存在"),
    PASSWORD_ERROR(2003, "用户名或密码错误"),
    USER_DISABLED(2004, "用户已被禁用"),

    // 图书相关 (3xxx)
    BOOK_NOT_FOUND(3001, "图书不存在"),
    BOOK_ALREADY_EXISTS(3002, "图书已存在"),

    // 业务逻辑 (4xxx)
    PARAM_ERROR(4001, "参数错误"),
    DATA_NOT_FOUND(4002, "数据不存在"),
    OPERATION_FAILED(4003, "操作失败"),

    // 系统错误 (5xxx)
    SYSTEM_ERROR(5001, "系统内部错误"),
    SERVICE_UNAVAILABLE(5002, "服务暂不可用");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
