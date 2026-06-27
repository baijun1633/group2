package com.cqu.springboot.service;

import com.cqu.springboot.entity.Users;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 用户表 服务类
 * </p>
 *
 * @author MisterDong
 * @since 2026-06-23
 */
public interface UsersService extends IService<Users> {

    /**
     * 分页查询用户列表
     * 支持按用户名/昵称模糊搜索、角色精确匹配、状态精确匹配
     * 返回结果中 password 字段会被置为 null
     *
     * @param page    页码（从1开始）
     * @param size    每页数量
     * @param keyword 关键词（匹配 username 或 nickname）
     * @param role    角色（ADMIN/USER）
     * @param status  状态（0-禁用, 1-启用）
     * @return 分页结果
     */
    Page<Users> listUsers(int page, int size, String keyword, String role, Byte status);

    /**
     * 新增用户
     *
     * @param username 用户名
     * @param password 明文密码（方法内部会加密）
     * @param nickname 昵称
     * @param email    邮箱
     * @param phone    手机号
     * @param role     角色（ADMIN/USER）
     */
    void createUser(String username, String password, String nickname, String email, String phone, String role);

    /**
     * 修改用户信息（部分更新）
     * 仅当参数非null时才更新对应字段
     *
     * @param userId   用户ID
     * @param nickname 昵称
     * @param email    邮箱
     * @param phone    手机号
     * @param role     角色
     * @param status   状态
     * @param password 明文密码（非空时重新加密）
     */
    void updateUser(Long userId, String nickname, String email, String phone, String role, Byte status, String password);
}
