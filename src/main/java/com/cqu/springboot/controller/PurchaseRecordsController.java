package com.cqu.springboot.controller;

import com.cqu.springboot.common.ApiResponse;
import com.cqu.springboot.common.PageResponse;
import com.cqu.springboot.dto.PurchaseRecordRequest;
import com.cqu.springboot.entity.PurchaseRecord;
import com.cqu.springboot.security.SecurityUtils;
import com.cqu.springboot.service.PurchaseRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 购买记录控制器
 * 路径: /api/v1/purchases
 */
@Tag(name = "购买记录", description = "用户购书意向记录与查询")
@RestController
@RequestMapping("/api/v1/purchases")
@RequiredArgsConstructor
public class PurchaseRecordsController {

    private final PurchaseRecordService purchaseRecordService;

    @Operation(summary = "记录购买意向", description = "用户点击购书链接跳转前上报，记录平台/价格/链接")
    @PostMapping
    public ApiResponse<Map<String, Object>> recordPurchase(@RequestBody PurchaseRecordRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        Long purchaseId = purchaseRecordService.recordPurchase(userId, request);
        return ApiResponse.success("记录成功", Map.of("purchaseId", purchaseId));
    }

    @Operation(summary = "我的购买记录", description = "分页查询当前用户的购买记录，支持按图书/平台过滤")
    @GetMapping
    public ApiResponse<PageResponse<PurchaseRecord>> listMyPurchases(
            @Parameter(description = "图书ID") @RequestParam(required = false) Long bookId,
            @Parameter(description = "购买平台") @RequestParam(required = false) String platform,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = SecurityUtils.getCurrentUserId();
        PageResponse<PurchaseRecord> response = purchaseRecordService.getUserPurchases(userId, bookId, platform, page, size);
        return ApiResponse.success(response);
    }
}
