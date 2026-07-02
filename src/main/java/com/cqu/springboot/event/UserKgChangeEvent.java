package com.cqu.springboot.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 用户知识图谱变更事件
 * 当用户执行加入书架、评分、书评等行为时发布此事件
 */
@Getter
public class UserKgChangeEvent extends ApplicationEvent {

    private final Long userId;
    private final Long bookId;
    private final Action action;

    public enum Action {
        /** 用户添加书籍到书架/评分/书评 → 增量添加实体 */
        ADD,
        /** 用户从书架移除书籍 → 移除相关实体 */
        REMOVE
    }

    public UserKgChangeEvent(Object source, Long userId, Long bookId, Action action) {
        super(source);
        this.userId = userId;
        this.bookId = bookId;
        this.action = action;
    }
}
