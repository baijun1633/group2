package com.cqu.springboot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cqu.springboot.common.BusinessException;
import com.cqu.springboot.common.ErrorCode;
import com.cqu.springboot.common.PageResponse;
import com.cqu.springboot.dto.ReadingProgressRequest;
import com.cqu.springboot.entity.ReadingProgress;
import com.cqu.springboot.entity.UserBehavior;
import com.cqu.springboot.mapper.ReadingProgressMapper;
import com.cqu.springboot.mapper.UserBehaviorMapper;
import com.cqu.springboot.service.ReadingProgressService;
import com.cqu.springboot.service.UserBehaviorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 阅读进度服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReadingProgressServiceImpl implements ReadingProgressService {

    private final ReadingProgressMapper readingProgressMapper;
    private final UserBehaviorMapper userBehaviorMapper;
    private final UserBehaviorService userBehaviorService;

    @Override
    public Long recordReading(Long userId, ReadingProgressRequest request) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "请先登录");
        }
        if (request == null || request.getBookId() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "图书ID不能为空");
        }

        int addDuration = request.getReadDuration() != null ? request.getReadDuration() : 0;
        BigDecimal percentage = computePercentage(request);

        // upsert by userId + bookId
        LambdaQueryWrapper<ReadingProgress> qw = new LambdaQueryWrapper<>();
        qw.eq(ReadingProgress::getUserId, userId).eq(ReadingProgress::getBookId, request.getBookId());
        ReadingProgress existing = readingProgressMapper.selectOne(qw);

        Long id;
        if (existing != null) {
            existing.setReadDuration(existing.getReadDuration() + addDuration);
            if (request.getCurrentPage() != null) {
                existing.setCurrentPage(request.getCurrentPage());
            }
            if (request.getChapter() != null) {
                existing.setChapter(request.getChapter());
            }
            if (request.getTotalPages() != null) {
                existing.setTotalPages(request.getTotalPages());
            }
            if (percentage != null) {
                existing.setPercentage(percentage);
            }
            if (request.getDevice() != null) {
                existing.setDevice(request.getDevice());
            }
            existing.setUpdateTime(LocalDateTime.now());
            readingProgressMapper.updateById(existing);
            id = existing.getId();
        } else {
            ReadingProgress rp = new ReadingProgress();
            rp.setUserId(userId);
            rp.setBookId(request.getBookId());
            rp.setCurrentPage(request.getCurrentPage() != null ? request.getCurrentPage() : 0);
            rp.setChapter(request.getChapter());
            rp.setTotalPages(request.getTotalPages() != null ? request.getTotalPages() : 0);
            rp.setPercentage(percentage != null ? percentage : BigDecimal.ZERO);
            rp.setReadDuration(addDuration);
            rp.setDevice(request.getDevice());
            rp.setUpdateTime(LocalDateTime.now());
            readingProgressMapper.insert(rp);
            id = rp.getId();
        }

        // 记录单次阅读行为（用于周/月时长统计）
        if (addDuration > 0) {
            try {
                userBehaviorService.recordBehavior(userId, request.getBookId(), "read", null, addDuration);
            } catch (Exception e) {
                log.warn("记录阅读行为失败 userId={}, bookId={}, err={}", userId, request.getBookId(), e.getMessage());
            }
        }
        log.info("记录阅读 userId={}, bookId={}, addDuration={}, progressId={}",
                userId, request.getBookId(), addDuration, id);
        return id;
    }

    @Override
    public ReadingProgress getReadingProgress(Long userId, Long bookId) {
        LambdaQueryWrapper<ReadingProgress> qw = new LambdaQueryWrapper<>();
        qw.eq(ReadingProgress::getUserId, userId).eq(ReadingProgress::getBookId, bookId);
        return readingProgressMapper.selectOne(qw);
    }

    @Override
    public PageResponse<ReadingProgress> getReadingHistory(Long userId, String status, int page, int size) {
        Page<ReadingProgress> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<ReadingProgress> qw = new LambdaQueryWrapper<>();
        qw.eq(ReadingProgress::getUserId, userId);
        if ("finished".equalsIgnoreCase(status)) {
            qw.ge(ReadingProgress::getPercentage, new BigDecimal("100"));
        } else if ("reading".equalsIgnoreCase(status)) {
            qw.lt(ReadingProgress::getPercentage, new BigDecimal("100"));
        }
        qw.orderByDesc(ReadingProgress::getUpdateTime);
        Page<ReadingProgress> result = readingProgressMapper.selectPage(pageParam, qw);
        return new PageResponse<>(result.getRecords(), result.getTotal(), page, size);
    }

    @Override
    public Map<String, Object> getReadingStats(Long userId) {
        Map<String, Object> stats = new HashMap<>();
        // 周/月时长：基于 user_behaviors(type=read) 的 SUM(duration)
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime weekAgo = now.minusDays(7);
        LocalDateTime monthAgo = now.minusDays(30);
        stats.put("weekDuration", sumReadDuration(userId, weekAgo));
        stats.put("monthDuration", sumReadDuration(userId, monthAgo));

        // 已读完 / 在读 数量：基于 reading_progress.percentage
        Long finished = readingProgressMapper.selectCount(new LambdaQueryWrapper<ReadingProgress>()
                .eq(ReadingProgress::getUserId, userId)
                .ge(ReadingProgress::getPercentage, new BigDecimal("100")));
        Long reading = readingProgressMapper.selectCount(new LambdaQueryWrapper<ReadingProgress>()
                .eq(ReadingProgress::getUserId, userId)
                .lt(ReadingProgress::getPercentage, new BigDecimal("100")));
        stats.put("finishedCount", finished != null ? finished : 0);
        stats.put("readingCount", reading != null ? reading : 0);
        return stats;
    }

    /** 计算 percentage = currentPage / totalPages * 100，保留2位小数 */
    private BigDecimal computePercentage(ReadingProgressRequest request) {
        if (request.getTotalPages() != null && request.getTotalPages() > 0 && request.getCurrentPage() != null) {
            double pct = request.getCurrentPage() * 100.0 / request.getTotalPages();
            if (pct > 100) {
                pct = 100;
            }
            return BigDecimal.valueOf(pct).setScale(2, RoundingMode.HALF_UP);
        }
        return null;
    }

    /** SUM(duration) from user_behaviors where user_id=? and behavior_type='read' and behavior_time >= since */
    private long sumReadDuration(Long userId, LocalDateTime since) {
        QueryWrapper<UserBehavior> qw = new QueryWrapper<>();
        qw.select("IFNULL(SUM(duration),0) AS total")
                .eq("user_id", userId)
                .eq("behavior_type", "read")
                .ge("behavior_time", since);
        List<Map<String, Object>> maps = userBehaviorMapper.selectMaps(qw);
        if (maps != null && !maps.isEmpty() && maps.get(0).get("total") != null) {
            return ((Number) maps.get(0).get("total")).longValue();
        }
        return 0L;
    }
}
