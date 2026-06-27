package com.cqu.springboot.service;

import com.cqu.springboot.common.PageResponse;
import com.cqu.springboot.dto.PurchaseRecordRequest;
import com.cqu.springboot.entity.PurchaseRecord;

/**
 * 购买记录服务
 */
public interface PurchaseRecordService {

    /**
     * 记录购买意向（用户点击购书链接时上报）
     *
     * @param userId  当前登录用户ID
     * @param request 购买请求（bookId/platform/price/url）
     * @return 购买记录ID
     */
    Long recordPurchase(Long userId, PurchaseRecordRequest request);

    /**
     * 分页查询用户购买记录
     *
     * @param userId   用户ID
     * @param bookId   图书ID（可选过滤）
     * @param platform 平台（可选过滤）
     * @param page     页码
     * @param size     每页大小
     * @return 分页结果
     */
    PageResponse<PurchaseRecord> getUserPurchases(Long userId, Long bookId, String platform, int page, int size);
}
