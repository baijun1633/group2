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
 * 图书评分表
 */
@Getter
@Setter
@TableName("book_ratings")
public class BookRatings implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 评分ID
     */
    @TableId(value = "rating_id", type = IdType.AUTO)
    private Long ratingId;

    /**
     * 用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 书籍ID
     */
    @TableField("book_id")
    private Long bookId;

    /**
     * 评分(1-5分)
     */
    @TableField("score")
    private Byte score;

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
