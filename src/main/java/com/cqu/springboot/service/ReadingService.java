package com.cqu.springboot.service;

import com.cqu.springboot.entity.ReadingProgress;

import java.util.List;
import java.util.Map;

/**
 * 阅读进度服务接口
 */
public interface ReadingService {

    /**
     * 同步阅读进度（upsert）
     */
    void syncProgress(Long userId, Long bookId, Integer currentPage, Integer totalPages, String device);

    /**
     * 获取某本书的阅读进度
     */
    ReadingProgress getProgress(Long userId, Long bookId);

    /**
     * 获取阅读历史
     */
    Map<String, Object> getReadingHistory(Long userId, int page, int size);
}
