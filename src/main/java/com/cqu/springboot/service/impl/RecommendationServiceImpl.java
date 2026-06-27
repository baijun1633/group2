package com.cqu.springboot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cqu.springboot.common.PageResponse;
import com.cqu.springboot.dto.RecommendItem;
import com.cqu.springboot.entity.Recommendation;
import com.cqu.springboot.mapper.RecommendationMapper;
import com.cqu.springboot.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 推荐记录持久化服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationServiceImpl implements RecommendationService {

    private final RecommendationMapper recommendationMapper;

    @Override
    public void generateRecommendation(Long userId, List<RecommendItem> items, String sourceType) {
        if (userId == null || items == null || items.isEmpty()) {
            return;
        }
        for (RecommendItem item : items) {
            try {
                if (item.getBookId() == null) {
                    continue;
                }
                // 查是否已存在（UNIQUE KEY uniq_user_book）
                QueryWrapper<Recommendation> qw = new QueryWrapper<>();
                qw.eq("user_id", userId).eq("book_id", item.getBookId());
                Recommendation existing = recommendationMapper.selectOne(qw);

                // 推荐 source 大写转小写（KG → kg），符合概要设计文档 recommendType: kg/cf/hybrid
                String recType = sourceType != null ? sourceType.toLowerCase() : "hybrid";
                // item 自带的 source 更精确（home 推荐里每个 item 可能来自不同算法）
                if (item.getSource() != null && !item.getSource().isBlank()) {
                    recType = item.getSource().toLowerCase();
                }

                BigDecimal score = item.getScore() != null
                        ? BigDecimal.valueOf(item.getScore()).setScale(4, java.math.RoundingMode.HALF_UP)
                        : BigDecimal.ZERO;
                String reason = item.getReason() != null && item.getReason().length() > 500
                        ? item.getReason().substring(0, 500) : item.getReason();

                if (existing == null) {
                    // 新记录
                    Recommendation rec = new Recommendation();
                    rec.setUserId(userId);
                    rec.setBookId(item.getBookId());
                    rec.setRecommendType(recType);
                    rec.setScore(score);
                    rec.setReason(reason);
                    rec.setRecommendTime(LocalDateTime.now());
                    rec.setIsClicked(false);
                    rec.setIsCollected(false);
                    recommendationMapper.insert(rec);
                    item.setRecommendId(rec.getRecommendId()); // 回填自增主键
                } else {
                    // 已存在：更新 score/reason/time/type，保留 is_clicked/is_collected
                    Recommendation update = new Recommendation();
                    update.setRecommendId(existing.getRecommendId());
                    update.setRecommendType(recType);
                    update.setScore(score);
                    update.setReason(reason);
                    update.setRecommendTime(LocalDateTime.now());
                    recommendationMapper.updateById(update);
                    item.setRecommendId(existing.getRecommendId()); // 回填已有主键
                }
            } catch (Exception e) {
                log.warn("落库推荐记录失败 userId={}, bookId={}, err={}",
                        userId, item.getBookId(), e.getMessage());
            }
        }
        log.debug("推荐落库完成 userId={}, source={}, count={}", userId, sourceType, items.size());
    }

    @Override
    public boolean recordClick(Long recommendId, Long userId) {
        if (recommendId == null || userId == null) {
            return false;
        }
        // 加 user_id 条件防越权（只能标记自己的推荐记录）
        UpdateWrapper<Recommendation> uw = new UpdateWrapper<>();
        uw.eq("recommend_id", recommendId)
                .eq("user_id", userId)
                .set("is_clicked", true);
        return recommendationMapper.update(null, uw) > 0;
    }

    @Override
    public void recordCollect(Long userId, Long bookId) {
        if (userId == null || bookId == null) {
            return;
        }
        // 按 userId + bookId 标记 is_collected=1（取消收藏时不改，保留历史状态）
        UpdateWrapper<Recommendation> uw = new UpdateWrapper<>();
        uw.eq("user_id", userId)
                .eq("book_id", bookId)
                .set("is_collected", true);
        recommendationMapper.update(null, uw);
    }

    @Override
    public PageResponse<Recommendation> getRecommendHistory(Long userId, Boolean clicked, Boolean collected, int page, int size) {
        Page<Recommendation> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Recommendation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Recommendation::getUserId, userId);
        if (Boolean.TRUE.equals(clicked)) {
            wrapper.eq(Recommendation::getIsClicked, true);
        }
        if (Boolean.TRUE.equals(collected)) {
            wrapper.eq(Recommendation::getIsCollected, true);
        }
        wrapper.orderByDesc(Recommendation::getRecommendTime);

        Page<Recommendation> result = recommendationMapper.selectPage(pageParam, wrapper);
        return new PageResponse<>(result.getRecords(), result.getTotal(), page, size);
    }
}
