package com.cqu.springboot.controller;

import com.cqu.springboot.common.ApiResponse;
import com.cqu.springboot.common.BusinessException;
import com.cqu.springboot.common.ErrorCode;
import com.cqu.springboot.security.SecurityUtils;
import com.cqu.springboot.service.RatingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 图书评分控制器
 * 路径: /api/v1/books/{bookId}/rating
 */
@RestController
@RequestMapping("/api/v1/books/{bookId}/rating")
@RequiredArgsConstructor
public class RatingController {

    private final RatingService ratingService;

    /**
     * 提交评分
     * POST /api/v1/books/{bookId}/rating
     */
    @PostMapping
    public ApiResponse<Void> submitRating(
            @PathVariable Long bookId,
            @RequestBody Map<String, Object> request) {

        Long userId = SecurityUtils.getCurrentUserId();
        Integer scoreInt = (Integer) request.get("score");
        if (scoreInt == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "评分不能为空");
        }
        Byte score = scoreInt.byteValue();

        ratingService.submitRating(userId, bookId, score);
        return ApiResponse.success("评分成功", null);
    }

    /**
     * 获取评分统计
     * GET /api/v1/books/{bookId}/rating
     */
    @GetMapping
    public ApiResponse<Map<String, Object>> getRatingStats(@PathVariable Long bookId) {
        Map<String, Object> stats = ratingService.getRatingStats(bookId);
        return ApiResponse.success(stats);
    }
}
