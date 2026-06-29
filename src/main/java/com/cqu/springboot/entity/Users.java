package com.cqu.springboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * 用户表
 */
@Getter
@Setter
@TableName("users")
public class Users implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    @TableId(value = "user_id", type = IdType.AUTO)
    private Long userId;

    /**
     * 用户名
     */
    @TableField("username")
    private String username;

    /**
     * 密码(BCrypt加密)
     */
    @TableField("password")
    private String password;

    /**
     * 二级操作密码(BCrypt加密)，用于高危操作二次验证（图谱删除、推荐参数修改、用户权限变更、批量导入）
     */
    @TableField("second_password")
    private String secondPassword;

    /**
     * 昵称
     */
    @TableField("nickname")
    private String nickname;

    /**
     * 邮箱
     */
    @TableField("email")
    private String email;

    /**
     * 手机号
     */
    @TableField("phone")
    private String phone;

    /**
     * 头像URL
     */
    @TableField("avatar")
    private String avatar;

    /**
     * 角色: USER-普通读者, BOOK_ADMIN-图书管理员, OPS_ADMIN-运营管理员, COMMUNITY_ADMIN-社区管理员, ADMIN-超级管理员
     * @see com.cqu.springboot.common.RoleConstants
     */
    @TableField("role")
    private String role;

    /**
     * 偏好标签(JSON格式，如 ["科幻","悬疑"])
     */
    @TableField("preference_tags")
    private String preferenceTags;

    /**
     * 状态: 0-禁用, 1-启用
     */
    @TableField("status")
    private Byte status;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField("update_time")
    private LocalDateTime updateTime;
}
