package com.cqu.springboot.service;

import com.cqu.springboot.dto.RecommendItem;

import java.util.List;

/**
 * 知识图谱推理推荐服务接口
 */
public interface KgRecommendService {

    /**
     * 基于知识图谱为用户推荐图书（个性化）
     * 根据用户画像（偏好分类/作者/标签）在图谱中查找关联图书
     *
     * @param userId 用户ID
     * @param limit  返回数量
     * @return 推荐列表（含推理路径）
     */
    List<RecommendItem> recommendByKg(Long userId, int limit);

    /**
     * 获取与指定图书相似的图书（1-2跳）
     * 同作者、同分类、同标签（基于用户个人图谱）
     *
     * @param userId 用户ID（可为null，为null时返回空）
     * @param bookId 图书ID
     * @param limit  返回数量
     * @return 推荐列表
     */
    List<RecommendItem> getSimilarBooks(Long userId, Long bookId, int limit);

    /**
     * 获取延伸阅读（3跳及以上路径推理）
     * 通过多跳路径发现间接关联的高质量图书（基于用户个人图谱）
     *
     * @param userId 用户ID（可为null，为null时返回空）
     * @param bookId 图书ID
     * @param limit  返回数量
     * @return 推荐列表（含推理路径）
     */
    List<RecommendItem> getExtendedBooks(Long userId, Long bookId, int limit);
}
