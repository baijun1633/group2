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
 * 借阅记录表
 * </p>
 *
 * @author MisterDong
 * @since 2026-06-23
 */
@Getter
@Setter
@TableName("borrow_records")
public class BorrowRecords implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 记录ID
     */
    @TableId(value = "record_id", type = IdType.AUTO)
    private Long recordId;

    /**
     * 用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 书籍ID
     */
    @TableField("book_id")
    private Long bookId;

    /**
     * 借阅日期
     */
    @TableField("borrow_date")
    private LocalDateTime borrowDate;

    /**
     * 应还日期
     */
    @TableField("due_date")
    private LocalDateTime dueDate;

    /**
     * 实际归还日期
     */
    @TableField("return_date")
    private LocalDateTime returnDate;

    /**
     * 状态: 1-借阅中, 2-已归还, 3-逾期
     */
    @TableField("status")
    private Byte status;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private LocalDateTime createTime;
}
