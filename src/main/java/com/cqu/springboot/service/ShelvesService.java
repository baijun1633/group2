package com.cqu.springboot.service;

import com.cqu.springboot.entity.Shelves;
import com.cqu.springboot.entity.ShelfBooks;

import java.util.List;
import java.util.Map;

/**
 * 书架管理服务接口
 */
public interface ShelvesService {

    /**
     * 创建书架
     */
    Shelves createShelf(Long userId, String name, String description);

    /**
     * 获取用户的书架列表（含图书数量）
     */
    List<Map<String, Object>> getUserShelves(Long userId);

    /**
     * 获取书架详情
     */
    Map<String, Object> getShelfDetail(Long userId, Long shelfId);

    /**
     * 添加图书到书架
     */
    void addBookToShelf(Long userId, Long shelfId, Long bookId, Integer readingStatus);

    /**
     * 更新书架中图书的阅读状态
     */
    void updateBookReadingStatus(Long userId, Long shelfId, Long bookId, Integer readingStatus);

    /**
     * 从书架移除图书
     */
    void removeBookFromShelf(Long userId, Long shelfId, Long bookId);

    /**
     * 删除书架
     */
    void deleteShelf(Long userId, Long shelfId);
}
