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
    PERMISSION_DENIED(1005, "权限不足，无法操作此资源"),

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
    REVIEW_NOT_FOUND(4004, "书评不存在"),
    REVIEW_AUDITED(4005, "该书评已审核，无法重复操作"),

    // 知识图谱相关 (6xxx)
    KG_BUILD_IN_PROGRESS(6001, "图谱构建任务正在进行中"),
    KG_NODE_NOT_FOUND(6002, "图谱节点不存在"),
    KG_QUERY_INVALID(6003, "Cypher查询语句不合法"),
    KG_RELATION_EXISTS(6004, "图谱关系已存在"),

    // 推荐引擎相关 (7xxx)
    RECOMMEND_CONFIG_INVALID(7001, "推荐权重配置无效，权重之和必须为1"),
    RECOMMEND_NO_DATA(7002, "暂无推荐数据"),
    RECOMMEND_BOOK_NOT_FOUND(7003, "推荐源图书不存在"),

    // 管理后台相关 (8xxx)
    USER_NOT_FOUND(8001, "用户不存在"),
    CSV_PARSE_ERROR(8002, "CSV文件解析失败"),
    CSV_FORMAT_ERROR(8003, "CSV文件格式错误"),
    BATCH_IMPORT_PARTIAL_FAIL(8004, "批量导入部分失败"),

    // 电子书相关 (9xxx)
    EBOOK_NOT_UPLOADED(9001, "该书未上传电子书"),
    EBOOK_FILE_NOT_FOUND(9002, "电子书文件不存在"),
    EBOOK_FORMAT_UNSUPPORTED(9003, "电子书格式不支持，仅支持 epub/pdf"),
    EBOOK_PARSE_ERROR(9004, "电子书解析失败"),
    EBOOK_FILE_TOO_LARGE(9005, "电子书文件过大，最大支持 100MB"),

    // 二级验证相关 (10xxx)
    SECOND_FACTOR_REQUIRED(10001, "该操作为高危操作，需提供二级操作密码"),
    SECOND_FACTOR_INVALID(10002, "二级操作密码不正确"),
    SECOND_FACTOR_NOT_SET(10003, "尚未设置二级操作密码，请先设置"),

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
