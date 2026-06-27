package com.cqu.springboot.service;

import java.util.List;
import java.util.Map;

/**
 * 书评服务接口
 */
public interface ReviewsService {

    /**
     * 发布书评（状态pending_audit）
     */
    Long createReview(Long userId, Long bookId, String content, String markdown);

    /**
     * 获取某本书的书评列表
     */
    Map<String, Object> getBookReviews(Long bookId, String sortBy, int page, int size);

    /**
     * 删除自己的书评（仅未审核或已通过可删）
     */
    void deleteReview(Long userId, Long reviewId);

    /**
     * 点赞/取消点赞（toggle逻辑）
     *
     * @return true-已点赞, false-已取消
     */
    boolean toggleLike(Long userId, Long reviewId);

    /**
     * 回复书评
     */
    Long replyReview(Long userId, Long reviewId, String content);

    /**
     * 获取书评的回复列表
     */
    List<Map<String, Object>> getReviewReplies(Long reviewId);

    /**
     * 管理员获取待审核书评列表
     */
    Map<String, Object> getPendingReviews(int page, int size);

    /**
     * 管理员审核书评（approve/reject/delete）
     */
    void auditReview(Long reviewId, String action);
}
