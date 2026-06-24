package com.cqu.springboot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cqu.springboot.entity.Books;
import com.cqu.springboot.mapper.BooksMapper;
import com.cqu.springboot.service.BooksService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.util.List;

/**
 * <p>
 * 书籍表 服务实现类
 * </p>
 *
 * @author MisterDong
 * @since 2026-06-23
 */
@Service
public class BooksServiceImpl extends ServiceImpl<BooksMapper, Books> implements BooksService {

    @Override
    public List<Books> findall() {
        QueryWrapper<Books> queryWrapper = new QueryWrapper<>();
        return this.baseMapper.selectList(queryWrapper);
    }
}
