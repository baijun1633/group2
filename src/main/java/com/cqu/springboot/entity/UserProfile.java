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
 * 用户画像表
 */
@Getter
@Setter
@TableName("user_profiles")
public class UserProfile implements Serializable {

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
     * 标签权重向量JSON，如{"科幻":0.8,"文学":0.6}
     */
    @TableField("tag_vector")
    private String tagVector;

    /**
     * 偏好作者列表JSON
     */
    @TableField("preferred_authors")
    private String preferredAuthors;

    /**
     * 偏好分类列表JSON
     */
    @TableField("preferred_categories")
    private String preferredCategories;

    /**
     * 最后更新时间
     */
    @TableField("last_updated")
    private LocalDateTime lastUpdated;
}
