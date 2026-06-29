package com.cqu.springboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * 书评表
 */
@Getter
@Setter
@TableName("reviews")
public class Reviews implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 书评ID
     */
    @TableId(value = "review_id", type = IdType.AUTO)
    private Long reviewId;

    /**
     * 书籍ID
     */
    @TableField("book_id")
    private Long bookId;

    /**
     * 用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 书评内容(纯文本)
     */
    @TableField("content")
    private String content;

    /**
     * 书评内容(Markdown格式)
     */
    @TableField("markdown")
    private String markdown;

    /**
     * 状态: 0-待审核, 1-已通过, 2-已拒绝, 3-已删除
     */
    @TableField("status")
    private Byte status;

    /**
     * 点赞数
     */
    @TableField("likes_count")
    private Integer likesCount;

    /**
     * 回复数
     */
    @TableField("replies_count")
    private Integer repliesCount;

    /**
     * 是否优质书评: 0-否, 1-是
     */
    @TableField("is_featured")
    private Byte isFeatured;

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
