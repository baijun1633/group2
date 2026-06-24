package com.cqu.springboot.service.impl;

import com.cqu.springboot.entity.Users;
import com.cqu.springboot.mapper.UsersMapper;
import com.cqu.springboot.service.UsersService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 用户表 服务实现类
 * </p>
 *
 * @author MisterDong
 * @since 2026-06-23
 */
@Service
public class UsersServiceImpl extends ServiceImpl<UsersMapper, Users> implements UsersService {

}
