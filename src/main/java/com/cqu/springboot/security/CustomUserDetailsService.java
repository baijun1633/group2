package com.cqu.springboot.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cqu.springboot.entity.Users;
import com.cqu.springboot.mapper.UsersMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * 自定义 UserDetailsService
 * 用于 Spring Security 的用户认证
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UsersMapper usersMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 查询用户
        Users user = usersMapper.selectOne(
                new LambdaQueryWrapper<Users>().eq(Users::getUsername, username)
        );

        if (user == null) {
            throw new UsernameNotFoundException("用户不存在: " + username);
        }

        // 检查用户状态
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new UsernameNotFoundException("用户已被禁用: " + username);
        }

        // 获取角色（默认为 USER）
        String role = "USER";
        if (user.getRole() != null) {
            role = user.getRole();
        }

        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + role);

        return new User(
                user.getUsername(),
                user.getPassword(),
                Collections.singletonList(authority)
        );
    }

    /**
     * 根据用户名查询用户
     */
    public Users getUserByUsername(String username) {
        return usersMapper.selectOne(
                new LambdaQueryWrapper<Users>().eq(Users::getUsername, username)
        );
    }
}
