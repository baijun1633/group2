package com.cqu.springboot.service.impl;

import com.cqu.springboot.entity.Categories;
import com.cqu.springboot.mapper.CategoriesMapper;
import com.cqu.springboot.service.CategoriesService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 图书分类表 服务实现类
 * </p>
 *
 * @author MisterDong
 * @since 2026-06-23
 */
@Service
public class CategoriesServiceImpl extends ServiceImpl<CategoriesMapper, Categories> implements CategoriesService {

}
