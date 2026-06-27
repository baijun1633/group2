package com.cqu.springboot.dto;

import lombok.Data;

import java.util.List;

/**
 * 推荐结果项 DTO
 */
@Data
public class RecommendItem {

    /** 图书ID */
    private Long bookId;

    /** 推荐记录ID（落库后回填，前端点击回调时传回） */
    private Long recommendId;

    /** 书名 */
    private String title;

    /** 作者 */
    private String author;

    /** 封面图片URL */
    private String coverImage;

    /** 平均评分 */
    private Double avgRating;

    /** 推荐分数（0-1，越高越推荐） */
    private Double score;

    /** 推荐理由（自然语言） */
    private String reason;

    /** 推理路径（节点ID列表，用于推荐解释） */
    private List<String> reasonPath;

    /** 推荐来源：KG / ITEMCF / HOT / NEW / HYBRID */
    private String source;

    public RecommendItem() {}

    public RecommendItem(Long bookId, String title, String author, String coverImage,
                         Double avgRating, Double score, String reason,
                         List<String> reasonPath, String source) {
        this.bookId = bookId;
        this.title = title;
        this.author = author;
        this.coverImage = coverImage;
        this.avgRating = avgRating;
        this.score = score;
        this.reason = reason;
        this.reasonPath = reasonPath;
        this.source = source;
    }
}
