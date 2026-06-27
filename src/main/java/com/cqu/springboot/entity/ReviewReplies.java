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
 * 书评回复表
 */
@Getter
@Setter
@TableName("review_replies")
public class ReviewReplies implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 回复ID
     */
    @TableId(value = "reply_id", type = IdType.AUTO)
    private Long replyId;

    /**
     * 书评ID
     */
    @TableField("review_id")
    private Long reviewId;

    /**
     * 用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 回复内容
     */
    @TableField("content")
    private String content;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private LocalDateTime createTime;
}
