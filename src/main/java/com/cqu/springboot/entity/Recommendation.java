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
 * 推荐记录表
 */
@Getter
@Setter
@TableName("recommendations")
public class Recommendation implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "recommend_id", type = IdType.AUTO)
    private Long recommendId;

    @TableField("user_id")
    private Long userId;

    @TableField("book_id")
    private Long bookId;

    @TableField("recommend_type")
    private String recommendType;

    @TableField("score")
    private BigDecimal score;

    @TableField("reason")
    private String reason;

    @TableField("recommend_time")
    private LocalDateTime recommendTime;

    @TableField("is_clicked")
    private Boolean isClicked;

    @TableField("is_collected")
    private Boolean isCollected;
}
