package com.cqu.springboot.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cqu.springboot.common.ApiResponse;
import com.cqu.springboot.common.BusinessException;
import com.cqu.springboot.common.ErrorCode;
import com.cqu.springboot.common.PageResponse;
import com.cqu.springboot.config.FileConfig;
import com.cqu.springboot.dto.EbookInfo;
import com.cqu.springboot.entity.Books;
import com.cqu.springboot.security.SecurityUtils;
import com.cqu.springboot.service.BooksService;
import com.cqu.springboot.service.EbookService;
import com.cqu.springboot.service.UserBehaviorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 图书控制器（公开接口）
 * 路径: /api/v1/books
 */
@Tag(name = "图书管理", description = "图书浏览、搜索、筛选相关接口")
@RestController
@RequestMapping("/api/v1/books")
@RequiredArgsConstructor
public class BooksController {

    private final BooksService booksService;
    private final UserBehaviorService userBehaviorService;
    private final EbookService ebookService;
    private final FileConfig fileConfig;

    /**
     * 多维度搜索
     * GET /api/v1/books/search?keyword=&publisher=&tag=&page=&size=
     */
    @Operation(summary = "多维度搜索图书", description = "按书名、作者、ISBN、出版社、标签进行模糊搜索")
    @GetMapping("/search")
    public ApiResponse<PageResponse<Books>> searchBooks(
            @Parameter(description = "搜索关键词（书名、作者或ISBN）", example = "三体") @RequestParam(required = false) String keyword,
            @Parameter(description = "出版社关键词", example = "人民文学") @RequestParam(required = false) String publisher,
            @Parameter(description = "标签关键词", example = "科幻") @RequestParam(required = false) String tag,
            @Parameter(description = "页码（从1开始）", example = "1") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量", example = "20") @RequestParam(defaultValue = "20") int size) {

        // 行为埋点：记录搜索关键词（bookId 传 null，behaviorValue 存搜索关键词）
        userBehaviorService.recordBehavior(SecurityUtils.getCurrentUserIdOrNull(), null, "search", keyword, null);

        Page<Books> result = booksService.searchBooks(keyword, publisher, tag, page, size);
        fixBookCoverImageUrls(result.getRecords());
        PageResponse<Books> response = new PageResponse<>(
                result.getRecords(), result.getTotal(), page, size);
        return ApiResponse.success(response);
    }

    /**
     * 分类/标签筛选 + 排序
     * GET /api/v1/books?categoryId=&tag=&sortBy=&order=&page=&size=
     */
    @Operation(summary = "分类/标签筛选图书", description = "支持按分类ID、标签筛选，并可按评分、出版日期、创建时间排序")
    @GetMapping
    public ApiResponse<PageResponse<Books>> filterBooks(
            @Parameter(description = "分类ID（可通过GET /api/v1/categories查询所有分类）", example = "1") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "标签名称（如：科幻、文学、历史等）", example = "科幻") @RequestParam(required = false) String tag,
            @Parameter(description = "排序字段：createTime-创建时间, rating-评分, publish_date-出版日期", example = "createTime") @RequestParam(required = false, defaultValue = "createTime") String sortBy,
            @Parameter(description = "排序方向：asc-升序, desc-降序", example = "desc") @RequestParam(required = false, defaultValue = "desc") String order,
            @Parameter(description = "页码（从1开始）", example = "1") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量", example = "20") @RequestParam(defaultValue = "20") int size) {

        Page<Books> result = booksService.filterBooks(categoryId, tag, sortBy, order, page, size);
        fixBookCoverImageUrls(result.getRecords());
        PageResponse<Books> response = new PageResponse<>(
                result.getRecords(), result.getTotal(), page, size);
        return ApiResponse.success(response);
    }

    /**
     * 获取图书详情
     * GET /api/v1/books/{bookId}
     */
    @Operation(summary = "获取图书详情", description = "获取图书的详细信息，包括标签、购书链接等")
    @GetMapping("/{bookId}")
    public ApiResponse<Map<String, Object>> getBookDetail(@Parameter(description = "图书ID", example = "1") @PathVariable Long bookId) {
        Map<String, Object> detail = booksService.getBookDetail(bookId);
        // 行为埋点：记录图书浏览（view 自带5分钟去重，未登录不记录）
        userBehaviorService.recordBehavior(SecurityUtils.getCurrentUserIdOrNull(), bookId, "view", null, null);
        return ApiResponse.success(detail);
    }

    /**
     * 图书试读
     * GET /api/v1/books/{bookId}/preview
     * <p>未登录用户可试读前500字符，登录用户可阅读全部内容</p>
     */
    @Operation(summary = "图书试读", description = "获取试读内容，未登录限500字，登录可读全部")
    @GetMapping("/{bookId}/preview")
    public ApiResponse<Map<String, Object>> previewBook(
            @Parameter(description = "图书ID", example = "1") @PathVariable Long bookId) {
        Books book = booksService.getById(bookId);
        if (book == null) {
            throw new BusinessException(ErrorCode.BOOK_NOT_FOUND);
        }

        Long currentUserId = SecurityUtils.getCurrentUserIdOrNull();
        boolean isLogin = currentUserId != null;
        String fullContent = book.getPreviewContent();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("bookId", bookId);
        result.put("title", book.getTitle());
        result.put("isLogin", isLogin);

        if (fullContent == null || fullContent.isBlank()) {
            result.put("previewContent", "");
            result.put("truncated", false);
            result.put("message", "暂无试读内容");
        } else if (isLogin) {
            // 登录用户可读全部
            result.put("previewContent", fullContent);
            result.put("truncated", false);
            result.put("totalLength", fullContent.length());
        } else {
            // 未登录用户限500字符
            int limit = 500;
            boolean truncated = fullContent.length() > limit;
            result.put("previewContent", truncated ? fullContent.substring(0, limit) : fullContent);
            result.put("truncated", truncated);
            result.put("totalLength", fullContent.length());
            if (truncated) {
                result.put("message", "登录后可阅读全部内容");
            }
        }

        return ApiResponse.success(result);
    }

    /**
     * 电子书试读
     * GET /api/v1/books/{bookId}/ebook
     * <p>未登录用户可试读前 3 章，登录用户可试读前 10 章</p>
     * <p>需要管理员先上传 EPUB/PDF 文件（POST /api/v1/admin/books/{bookId}/ebook）</p>
     */
    @Operation(summary = "电子书试读", description = "解析 EPUB/PDF 电子书，未登录返回前3章，登录返回前10章")
    @GetMapping("/{bookId}/ebook")
    public ApiResponse<EbookInfo> getEbookPreview(
            @Parameter(description = "图书ID", example = "1") @PathVariable Long bookId) {
        Long userId = SecurityUtils.getCurrentUserIdOrNull();
        EbookInfo info = ebookService.getEbookPreview(bookId, userId);
        return ApiResponse.success(info);
    }

    private void fixBookCoverImageUrls(List<Books> books) {
        if (books != null) {
            books.forEach(this::fixBookCoverImageUrl);
        }
    }

    private void fixBookCoverImageUrl(Books book) {
        if (book != null) {
            if (book.getCoverImage() != null && !book.getCoverImage().startsWith("http")) {
                book.setCoverImage(fileConfig.getBaseUrl() + book.getCoverImage());
            }
            if (book.getEbookUrl() != null && !book.getEbookUrl().startsWith("http")) {
                book.setEbookUrl(fileConfig.getBaseUrl() + book.getEbookUrl());
            }
        }
    }
}
