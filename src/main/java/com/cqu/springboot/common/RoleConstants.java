package com.cqu.springboot.common;

/**
 * 角色常量
 * <p>
 * 系统支持 5 类角色（对应概要设计文档 line 118）：
 * <ul>
 *     <li>{@link #USER}              普通读者</li>
 *     <li>{@link #BOOK_ADMIN}        图书管理员（图书/电子书/购书链接/知识图谱）</li>
 *     <li>{@link #OPS_ADMIN}         运营管理员（统计/推荐配置/用户管理）</li>
 *     <li>{@link #COMMUNITY_ADMIN}   社区管理员（书评审核/优质标记/违规清理）</li>
 *     <li>{@link #ADMIN}             超级管理员（拥有所有管理模块权限）</li>
 * </ul>
 * 角色值存于 users.role 字段，JWT claim "role" 直接存储该字符串。
 * Spring Security 权限名为 "ROLE_" + role（如 ROLE_BOOK_ADMIN）。
 */
public final class RoleConstants {

    private RoleConstants() {}

    /** 普通读者 */
    public static final String USER = "USER";

    /** 图书管理员：图书 / 电子书 / 购书链接 / 知识图谱（仅构建参数配置和统计查看） */
    public static final String BOOK_ADMIN = "BOOK_ADMIN";

    /** 运营管理员：系统统计 / 推荐配置 / 用户管理 */
    public static final String OPS_ADMIN = "OPS_ADMIN";

    /** 社区管理员：书评审核 / 优质标记 / 违规清理 */
    public static final String COMMUNITY_ADMIN = "COMMUNITY_ADMIN";

    /** 超级管理员：拥有所有管理模块权限 */
    public static final String ADMIN = "ADMIN";
}
