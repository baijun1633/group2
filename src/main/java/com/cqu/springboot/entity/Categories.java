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
 * <p>
 * 图书分类表
 * </p>
 *
 * @author MisterDong
 * @since 2026-06-23
 */
@Getter
@Setter
@TableName("categories")
public class Categories implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 分类ID
     */
    @TableId(value = "category_id", type = IdType.AUTO)
    private Long categoryId;

    /**
     * 分类名称
     */
    @TableField("name")
    private String name;

    /**
     * 父分类ID
     */
    @TableField("parent_id")
    private Long parentId;

    /**
     * 分类描述
     */
    @TableField("description")
    private String description;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private LocalDateTime createTime;
}
