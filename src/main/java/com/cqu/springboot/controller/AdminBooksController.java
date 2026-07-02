package com.cqu.springboot.controller;

import com.cqu.springboot.common.ApiResponse;
import com.cqu.springboot.common.BusinessException;
import com.cqu.springboot.common.ErrorCode;
import com.cqu.springboot.common.RequireSecondFactor;
import com.cqu.springboot.entity.Books;
import com.cqu.springboot.service.BooksService;
import com.cqu.springboot.service.CoverImageService;
import com.cqu.springboot.service.EbookService;
import com.cqu.springboot.util.CsvUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 管理员图书管理控制器
 * 路径: /api/v1/admin/books
 */
@Tag(name = "管理员-图书管理", description = "图书 CRUD、批量导入、试读内容、电子书文件上传")
@RestController
@RequestMapping("/api/v1/admin/books")
@RequiredArgsConstructor
public class AdminBooksController {

    private final BooksService booksService;
    private final ObjectMapper objectMapper;
    private final EbookService ebookService;
    private final CoverImageService coverImageService;

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
            book.setPrice(new BigDecimal(request.get("price").toString()));
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
            book.setPrice(new BigDecimal(request.get("price").toString()));
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
        if (request.containsKey("status")) {
            book.setStatus(((Number) request.get("status")).byteValue());
        }

        book.setUpdateTime(LocalDateTime.now());
        booksService.updateById(book);

        return ApiResponse.success("更新成功", book);
    }

    /**
     * 删除图书
     * DELETE /api/v1/admin/books/{bookId}
     */
    @RequireSecondFactor("删除图书")
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
     * 批量导入图书
     * POST /api/v1/admin/books/batch
     * <p>
     * 上传 CSV 文件，表头：title, author, isbn, publisher, price, stock, pages, categoryId, description
     * 返回导入统计：{total, success, failed, errors:[{row, message}]}
     * </p>
     */
    @PostMapping("/batch")
    @RequireSecondFactor("图书批量导入")
    public ApiResponse<Map<String, Object>> batchImport(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "文件不能为空");
        }
        try {
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);
            var rows = CsvUtil.parse(content);
            int success = 0;
            int failed = 0;
            var errors = new ArrayList<Map<String, Object>>();

            for (int i = 0; i < rows.size(); i++) {
                Map<String, String> row = rows.get(i);
                try {
                    Books book = new Books();
                    book.setTitle(row.get("title"));
                    book.setAuthor(row.get("author"));
                    book.setIsbn(row.get("isbn"));
                    book.setPublisher(row.get("publisher"));
                    book.setDescription(row.get("description"));
                    if (row.get("price") != null && !row.get("price").isBlank()) {
                        book.setPrice(new BigDecimal(row.get("price")));
                    }
                    if (row.get("stock") != null && !row.get("stock").isBlank()) {
                        book.setStock(Integer.valueOf(row.get("stock")));
                    }
                    if (row.get("pages") != null && !row.get("pages").isBlank()) {
                        book.setPages(Integer.valueOf(row.get("pages")));
                    }
                    if (row.get("categoryId") != null && !row.get("categoryId").isBlank()) {
                        book.setCategoryId(Long.valueOf(row.get("categoryId")));
                    }
                    if (book.getTitle() == null || book.getTitle().isBlank()) {
                        throw new IllegalArgumentException("书名不能为空");
                    }
                    book.setStatus((byte) 1);
                    book.setCreateTime(LocalDateTime.now());
                    book.setUpdateTime(LocalDateTime.now());
                    booksService.save(book);
                    success++;
                } catch (Exception e) {
                    failed++;
                    Map<String, Object> err = new LinkedHashMap<>();
                    err.put("row", i + 2); // +2: 表头1行 + 索引从0开始
                    err.put("message", e.getMessage());
                    errors.add(err);
                }
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("total", rows.size());
            result.put("success", success);
            result.put("failed", failed);
            result.put("errors", errors);
            return ApiResponse.success("批量导入完成", result);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.CSV_PARSE_ERROR, e.getMessage());
        }
    }

    /**
     * 更新试读内容
     * PUT /api/v1/admin/books/{bookId}/preview
     * <p>请求体：{"previewContent": "试读文本..."}</p>
     */
    @PutMapping("/{bookId}/preview")
    public ApiResponse<Void> updatePreview(
            @PathVariable Long bookId,
            @RequestBody Map<String, String> request) {
        Books book = booksService.getById(bookId);
        if (book == null) {
            throw new BusinessException(ErrorCode.BOOK_NOT_FOUND);
        }
        book.setPreviewContent(request.get("previewContent"));
        book.setUpdateTime(LocalDateTime.now());
        booksService.updateById(book);
        return ApiResponse.success("试读内容已更新", null);
    }

    /**
     * 上传电子书文件（EPUB/TXT/PDF）
     * POST /api/v1/admin/books/{bookId}/ebook
     * <p>multipart/form-data，参数名 file，支持 .epub/.txt/.pdf，最大 100MB</p>
     * <p>文件存到 d:/code/library_sys/uploads/ebooks/{bookId}.{ext}，并更新 books 表</p>
     * <p>推荐使用 EPUB 格式（体积小、阅读体验好），TXT 格式（最轻量、零依赖）作为备选</p>
     */
    @Operation(summary = "上传电子书文件", description = "上传 EPUB/TXT/PDF 文件，最大 100MB。文件名固定为 {bookId}.{ext}，重复上传会覆盖旧文件。推荐使用 EPUB 格式")
    @PostMapping("/{bookId}/ebook")
    public ApiResponse<Map<String, Object>> uploadEbook(
            @PathVariable Long bookId,
            @RequestParam("file") MultipartFile file) {
        String url = ebookService.saveEbookFile(bookId, file);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("bookId", bookId);
        result.put("ebookUrl", url);
        result.put("ebookType", url.substring(url.lastIndexOf('.') + 1));
        result.put("size", file.getSize());
        return ApiResponse.success("电子书上传成功", result);
    }

    /**
     * 上传图书封面图片
     * POST /api/v1/admin/books/{bookId}/cover
     * <p>multipart/form-data，参数名 file，支持 jpg/jpeg/png/gif/webp，最大 5MB</p>
     * <p>文件存到 d:/code/library_sys/uploads/covers/{bookId}.{ext}，并更新 books 表 cover_image 字段</p>
     */
    @Operation(summary = "上传图书封面图片", description = "上传封面图片，支持 jpg/jpeg/png/gif/webp，最大 5MB。文件名固定为 {bookId}.{ext}，重复上传会覆盖旧文件")
    @PostMapping("/{bookId}/cover")
    public ApiResponse<Map<String, Object>> uploadCover(
            @PathVariable Long bookId,
            @RequestParam("file") MultipartFile file) {
        String url = coverImageService.uploadCover(bookId, file);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("bookId", bookId);
        result.put("coverImage", url);
        result.put("size", file.getSize());
        return ApiResponse.success("封面上传成功", result);
    }
}
