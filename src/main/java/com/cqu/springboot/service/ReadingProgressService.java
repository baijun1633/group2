package com.cqu.springboot.service;

import com.cqu.springboot.common.PageResponse;
import com.cqu.springboot.dto.ReadingProgressRequest;
import com.cqu.springboot.entity.ReadingProgress;

import java.util.Map;

/**
 * 阅读进度服务
 */
public interface ReadingProgressService {

    /**
     * 记录阅读（上报进度 + 本次时长）
     * - upsert reading_progress（累加 readDuration，更新 currentPage/chapter/percentage）
     * - 同时记录一条 user_behaviors(type=read, duration=本次时长) 用于周/月统计
     *
     * @param userId  用户ID
     * @param request 上报请求
     * @return 阅读进度记录ID
     */
    Long recordReading(Long userId, ReadingProgressRequest request);

    /**
     * 获取某本书的阅读进度
     */
    ReadingProgress getReadingProgress(Long userId, Long bookId);

    /**
     * 分页查询阅读历史
     *
     * @param userId 用户ID
     * @param status 状态过滤：reading(在读 0<=percentage<100) / finished(已读 percentage>=100) / null(全部)
     */
    PageResponse<ReadingProgress> getReadingHistory(Long userId, String status, int page, int size);

    /**
     * 阅读统计：周时长、月时长、已读完数量、在读数量
     * - 周/月时长基于 user_behaviors(type=read) 的 SUM(duration)
     * - 已读完/在读基于 reading_progress.percentage
     */
    Map<String, Object> getReadingStats(Long userId);
}
