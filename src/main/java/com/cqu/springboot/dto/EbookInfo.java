package com.cqu.springboot.dto;

import lombok.Data;

import java.util.List;

/**
 * 电子书试读响应
 */
@Data
public class EbookInfo {

    /**
     * 图书ID
     */
    private Long bookId;

    /**
     * 书名
     */
    private String title;

    /**
     * 电子书类型：epub/pdf
     */
    private String ebookType;

    /**
     * 总章节数
     */
    private Integer totalChapters;

    /**
     * 可试读章节数（未登录3章，登录10章）
     */
    private Integer previewChapters;

    /**
     * 是否登录
     */
    private Boolean isLogin;

    /**
     * 是否截断（previewChapters < totalChapters）
     */
    private Boolean truncated;

    /**
     * 可试读的章节列表
     */
    private List<EbookChapter> chapters;

    /**
     * 提示信息（如"登录后可试读前10章"）
     */
    private String message;
}
