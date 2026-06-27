package com.cqu.springboot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cqu.springboot.common.PageResponse;
import com.cqu.springboot.entity.Books;
import com.cqu.springboot.entity.UserBehavior;
import com.cqu.springboot.mapper.BooksMapper;
import com.cqu.springboot.mapper.UserBehaviorMapper;
import com.cqu.springboot.service.UserBehaviorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 用户行为采集服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserBehaviorServiceImpl implements UserBehaviorService {

    private final UserBehaviorMapper userBehaviorMapper;
    private final BooksMapper booksMapper;

    @Override
    public void recordBehavior(Long userId, Long bookId, String behaviorType, String behaviorValue, Integer duration) {
        // 记录行为失败不应影响主业务，整体 try-catch 静默忽略
        try {
            // 未登录不记录
            if (userId == null) {
                return;
            }

            // view 行为自带5分钟去重：该用户对该书最近5分钟内已有 view 记录则跳过
            if ("view".equals(behaviorType)) {
                QueryWrapper<UserBehavior> dedupWrapper = new QueryWrapper<>();
                dedupWrapper.eq("user_id", userId)
                        .eq("book_id", bookId)
                        .eq("behavior_type", "view")
                        .gt("behavior_time", LocalDateTime.now().minusMinutes(5));
                Long count = userBehaviorMapper.selectCount(dedupWrapper);
                if (count != null && count > 0) {
                    return;
                }
            }

            UserBehavior behavior = new UserBehavior();
            behavior.setUserId(userId);
            behavior.setBookId(bookId);
            behavior.setBehaviorType(behaviorType);
            behavior.setBehaviorTime(LocalDateTime.now());
            behavior.setBehaviorValue(behaviorValue);
            behavior.setDuration(duration == null ? 0 : duration);
            userBehaviorMapper.insert(behavior);

            // 方案A：行为联动计数 —— view → books.view_count+1，collect → books.collect_count+1
            if (bookId != null) {
                String countField = null;
                if ("view".equals(behaviorType)) {
                    countField = "view_count";
                } else if ("collect".equals(behaviorType)) {
                    countField = "collect_count";
                }
                if (countField != null) {
                    incrementBookCount(bookId, countField);
                }
            }
        } catch (Exception e) {
            log.warn("记录用户行为失败 userId={}, bookId={}, type={}, err={}",
                    userId, bookId, behaviorType, e.getMessage());
        }
    }

    /**
     * 原子自增 books 表的计数字段（SET field = field + 1）
     */
    private void incrementBookCount(Long bookId, String field) {
        UpdateWrapper<Books> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("book_id", bookId).setSql(field + " = " + field + " + 1");
        booksMapper.update(null, updateWrapper);
    }

    @Override
    public void decrementCollectCount(Long bookId) {
        // 取消收藏联动减计数失败不应影响主业务，静默忽略
        try {
            if (bookId == null) {
                return;
            }
            // GREATEST 保证不会变成负数
            UpdateWrapper<Books> updateWrapper = new UpdateWrapper<>();
            updateWrapper.eq("book_id", bookId)
                    .setSql("collect_count = GREATEST(0, collect_count - 1)");
            booksMapper.update(null, updateWrapper);
        } catch (Exception e) {
            log.warn("减少收藏计数失败 bookId={}, err={}", bookId, e.getMessage());
        }
    }

    @Override
    public PageResponse<UserBehavior> listUserBehaviors(Long userId, String behaviorType, int page, int size) {
        Page<UserBehavior> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<UserBehavior> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserBehavior::getUserId, userId);
        if (behaviorType != null && !behaviorType.isEmpty()) {
            wrapper.eq(UserBehavior::getBehaviorType, behaviorType);
        }
        wrapper.orderByDesc(UserBehavior::getBehaviorTime);

        Page<UserBehavior> result = userBehaviorMapper.selectPage(pageParam, wrapper);
        return new PageResponse<>(result.getRecords(), result.getTotal(), page, size);
    }
}
