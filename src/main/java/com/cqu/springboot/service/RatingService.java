package com.cqu.springboot.service;

import java.util.Map;

/**
 * 图书评分服务接口
 */
public interface RatingService {

    /**
     * 提交评分（upsert逻辑：存在则更新，不存在则插入）
     */
    void submitRating(Long userId, Long bookId, Byte score);

    /**
     * 获取评分统计（平均分、分布）
     */
    Map<String, Object> getRatingStats(Long bookId);
}
