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
 * 用户行为记录表
 */
@Getter
@Setter
@TableName("user_behaviors")
public class UserBehavior implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "behavior_id", type = IdType.AUTO)
    private Long behaviorId;

    @TableField("user_id")
    private Long userId;

    @TableField("book_id")
    private Long bookId;

    @TableField("behavior_type")
    private String behaviorType;

    @TableField("behavior_time")
    private LocalDateTime behaviorTime;

    @TableField("behavior_value")
    private String behaviorValue;

    @TableField("duration")
    private Integer duration;
}
