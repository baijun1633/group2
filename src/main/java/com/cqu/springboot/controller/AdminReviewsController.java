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

import java.util.Map;

/**
 * 管理员书评审核控制器
 * 路径: /api/v1/admin/reviews
 */
@Tag(name = "书评审核管理", description = "管理员审核书评")
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
}
