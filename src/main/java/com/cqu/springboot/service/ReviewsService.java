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

    /**
     * 标记/取消标记优质书评
     *
     * @param reviewId 书评ID
     * @param featured true-标记优质, false-取消标记
     */
    void markFeatured(Long reviewId, boolean featured);

    /**
     * 获取优质书评列表
     */
    Map<String, Object> getFeaturedReviews(int page, int size);

    /**
     * 批量软删除书评（违规评论清理）
     *
     * @param reviewIds 要删除的书评ID列表
     * @return 实际删除数量
     */
    int batchDeleteReviews(List<Long> reviewIds);

    /**
     * 搜索书评（按关键词匹配内容，支持分页）
     *
     * @param keyword  搜索关键词（匹配书评内容）
     * @param status   状态过滤（null=全部）
     * @param page     页码
     * @param size     每页数量
     * @return 书评列表
     */
    Map<String, Object> searchReviews(String keyword, Byte status, int page, int size);
}
