package com.cqu.springboot.service;

import com.cqu.springboot.common.PageResponse;
import com.cqu.springboot.entity.UserBehavior;

/**
 * 用户行为采集服务
 */
public interface UserBehaviorService {

    /**
     * 记录行为（view自带5分钟去重，其余每次都记）
     *
     * @param userId        用户ID（为 null 时直接返回，不记录）
     * @param bookId        图书ID（搜索等场景可为 null）
     * @param behaviorType  行为类型：view/search/rate/collect/review
     * @param behaviorValue 行为内容（如搜索关键词、评分分数、评论内容）
     * @param duration      停留时长（秒），可为 null
     */
    void recordBehavior(Long userId, Long bookId, String behaviorType, String behaviorValue, Integer duration);

    /**
     * 分页查询用户行为历史
     *
     * @param userId       用户ID
     * @param behaviorType 行为类型，为 null 时查询全部
     * @param page         页码（从1开始）
     * @param size         每页数量
     * @return 行为记录分页数据
     */
    PageResponse<UserBehavior> listUserBehaviors(Long userId, String behaviorType, int page, int size);

    /**
     * 取消收藏时联动减少 collect_count（不低于 0）
     *
     * @param bookId 图书ID
     */
    void decrementCollectCount(Long bookId);
}
