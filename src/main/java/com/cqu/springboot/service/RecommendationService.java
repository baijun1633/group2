package com.cqu.springboot.service;

import com.cqu.springboot.common.PageResponse;
import com.cqu.springboot.dto.RecommendItem;
import com.cqu.springboot.entity.Recommendation;

import java.util.List;

/**
 * 推荐记录持久化服务
 * 负责推荐结果落库、点击/收藏回调、推荐历史查询
 */
public interface RecommendationService {

    /**
     * 批量落库推荐结果（upsert：已存在则更新 score/reason/time，保留 is_clicked/is_collected）
     * 落库后回填 recommendId 到 RecommendItem，供前端点击回调使用
     *
     * @param userId     用户ID（为 null 时直接返回，不落库）
     * @param items      推荐结果列表
     * @param sourceType 推荐来源类型：home/similar/extended
     */
    void generateRecommendation(Long userId, List<RecommendItem> items, String sourceType);

    /**
     * 记录点击（标记 is_clicked=1）
     * 按 recommendId + userId 更新（加 userId 防越权）
     *
     * @param recommendId 推荐记录ID
     * @param userId       用户ID
     * @return 是否更新成功
     */
    boolean recordClick(Long recommendId, Long userId);

    /**
     * 记录收藏（标记 is_collected=1，取消收藏时不改，保留历史状态）
     * 按 userId + bookId 更新
     *
     * @param userId 用户ID
     * @param bookId 图书ID
     */
    void recordCollect(Long userId, Long bookId);

    /**
     * 分页查询推荐历史
     *
     * @param userId    用户ID
     * @param clicked   是否只看已点击的，null 表示不过滤
     * @param collected 是否只看已收藏的，null 表示不过滤
     * @param page      页码（从1开始）
     * @param size      每页数量
     * @return 推荐记录分页数据
     */
    PageResponse<Recommendation> getRecommendHistory(Long userId, Boolean clicked, Boolean collected, int page, int size);
}
