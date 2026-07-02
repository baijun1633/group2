package com.cqu.springboot.controller;

import com.cqu.springboot.common.ApiResponse;
import com.cqu.springboot.common.BusinessException;
import com.cqu.springboot.common.ErrorCode;
import com.cqu.springboot.event.UserKgChangeEvent;
import com.cqu.springboot.security.SecurityUtils;
import com.cqu.springboot.service.RatingService;
import com.cqu.springboot.service.UserBehaviorService;
import org.springframework.context.ApplicationEventPublisher;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 图书评分控制器
 * 路径: /api/v1/books/{bookId}/rating
 */
@Tag(name = "图书评分", description = "提交评分、获取评分统计")
@RestController
@RequestMapping("/api/v1/books/{bookId}/rating")
@RequiredArgsConstructor
public class RatingController {

    private final RatingService ratingService;
    private final UserBehaviorService userBehaviorService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 提交评分
     * POST /api/v1/books/{bookId}/rating
     */
    @Operation(summary = "提交评分", description = "提交图书评分（1-5分），支持新增和更新")
    @PostMapping
    public ApiResponse<Void> submitRating(
            @Parameter(description = "图书ID") @PathVariable Long bookId,
            @RequestBody Map<String, Object> request) {

        Long userId = SecurityUtils.getCurrentUserId();
        Integer scoreInt = (Integer) request.get("score");
        if (scoreInt == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "评分不能为空");
        }
        Byte score = scoreInt.byteValue();

        ratingService.submitRating(userId, bookId, score);
        // 行为埋点：记录评分行为（behaviorValue 存分数）
        userBehaviorService.recordBehavior(userId, bookId, "rate", String.valueOf(score), null);
        // 触发知识图谱变更事件
        eventPublisher.publishEvent(new UserKgChangeEvent(this, userId, bookId, UserKgChangeEvent.Action.ADD));
        return ApiResponse.success("评分成功", null);
    }

    /**
     * 获取评分统计
     * GET /api/v1/books/{bookId}/rating
     */
    @Operation(summary = "获取评分统计", description = "获取图书评分统计（平均分、评分分布）")
    @GetMapping
    public ApiResponse<Map<String, Object>> getRatingStats(@Parameter(description = "图书ID") @PathVariable Long bookId) {
        Map<String, Object> stats = ratingService.getRatingStats(bookId);
        return ApiResponse.success(stats);
    }
}
