package com.cqu.springboot.controller;

import com.cqu.springboot.common.ApiResponse;
import com.cqu.springboot.common.BusinessException;
import com.cqu.springboot.common.ErrorCode;
import com.cqu.springboot.entity.Books;
import com.cqu.springboot.service.BooksService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 管理员图书管理控制器
 * 路径: /api/v1/admin/books
 */
@RestController
@RequestMapping("/api/v1/admin/books")
@RequiredArgsConstructor
public class AdminBooksController {

    private final BooksService booksService;
    private final ObjectMapper objectMapper;

    /**
     * 单本录入
     * POST /api/v1/admin/books
     */
    @PostMapping
    public ApiResponse<Books> createBook(@RequestBody Map<String, Object> request) {
        Books book = new Books();
        book.setTitle((String) request.get("title"));
        book.setAuthor((String) request.get("author"));
        book.setIsbn((String) request.get("isbn"));
        book.setPublisher((String) request.get("publisher"));
        book.setDescription((String) request.get("description"));
        book.setCoverImage((String) request.get("coverImage"));
        book.setStatus((byte) 1);
        book.setCreateTime(LocalDateTime.now());
        book.setUpdateTime(LocalDateTime.now());

        // 处理标签
        if (request.get("tags") != null) {
            try {
                book.setTags(objectMapper.writeValueAsString(request.get("tags")));
            } catch (Exception e) {
                // 忽略
            }
        }

        // 处理数值字段
        if (request.get("price") != null) {
            book.setPrice(new java.math.BigDecimal(request.get("price").toString()));
        }
        if (request.get("stock") != null) {
            book.setStock((Integer) request.get("stock"));
        }
        if (request.get("pages") != null) {
            book.setPages((Integer) request.get("pages"));
        }
        if (request.get("categoryId") != null) {
            book.setCategoryId(Long.valueOf(request.get("categoryId").toString()));
        }
        if (request.get("seriesInfo") != null) {
            book.setSeriesInfo((String) request.get("seriesInfo"));
        }

        booksService.save(book);
        return ApiResponse.success("录入成功", book);
    }

    /**
     * 编辑图书（部分更新）
     * PUT /api/v1/admin/books/{bookId}
     */
    @PutMapping("/{bookId}")
    public ApiResponse<Books> updateBook(
            @PathVariable Long bookId,
            @RequestBody Map<String, Object> request) {

        Books book = booksService.getById(bookId);
        if (book == null) {
            throw new BusinessException(ErrorCode.BOOK_NOT_FOUND);
        }

        // 部分更新
        if (request.containsKey("title")) book.setTitle((String) request.get("title"));
        if (request.containsKey("author")) book.setAuthor((String) request.get("author"));
        if (request.containsKey("isbn")) book.setIsbn((String) request.get("isbn"));
        if (request.containsKey("publisher")) book.setPublisher((String) request.get("publisher"));
        if (request.containsKey("description")) book.setDescription((String) request.get("description"));
        if (request.containsKey("coverImage")) book.setCoverImage((String) request.get("coverImage"));
        if (request.containsKey("seriesInfo")) book.setSeriesInfo((String) request.get("seriesInfo"));

        if (request.containsKey("tags")) {
            try {
                book.setTags(objectMapper.writeValueAsString(request.get("tags")));
            } catch (Exception e) {
                // 忽略
            }
        }
        if (request.containsKey("price")) {
            book.setPrice(new java.math.BigDecimal(request.get("price").toString()));
        }
        if (request.containsKey("stock")) {
            book.setStock((Integer) request.get("stock"));
        }
        if (request.containsKey("pages")) {
            book.setPages((Integer) request.get("pages"));
        }
        if (request.containsKey("categoryId")) {
            book.setCategoryId(Long.valueOf(request.get("categoryId").toString()));
        }

        book.setUpdateTime(LocalDateTime.now());
        booksService.updateById(book);

        return ApiResponse.success("更新成功", book);
    }

    /**
     * 删除图书
     * DELETE /api/v1/admin/books/{bookId}
     */
    @DeleteMapping("/{bookId}")
    public ApiResponse<Void> deleteBook(@PathVariable Long bookId) {
        Books book = booksService.getById(bookId);
        if (book == null) {
            throw new BusinessException(ErrorCode.BOOK_NOT_FOUND);
        }

        booksService.removeById(bookId);
        return ApiResponse.success("删除成功", null);
    }

    /**
     * 批量导入（暂不实现，预留接口）
     * POST /api/v1/admin/books/batch
     */
    @PostMapping("/batch")
    public ApiResponse<String> batchImport() {
        return ApiResponse.success("批量导入功能待实现", "TODO");
    }
}
