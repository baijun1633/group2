package com.cqu.springboot.controller;

import com.cqu.springboot.common.ApiResponse;
import com.cqu.springboot.entity.Shelves;
import com.cqu.springboot.security.SecurityUtils;
import com.cqu.springboot.service.RecommendationService;
import com.cqu.springboot.service.ShelvesService;
import com.cqu.springboot.service.UserBehaviorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 书架管理控制器
 * 路径: /api/v1/shelves
 */
@Tag(name = "书架管理", description = "用户书架的创建、图书添加和管理")
@RestController
@RequestMapping("/api/v1/shelves")
@RequiredArgsConstructor
public class ShelvesController {

    private final ShelvesService shelvesService;
    private final UserBehaviorService userBehaviorService;
    private final RecommendationService recommendationService;

    /**
     * 创建书架
     * POST /api/v1/shelves
     */
    @Operation(summary = "创建书架", description = "创建一个新的书架")
    @PostMapping
    public ApiResponse<Map<String, Object>> createShelf(
            @Parameter(description = "书架名称", example = "我的科幻书架") @RequestParam String name,
            @Parameter(description = "书架描述", example = "收藏我喜欢的科幻小说") @RequestParam(required = false) String description) {
        Long userId = SecurityUtils.getCurrentUserId();
        Shelves shelf = shelvesService.createShelf(userId, name, description);

        return ApiResponse.success(Map.of(
                "shelfId", shelf.getShelfId(),
                "name", shelf.getName(),
                "description", shelf.getDescription() != null ? shelf.getDescription() : "",
                "createTime", shelf.getCreateTime()
        ));
    }

    /**
     * 获取书架列表
     * GET /api/v1/shelves
     */
    @Operation(summary = "获取书架列表", description = "获取当前用户的所有书架，包含每个书架的图书数量")
    @GetMapping
    public ApiResponse<List<Map<String, Object>>> getUserShelves() {
        Long userId = SecurityUtils.getCurrentUserId();
        List<Map<String, Object>> shelves = shelvesService.getUserShelves(userId);
        return ApiResponse.success(shelves);
    }

    /**
     * 获取书架详情
     * GET /api/v1/shelves/{shelfId}
     */
    @Operation(summary = "获取书架详情", description = "获取指定书架的详细信息，包括其中的图书列表")
    @GetMapping("/{shelfId}")
    public ApiResponse<Map<String, Object>> getShelfDetail(
            @Parameter(description = "书架ID", example = "1") @PathVariable Long shelfId) {
        Long userId = SecurityUtils.getCurrentUserId();
        Map<String, Object> detail = shelvesService.getShelfDetail(userId, shelfId);
        return ApiResponse.success(detail);
    }

    /**
     * 添加图书到书架
     * POST /api/v1/shelves/{shelfId}/books
     */
    @Operation(summary = "添加图书到书架", description = "将指定图书添加到书架中")
    @PostMapping("/{shelfId}/books")
    public ApiResponse<Void> addBookToShelf(
            @Parameter(description = "书架ID", example = "1") @PathVariable Long shelfId,
            @Parameter(description = "图书ID", example = "1") @RequestParam Long bookId,
            @Parameter(description = "阅读状态：0-未读, 1-在读, 2-已读", example = "0") @RequestParam(required = false, defaultValue = "0") Integer readingStatus) {
        Long userId = SecurityUtils.getCurrentUserId();
        shelvesService.addBookToShelf(userId, shelfId, bookId, readingStatus);
        // 行为埋点：记录收藏行为
        userBehaviorService.recordBehavior(userId, bookId, "collect", null, null);
        // 联动：标记推荐记录 is_collected=1（失败不影响主业务）
        try {
            recommendationService.recordCollect(userId, bookId);
        } catch (Exception e) {
            // ignore：仅记录日志，不影响加书架主流程
        }
        return ApiResponse.success("添加成功", null);
    }

    /**
     * 更新图书阅读状态
     * PUT /api/v1/shelves/{shelfId}/books/{bookId}
     */
    @Operation(summary = "更新阅读状态", description = "更新书架中某本图书的阅读状态")
    @PutMapping("/{shelfId}/books/{bookId}")
    public ApiResponse<Void> updateBookReadingStatus(
            @Parameter(description = "书架ID", example = "1") @PathVariable Long shelfId,
            @Parameter(description = "图书ID", example = "1") @PathVariable Long bookId,
            @Parameter(description = "阅读状态：0-未读, 1-在读, 2-已读", example = "1") @RequestParam Integer readingStatus) {
        Long userId = SecurityUtils.getCurrentUserId();
        shelvesService.updateBookReadingStatus(userId, shelfId, bookId, readingStatus);
        return ApiResponse.success("更新成功", null);
    }

    /**
     * 从书架移除图书
     * DELETE /api/v1/shelves/{shelfId}/books/{bookId}
     */
    @Operation(summary = "移除图书", description = "从书架中移除指定图书")
    @DeleteMapping("/{shelfId}/books/{bookId}")
    public ApiResponse<Void> removeBookFromShelf(
            @Parameter(description = "书架ID", example = "1") @PathVariable Long shelfId,
            @Parameter(description = "图书ID", example = "1") @PathVariable Long bookId) {
        Long userId = SecurityUtils.getCurrentUserId();
        shelvesService.removeBookFromShelf(userId, shelfId, bookId);
        // 联动减少 books.collect_count（失败不影响主业务）
        userBehaviorService.decrementCollectCount(bookId);
        return ApiResponse.success("移除成功", null);
    }

    /**
     * 删除书架
     * DELETE /api/v1/shelves/{shelfId}
     */
    @Operation(summary = "删除书架", description = "删除指定书架及其所有图书关联")
    @DeleteMapping("/{shelfId}")
    public ApiResponse<Void> deleteShelf(
            @Parameter(description = "书架ID", example = "1") @PathVariable Long shelfId) {
        Long userId = SecurityUtils.getCurrentUserId();
        shelvesService.deleteShelf(userId, shelfId);
        return ApiResponse.success("删除成功", null);
    }
}
