package com.cqu.springboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 图书相似度矩阵（离线计算）
 */
@Getter
@Setter
@TableName("book_similarity")
public class BookSimilarity implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("book_id_a")
    private Long bookIdA;

    @TableField("book_id_b")
    private Long bookIdB;

    @TableField("similarity")
    private BigDecimal similarity;

    @TableField("co_count")
    private Integer coCount;

    @TableField("compute_time")
    private LocalDateTime computeTime;
}
