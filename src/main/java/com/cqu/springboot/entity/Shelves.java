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
 * 用户书架表
 */
@Getter
@Setter
@TableName("shelves")
public class Shelves implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 书架ID
     */
    @TableId(value = "shelf_id", type = IdType.AUTO)
    private Long shelfId;

    /**
     * 用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 书架名称
     */
    @TableField("name")
    private String name;

    /**
     * 书架描述
     */
    @TableField("description")
    private String description;

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
