package com.cqu.springboot.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 电子书章节
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EbookChapter {

    /**
     * 章节序号（从1开始）
     */
    private int index;

    /**
     * 章节标题
     */
    private String title;

    /**
     * 章节内容（HTML）
     */
    private String content;

    /**
     * 内容长度（字符数）
     */
    private int length;
}
