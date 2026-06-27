package com.cqu.springboot.service;

import com.cqu.springboot.dto.RecommendItem;

import java.util.List;

/**
 * 协同过滤推荐服务接口（ItemCF）
 */
public interface CollaborativeFilteringService {

    /**
     * 基于ItemCF为用户推荐图书
     * 原理：找到与用户已读/已评分图书最相似的书
     *
     * @param userId 用户ID
     * @param limit  返回数量
     * @return 推荐列表
     */
    List<RecommendItem> recommendByItemCF(Long userId, int limit);
}
