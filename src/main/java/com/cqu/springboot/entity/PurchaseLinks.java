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
 * 购书链接表
 */
@Getter
@Setter
@TableName("purchase_links")
public class PurchaseLinks implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 链接ID
     */
    @TableId(value = "link_id", type = IdType.AUTO)
    private Long linkId;

    /**
     * 书籍ID
     */
    @TableField("book_id")
    private Long bookId;

    /**
     * 平台名称
     */
    @TableField("platform")
    private String platform;

    /**
     * 购买链接
     */
    @TableField("url")
    private String url;

    /**
     * 平台价格
     */
    @TableField("price")
    private BigDecimal price;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private LocalDateTime createTime;
}
