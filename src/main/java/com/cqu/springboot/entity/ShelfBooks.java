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
 * 书架图书关联表
 */
@Getter
@Setter
@TableName("shelf_books")
public class ShelfBooks implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 书架ID
     */
    @TableField("shelf_id")
    private Long shelfId;

    /**
     * 图书ID
     */
    @TableField("book_id")
    private Long bookId;

    /**
     * 阅读状态：0-未读, 1-在读, 2-已读
     */
    @TableField("reading_status")
    private Integer readingStatus;

    /**
     * 添加时间
     */
    @TableField("add_time")
    private LocalDateTime addTime;
}
