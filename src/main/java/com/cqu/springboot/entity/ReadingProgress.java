package com.cqu.springboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * 阅读进度表
 */
@Getter
@Setter
@TableName("reading_progress")
public class ReadingProgress implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 图书ID
     */
    @TableField("book_id")
    private Long bookId;

    /**
     * 当前页码
     */
    @TableField("current_page")
    private Integer currentPage;

    /**
     * 当前章节
     */
    @TableField("chapter")
    private String chapter;

    /**
     * 总页数
     */
    @TableField("total_pages")
    private Integer totalPages;

    /**
     * 阅读百分比
     */
    @TableField("percentage")
    private BigDecimal percentage;

    /**
     * 总阅读时长(秒)
     */
    @TableField("read_duration")
    private Integer readDuration;

    /**
     * 设备信息
     */
    @TableField("device")
    private String device;

    /**
     * 更新时间
     */
    @TableField("update_time")
    private LocalDateTime updateTime;
}
