package com.cqu.springboot.controller;

import com.cqu.springboot.common.ApiResponse;
import com.cqu.springboot.common.BusinessException;
import com.cqu.springboot.common.ErrorCode;
import com.cqu.springboot.event.UserKgChangeEvent;
import com.cqu.springboot.security.SecurityUtils;
import com.cqu.springboot.service.ReviewsService;
import com.cqu.springboot.service.UserBehaviorService;
import org.springframework.context.ApplicationEventPublisher;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 书评控制器
 * 路径: /api/v1/books/{bookId}/reviews 及 /api/v1/reviews
 */
@Tag(name = "书评管理", description = "书评发布、列表、点赞和回复")
@RestController
@RequiredArgsConstructor
public class ReviewsController {

    private final ReviewsService reviewsService;
    private final UserBehaviorService userBehaviorService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 发布书评
     * POST /api/v1/books/{bookId}/reviews
     */
    @Operation(summary = "发布书评", description = "发布书评，状态为待审核")
    @PostMapping("/api/v1/books/{bookId}/reviews")
    public ApiResponse<Map<String, Object>> createReview(
            @Parameter(description = "书籍ID", example = "1") @PathVariable Long bookId,
            @RequestBody Map<String, String> request) {

        Long userId = SecurityUtils.getCurrentUserId();
        String content = request.get("content");
        String markdown = request.get("markdown");

        if (content == null || content.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "书评内容不能为空");
        }

        Long reviewId = reviewsService.createReview(userId, bookId, content, markdown);
        // 行为埋点：记录书评行为（behaviorValue 存评论内容，超500字符截断避免超长）
        String reviewSnippet = content.length() > 500 ? content.substring(0, 500) : content;
        userBehaviorService.recordBehavior(userId, bookId, "review", reviewSnippet, null);
        // 触发知识图谱变更事件
        eventPublisher.publishEvent(new UserKgChangeEvent(this, userId, bookId, UserKgChangeEvent.Action.ADD));

        Map<String, Object> data = new HashMap<>();
        data.put("reviewId", reviewId);
        data.put("status", "pending_audit");
        return ApiResponse.success("发布成功，待审核", data);
    }

    /**
     * 获取书评列表
     * GET /api/v1/books/{bookId}/reviews?sortBy=&page=&size=
     */
    @Operation(summary = "获取书评列表", description = "获取指定书籍的书评列表，仅返回已通过审核的书评")
    @GetMapping("/api/v1/books/{bookId}/reviews")
    public ApiResponse<Map<String, Object>> getBookReviews(
            @Parameter(description = "书籍ID", example = "1") @PathVariable Long bookId,
            @Parameter(description = "排序方式: likes-按点赞数, time-按时间", example = "likes")
            @RequestParam(defaultValue = "likes") String sortBy,
            @Parameter(description = "页码", example = "1") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量", example = "10") @RequestParam(defaultValue = "10") int size) {

        Map<String, Object> reviews = reviewsService.getBookReviews(bookId, sortBy, page, size);
        return ApiResponse.success(reviews);
    }

    /**
     * 删除自己的书评
     * DELETE /api/v1/reviews/{reviewId}
     */
    @Operation(summary = "删除书评", description = "删除自己的书评（仅未审核或已通过可删）")
    @DeleteMapping("/api/v1/reviews/{reviewId}")
    public ApiResponse<Void> deleteReview(
            @Parameter(description = "书评ID", example = "1") @PathVariable Long reviewId) {

        Long userId = SecurityUtils.getCurrentUserId();
        reviewsService.deleteReview(userId, reviewId);
        return ApiResponse.success("删除成功", null);
    }

    /**
     * 点赞/取消点赞
     * POST /api/v1/reviews/{reviewId}/like
     */
    @Operation(summary = "点赞/取消点赞", description = "toggle逻辑，已点赞则取消，未点赞则点赞")
    @PostMapping("/api/v1/reviews/{reviewId}/like")
    public ApiResponse<Map<String, Object>> toggleLike(
            @Parameter(description = "书评ID", example = "1") @PathVariable Long reviewId) {

        Long userId = SecurityUtils.getCurrentUserId();
        boolean liked = reviewsService.toggleLike(userId, reviewId);

        Map<String, Object> data = new HashMap<>();
        data.put("liked", liked);
        data.put("message", liked ? "点赞成功" : "取消点赞");
        return ApiResponse.success(data);
    }

    /**
     * 回复书评
     * POST /api/v1/reviews/{reviewId}/reply
     */
    @Operation(summary = "回复书评", description = "对指定书评进行回复")
    @PostMapping("/api/v1/reviews/{reviewId}/reply")
    public ApiResponse<Map<String, Object>> replyReview(
            @Parameter(description = "书评ID", example = "1") @PathVariable Long reviewId,
            @RequestBody Map<String, String> request) {

        Long userId = SecurityUtils.getCurrentUserId();
        String content = request.get("content");
        if (content == null || content.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "回复内容不能为空");
        }

        Long replyId = reviewsService.replyReview(userId, reviewId, content);

        Map<String, Object> data = new HashMap<>();
        data.put("replyId", replyId);
        return ApiResponse.success("回复成功", data);
    }

    /**
     * 获取书评的回复列表
     * GET /api/v1/reviews/{reviewId}/replies
     */
    @Operation(summary = "获取书评回复列表", description = "获取指定书评的所有回复")
    @GetMapping("/api/v1/reviews/{reviewId}/replies")
    public ApiResponse<List<Map<String, Object>>> getReviewReplies(
            @Parameter(description = "书评ID", example = "1") @PathVariable Long reviewId) {

        List<Map<String, Object>> replies = reviewsService.getReviewReplies(reviewId);
        return ApiResponse.success(replies);
    }
}
