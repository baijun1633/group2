package com.cqu.springboot.event;

import com.cqu.springboot.service.KnowledgeGraphService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 用户知识图谱变更事件监听器
 * 监听用户行为变化，自动更新用户的知识图谱
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserKgEventListener {

    private final KnowledgeGraphService knowledgeGraphService;

    /**
     * 处理知识图谱变更事件（异步执行）
     */
    @Async
    @EventListener
    public void handleKgChange(UserKgChangeEvent event) {
        try {
            Long userId = event.getUserId();
            Long bookId = event.getBookId();
            UserKgChangeEvent.Action action = event.getAction();

            log.info("处理知识图谱变更事件: userId={}, bookId={}, action={}", userId, bookId, action);

            switch (action) {
                case ADD -> knowledgeGraphService.addBookToUserGraph(userId, bookId);
                case REMOVE -> knowledgeGraphService.removeBookFromUserGraph(userId, bookId);
            }
        } catch (Exception e) {
            log.error("处理知识图谱变更事件失败: userId={}, bookId={}, action={}",
                    event.getUserId(), event.getBookId(), event.getAction(), e);
        }
    }
}