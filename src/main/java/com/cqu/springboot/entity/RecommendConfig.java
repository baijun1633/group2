package com.cqu.springboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 推荐算法权重配置实体
 */
@Data
@TableName("recommend_config")
public class RecommendConfig {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String configKey;

    private BigDecimal configValue;

    private String description;

    private LocalDateTime updateTime;
}
