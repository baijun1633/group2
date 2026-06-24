package com.cqu.springboot.service;

import com.cqu.springboot.entity.Books;
import com.baomidou.mybatisplus.extension.service.IService;

import java.awt.*;
import java.util.List;

/**
 * <p>
 * 书籍表 服务类
 * </p>
 *
 * @author MisterDong
 * @since 2026-06-23
 */
public interface BooksService extends IService<Books> {
    List<Books> findall();
}
