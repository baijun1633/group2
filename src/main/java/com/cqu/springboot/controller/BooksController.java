package com.cqu.springboot.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cqu.springboot.common.ApiResponse;
import com.cqu.springboot.common.PageResponse;
import com.cqu.springboot.entity.Books;
import com.cqu.springboot.service.BooksService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 图书控制器（公开接口）
 * 路径: /api/v1/books
 */
@RestController
@RequestMapping("/api/v1/books")
@RequiredArgsConstructor
public class BooksController {

    private final BooksService booksService;

    /**
     * 多维度搜索
     * GET /api/v1/books/search?keyword=&page=&size=
     */
    @GetMapping("/search")
    public ApiResponse<PageResponse<Books>> searchBooks(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<Books> result = booksService.searchBooks(keyword, page, size);
        PageResponse<Books> response = new PageResponse<>(
                result.getRecords(), result.getTotal(), page, size);
        return ApiResponse.success(response);
    }

    /**
     * 分类/标签筛选 + 排序
     * GET /api/v1/books?categoryId=&tag=&sortBy=&order=&page=&size=
     */
    @GetMapping
    public ApiResponse<PageResponse<Books>> filterBooks(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false, defaultValue = "createTime") String sortBy,
            @RequestParam(required = false, defaultValue = "desc") String order,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<Books> result = booksService.filterBooks(categoryId, tag, sortBy, order, page, size);
        PageResponse<Books> response = new PageResponse<>(
                result.getRecords(), result.getTotal(), page, size);
        return ApiResponse.success(response);
    }

    /**
     * 获取图书详情
     * GET /api/v1/books/{bookId}
     */
    @GetMapping("/{bookId}")
    public ApiResponse<Map<String, Object>> getBookDetail(@PathVariable Long bookId) {
        Map<String, Object> detail = booksService.getBookDetail(bookId);
        return ApiResponse.success(detail);
    }
}
