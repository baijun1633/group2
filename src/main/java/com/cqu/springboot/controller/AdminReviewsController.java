package com.cqu.springboot.controller;

import com.cqu.springboot.common.ApiResponse;
import com.cqu.springboot.common.BusinessException;
import com.cqu.springboot.common.ErrorCode;
import com.cqu.springboot.service.ReviewsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 管理员书评审核控制器
 * 路径: /api/v1/admin/reviews
 */
@Tag(name = "书评审核管理", description = "管理员审核书评、优质标记、违规清理")
@RestController
@RequestMapping("/api/v1/admin/reviews")
@RequiredArgsConstructor
public class AdminReviewsController {

    private final ReviewsService reviewsService;

    /**
     * 获取待审核书评列表
     * GET /api/v1/admin/reviews/pending?page=&size=
     */
    @Operation(summary = "获取待审核书评列表", description = "分页获取状态为待审核的书评")
    @GetMapping("/pending")
    public ApiResponse<Map<String, Object>> getPendingReviews(
            @Parameter(description = "页码", example = "1") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量", example = "20") @RequestParam(defaultValue = "20") int size) {

        Map<String, Object> pendingReviews = reviewsService.getPendingReviews(page, size);
        return ApiResponse.success(pendingReviews);
    }

    /**
     * 审核书评
     * PUT /api/v1/admin/reviews/{reviewId}/audit
     */
    @Operation(summary = "审核书评", description = "审核书评：approve-通过, reject-拒绝, delete-删除")
    @PutMapping("/{reviewId}/audit")
    public ApiResponse<Void> auditReview(
            @Parameter(description = "书评ID", example = "1") @PathVariable Long reviewId,
            @RequestBody Map<String, String> request) {

        String action = request.get("action");
        if (action == null || action.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "审核操作不能为空");
        }

        reviewsService.auditReview(reviewId, action);
        String message = switch (action) {
            case "approve" -> "审核通过";
            case "reject" -> "已拒绝";
            case "delete" -> "已删除";
            default -> "操作完成";
        };
        return ApiResponse.success(message, null);
    }

    /**
     * 标记/取消标记优质书评
     * PUT /api/v1/admin/reviews/{reviewId}/featured
     */
    @Operation(summary = "标记优质书评", description = "标记或取消标记书评为优质书评（仅已通过审核的书评可标记）")
    @PutMapping("/{reviewId}/featured")
    public ApiResponse<Void> markFeatured(
            @Parameter(description = "书评ID", example = "1") @PathVariable Long reviewId,
            @RequestBody Map<String, Boolean> request) {

        Boolean featured = request.get("featured");
        if (featured == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "featured 参数不能为空");
        }
        reviewsService.markFeatured(reviewId, featured);
        String message = featured ? "已标记为优质书评" : "已取消优质书评标记";
        return ApiResponse.success(message, null);
    }

    /**
     * 获取优质书评列表
     * GET /api/v1/admin/reviews/featured?page=&size=
     */
    @Operation(summary = "获取优质书评列表", description = "分页获取已标记为优质的已通过书评")
    @GetMapping("/featured")
    public ApiResponse<Map<String, Object>> getFeaturedReviews(
            @Parameter(description = "页码", example = "1") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量", example = "20") @RequestParam(defaultValue = "20") int size) {

        Map<String, Object> result = reviewsService.getFeaturedReviews(page, size);
        return ApiResponse.success(result);
    }

    /**
     * 批量软删除书评（违规评论清理）
     * POST /api/v1/admin/reviews/batch-delete
     */
    @Operation(summary = "批量删除书评", description = "批量软删除书评（状态改为已删除），用于违规评论清理")
    @PostMapping("/batch-delete")
    public ApiResponse<Map<String, Object>> batchDeleteReviews(
            @RequestBody Map<String, List<Long>> request) {

        List<Long> reviewIds = request.get("reviewIds");
        if (reviewIds == null || reviewIds.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "书评ID列表不能为空");
        }
        int deleted = reviewsService.batchDeleteReviews(reviewIds);
        return ApiResponse.success("批量删除完成", Map.of(
                "requested", reviewIds.size(),
                "deleted", deleted
        ));
    }

    /**
     * 搜索书评（用于查找违规内容）
     * GET /api/v1/admin/reviews/search?keyword=&status=&page=&size=
     */
    @Operation(summary = "搜索书评", description = "按关键词搜索书评内容，可按状态过滤，用于查找违规评论")
    @GetMapping("/search")
    public ApiResponse<Map<String, Object>> searchReviews(
            @Parameter(description = "搜索关键词（匹配书评内容）") @RequestParam(required = false) String keyword,
            @Parameter(description = "状态过滤: 0-待审核, 1-已通过, 2-已拒绝, 不传=全部(不含已删除)") @RequestParam(required = false) Byte status,
            @Parameter(description = "页码", example = "1") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量", example = "20") @RequestParam(defaultValue = "20") int size) {

        Map<String, Object> result = reviewsService.searchReviews(keyword, status, page, size);
        return ApiResponse.success(result);
    }
}
