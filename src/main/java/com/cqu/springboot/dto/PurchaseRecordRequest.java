package com.cqu.springboot.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 购买记录上报请求 DTO
 */
@Data
public class PurchaseRecordRequest {

    /** 图书ID */
    private Long bookId;

    /** 购买平台：京东/当当/淘宝等 */
    private String platform;

    /** 实际支付价格（可选，前端不知道实时价时留空） */
    private BigDecimal price;

    /** 购买链接（跳转URL） */
    private String url;
}
