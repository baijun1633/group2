package com.cqu.springboot.dto;

import lombok.Data;

/**
 * 阅读进度上报请求 DTO
 */
@Data
public class ReadingProgressRequest {

    /** 图书ID */
    private Long bookId;

    /** 当前页码 */
    private Integer currentPage;

    /** 当前章节 */
    private String chapter;

    /** 总页数 */
    private Integer totalPages;

    /** 本次会话阅读时长（秒），后端累加到 read_duration */
    private Integer readDuration;

    /** 设备信息：PC/Android/iOS 等 */
    private String device;
}
