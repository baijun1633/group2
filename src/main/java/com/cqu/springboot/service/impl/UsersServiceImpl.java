package com.cqu.springboot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cqu.springboot.common.BusinessException;
import com.cqu.springboot.common.ErrorCode;
import com.cqu.springboot.entity.Users;
import com.cqu.springboot.mapper.UsersMapper;
import com.cqu.springboot.service.UsersService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

/**
 * <p>
 * 用户表 服务实现类
 * </p>
 *
 * @author MisterDong
 * @since 2026-06-23
 */
@Service
@RequiredArgsConstructor
public class UsersServiceImpl extends ServiceImpl<UsersMapper, Users> implements UsersService {

    private final PasswordEncoder passwordEncoder;

    /**
     * 分页查询用户列表
     * 支持按 keyword 模糊匹配 username/nickname，按 role、status 精确匹配
     * 返回结果中 password 字段会被置为 null，避免暴露密码
     */
    @Override
    public Page<Users> listUsers(int page, int size, String keyword, String role, Byte status) {
        // 构造查询条件
        LambdaQueryWrapper<Users> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(Users::getUsername, kw)
                    .or().like(Users::getNickname, kw));
        }
        if (role != null && !role.trim().isEmpty()) {
            wrapper.eq(Users::getRole, role.trim());
        }
        if (status != null) {
            wrapper.eq(Users::getStatus, status);
        }
        // 按创建时间倒序
        wrapper.orderByDesc(Users::getCreateTime);

        Page<Users> pageObj = new Page<>(page, size);
        Page<Users> result = this.page(pageObj, wrapper);

        // 将 password 置为 null，避免暴露
        if (result.getRecords() != null) {
            result.getRecords().forEach(u -> u.setPassword(null));
        }
        return result;
    }

    /**
     * 新增用户
     * 检查 username 唯一性，密码加密后存储，默认 status=1
     */
    @Override
    public void createUser(String username, String password, String nickname, String email, String phone, String role) {
        // 校验用户名是否已存在
        Users existing = baseMapper.selectByUsername(username);
        if (existing != null) {
            throw new BusinessException(ErrorCode.USERNAME_EXISTS);
        }

        LocalDateTime now = LocalDateTime.now();
        Users user = new Users();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setNickname(nickname);
        user.setEmail(email);
        user.setPhone(phone);
        user.setRole(role);
        user.setStatus((byte) 1);
        user.setCreateTime(now);
        user.setUpdateTime(now);

        this.save(user);
    }

    /**
     * 修改用户信息（部分更新）
     * 仅当参数非 null 时更新对应字段，password 非空时重新加密
     */
    @Override
    public void updateUser(Long userId, String nickname, String email, String phone, String role, Byte status, String password) {
        Users user = this.getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        if (nickname != null) {
            user.setNickname(nickname);
        }
        if (email != null) {
            user.setEmail(email);
        }
        if (phone != null) {
            user.setPhone(phone);
        }
        if (role != null) {
            user.setRole(role);
        }
        if (status != null) {
            user.setStatus(status);
        }
        if (password != null && !password.trim().isEmpty()) {
            user.setPassword(passwordEncoder.encode(password));
        }
        user.setUpdateTime(LocalDateTime.now());

        this.updateById(user);
    }
}
