package com.cqu.springboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * 书籍表
 */
@Getter
@Setter
@TableName("books")
public class Books implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 书籍ID
     */
    @TableId(value = "book_id", type = IdType.AUTO)
    private Long bookId;

    /**
     * 书名
     */
    @TableField("title")
    private String title;

    /**
     * 作者
     */
    @TableField("author")
    private String author;

    /**
     * ISBN号
     */
    @TableField("isbn")
    private String isbn;

    /**
     * 出版社
     */
    @TableField("publisher")
    private String publisher;

    /**
     * 出版日期
     */
    @TableField("publish_date")
    private LocalDate publishDate;

    /**
     * 分类ID
     */
    @TableField("category_id")
    private Long categoryId;

    /**
     * 价格
     */
    @TableField("price")
    private BigDecimal price;

    /**
     * 库存数量
     */
    @TableField("stock")
    private Integer stock;

    /**
     * 页数
     */
    @TableField("pages")
    private Integer pages;

    /**
     * 标签(JSON格式，如 ["科幻","刘慈欣","三体"])
     */
    @TableField("tags")
    private String tags;

    /**
     * 平均评分(1-5分)
     */
    @TableField("avg_rating")
    private BigDecimal avgRating;

    /**
     * 评分人数
     */
    @TableField("rating_count")
    private Integer ratingCount;

    /**
     * 浏览次数
     */
    @TableField("view_count")
    private Integer viewCount;

    /**
     * 收藏次数
     */
    @TableField("collect_count")
    private Integer collectCount;

    /**
     * 系列信息(如 "三体三部曲")
     */
    @TableField("series_info")
    private String seriesInfo;

    /**
     * 书籍简介
     */
    @TableField("description")
    private String description;

    /**
     * 试读内容（前几章文本）
     */
    @TableField("preview_content")
    private String previewContent;

    /**
     * 电子书文件URL（如 /uploads/ebooks/123.epub）
     */
    @TableField("ebook_url")
    private String ebookUrl;

    /**
     * 电子书类型：epub/pdf
     */
    @TableField("ebook_type")
    private String ebookType;

    /**
     * 电子书文件大小（字节）
     */
    @TableField("ebook_size")
    private Long ebookSize;

    /**
     * 封面图片URL
     */
    @JsonProperty("coverUrl")
    @TableField("cover_image")
    private String coverImage;

    /**
     * 状态: 0-下架, 1-上架
     */
    @TableField("status")
    private Byte status;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField("update_time")
    private LocalDateTime updateTime;
}
