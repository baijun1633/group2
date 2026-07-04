package com.cqu.springboot.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cqu.springboot.entity.Books;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

/**
 * 书籍表 服务类
 */
public interface BooksService extends IService<Books> {

    /**
     * 多维度搜索（按书名/作者/ISBN/出版社/标签）
     */
    Page<Books> searchBooks(String keyword, String publisher, String tag, int page, int size);

    /**
     * 分类/标签筛选 + 排序
     */
    Page<Books> filterBooks(Long categoryId, String tag, String sortBy, String order, int page, int size);

    /**
     * 获取图书详情
     */
    Map<String, Object> getBookDetail(Long bookId);
}
