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
 * 购买记录表
 */
@Getter
@Setter
@TableName("purchase_records")
public class PurchaseRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "purchase_id", type = IdType.AUTO)
    private Long purchaseId;

    @TableField("user_id")
    private Long userId;

    @TableField("book_id")
    private Long bookId;

    @TableField("purchase_time")
    private LocalDateTime purchaseTime;

    @TableField("purchase_platform")
    private String purchasePlatform;

    @TableField("purchase_price")
    private BigDecimal purchasePrice;

    @TableField("purchase_url")
    private String purchaseUrl;
}
