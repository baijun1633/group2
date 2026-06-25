package com.cqu.springboot.mapper;

import com.cqu.springboot.entity.Users;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 用户表 Mapper 接口
 */
public interface UsersMapper extends BaseMapper<Users> {

    /**
     * 根据用户名查询用户
     */
    @Select("SELECT * FROM users WHERE username = #{username}")
    Users selectByUsername(@Param("username") String username);
}
