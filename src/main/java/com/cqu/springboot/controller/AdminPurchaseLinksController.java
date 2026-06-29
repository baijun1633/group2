package com.cqu.springboot.controller;

import com.cqu.springboot.common.ApiResponse;
import com.cqu.springboot.common.BusinessException;
import com.cqu.springboot.common.ErrorCode;
import com.cqu.springboot.common.RequireSecondFactor;
import com.cqu.springboot.entity.Books;
import com.cqu.springboot.entity.PurchaseLinks;
import com.cqu.springboot.mapper.BooksMapper;
import com.cqu.springboot.mapper.PurchaseLinksMapper;
import com.cqu.springboot.util.CsvUtil;
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
 * 管理员购书链接管理控制器
 * 路径: /api/v1/admin/purchase-links
 */
@Tag(name = "购书链接管理", description = "批量导入购书链接")
@RestController
@RequestMapping("/api/v1/admin/purchase-links")
@RequiredArgsConstructor
public class AdminPurchaseLinksController {

    private final PurchaseLinksMapper purchaseLinksMapper;
    private final BooksMapper booksMapper;

    /**
     * 批量导入购书链接
     * POST /api/v1/admin/purchase-links/batch
     * <p>
     * 上传 CSV 文件，表头：book_id, platform, url, price
     * 每行验证 book_id 对应的图书是否存在，不存在则该行失败
     * 返回导入统计：{total, success, failed, errors:[{row, message}]}
     * </p>
     */
    @Operation(summary = "批量导入购书链接", description = "CSV格式：book_id, platform, url, price")
    @PostMapping("/batch")
    @RequireSecondFactor("购书链接批量导入")
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
                    String bookIdStr = row.get("book_id");
                    String platform = row.get("platform");
                    String url = row.get("url");
                    String priceStr = row.get("price");

                    if (bookIdStr == null || bookIdStr.isBlank()) {
                        throw new IllegalArgumentException("book_id不能为空");
                    }
                    if (platform == null || platform.isBlank()) {
                        throw new IllegalArgumentException("platform不能为空");
                    }
                    if (url == null || url.isBlank()) {
                        throw new IllegalArgumentException("url不能为空");
                    }

                    Long bookId = Long.valueOf(bookIdStr);
                    // 验证图书是否存在
                    Books book = booksMapper.selectById(bookId);
                    if (book == null) {
                        throw new IllegalArgumentException("图书ID " + bookId + " 不存在");
                    }

                    PurchaseLinks link = new PurchaseLinks();
                    link.setBookId(bookId);
                    link.setPlatform(platform);
                    link.setUrl(url);
                    if (priceStr != null && !priceStr.isBlank()) {
                        link.setPrice(new BigDecimal(priceStr));
                    }
                    link.setCreateTime(LocalDateTime.now());
                    purchaseLinksMapper.insert(link);
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
}
