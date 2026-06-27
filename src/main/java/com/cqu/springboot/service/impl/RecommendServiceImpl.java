package com.cqu.springboot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cqu.springboot.common.BusinessException;
import com.cqu.springboot.common.ErrorCode;
import com.cqu.springboot.dto.RecommendItem;
import com.cqu.springboot.entity.BookRatings;
import com.cqu.springboot.entity.Books;
import com.cqu.springboot.entity.RecommendConfig;
import com.cqu.springboot.mapper.BookRatingsMapper;
import com.cqu.springboot.mapper.BooksMapper;
import com.cqu.springboot.mapper.RecommendConfigMapper;
import com.cqu.springboot.service.CollaborativeFilteringService;
import com.cqu.springboot.service.KgRecommendService;
import com.cqu.springboot.service.RecommendService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 混合推荐服务实现
 * <p>
 * 策略组合：
 * - 登录用户：KG(40%) + ItemCF(30%) + 热门(15%) + 新书(15%)
 * - 未登录用户：热门(50%) + 新书(50%) 兜底
 * - 权重可通过 recommend_config 表动态配置
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendServiceImpl implements RecommendService {

    private final CollaborativeFilteringService cfService;
    private final KgRecommendService kgService;
    private final BooksMapper booksMapper;
    private final RecommendConfigMapper configMapper;
    private final BookRatingsMapper bookRatingsMapper;

    @Override
    public List<RecommendItem> getHomeRecommend(Long userId, int limit) {
        if (userId == null) {
            // 未登录：热门 + 新书兜底
            List<RecommendItem> hot = getHotBooks(90, limit / 2 + 1);
            List<RecommendItem> newBooks = getNewBooks(6, limit / 2 + 1);
            List<RecommendItem> merged = mergeAndDedup(hot, newBooks, limit);
            merged.forEach(i -> i.setSource("FALLBACK"));
            return merged;
        }

        // 登录用户：混合策略
        Map<String, Double> weights = getWeights();
        int kgLimit = (int) Math.ceil(limit * weights.get("kg_weight")) + 2;
        int cfLimit = (int) Math.ceil(limit * weights.get("itemcf_weight")) + 2;
        int hotLimit = (int) Math.ceil(limit * weights.get("hot_weight")) + 1;
        int newLimit = (int) Math.ceil(limit * weights.get("new_weight")) + 1;

        List<RecommendItem> kgItems = kgService.recommendByKg(userId, kgLimit);
        List<RecommendItem> cfItems = cfService.recommendByItemCF(userId, cfLimit);
        List<RecommendItem> hotItems = getHotBooks(90, hotLimit);
        List<RecommendItem> newItems = getNewBooks(6, newLimit);

        // 加权合并
        return weightedMerge(kgItems, cfItems, hotItems, newItems, weights, limit, userId);
    }

    @Override
    public List<RecommendItem> getHotBooks(int days, int limit) {
        // days 参数保留兼容性；view_count 为累计值，热门排序以浏览量为主，评分作为 fallback
        QueryWrapper<Books> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1)
            .orderByDesc("view_count", "rating_count", "avg_rating")
            .last("LIMIT " + limit);

        List<Books> books = booksMapper.selectList(wrapper);
        return books.stream()
            .map(b -> new RecommendItem(
                b.getBookId(), b.getTitle(), b.getAuthor(), b.getCoverImage(),
                b.getAvgRating() != null ? b.getAvgRating().doubleValue() : null,
                computeHotScore(b),
                String.format("热门图书（%d次浏览，%d人评价，评分%.1f）",
                    b.getViewCount() != null ? b.getViewCount() : 0,
                    b.getRatingCount() != null ? b.getRatingCount() : 0,
                    b.getAvgRating() != null ? b.getAvgRating().doubleValue() : 0.0),
                Collections.emptyList(),
                "HOT"
            ))
            .collect(Collectors.toList());
    }

    @Override
    public List<RecommendItem> getNewBooks(int months, int limit) {
        LocalDate since = LocalDate.now().minusMonths(months);
        QueryWrapper<Books> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1)
            .ge("publish_date", since)
            .orderByDesc("publish_date", "create_time")
            .last("LIMIT " + limit);

        List<Books> books = booksMapper.selectList(wrapper);
        return books.stream()
            .map(b -> new RecommendItem(
                b.getBookId(), b.getTitle(), b.getAuthor(), b.getCoverImage(),
                b.getAvgRating() != null ? b.getAvgRating().doubleValue() : null,
                0.5,
                String.format("新书推荐（出版于%s）",
                    b.getPublishDate() != null ? b.getPublishDate().toString() : "近期"),
                Collections.emptyList(),
                "NEW"
            ))
            .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> getConfig() {
        List<RecommendConfig> configs = configMapper.selectList(null);
        Map<String, Object> result = new LinkedHashMap<>();
        double total = 0;
        for (RecommendConfig c : configs) {
            result.put(c.getConfigKey(), c.getConfigValue());
            total += c.getConfigValue().doubleValue();
        }
        result.put("total", Math.round(total * 1000.0) / 1000.0);
        return result;
    }

    @Override
    public void updateConfig(Map<String, BigDecimal> weights) {
        // 验证权重之和为1
        double total = weights.values().stream().mapToDouble(BigDecimal::doubleValue).sum();
        if (Math.abs(total - 1.0) > 0.01) {
            throw new BusinessException(ErrorCode.RECOMMEND_CONFIG_INVALID);
        }

        for (Map.Entry<String, BigDecimal> entry : weights.entrySet()) {
            QueryWrapper<RecommendConfig> wrapper = new QueryWrapper<>();
            wrapper.eq("config_key", entry.getKey());
            RecommendConfig config = configMapper.selectOne(wrapper);
            if (config != null) {
                config.setConfigValue(entry.getValue());
                configMapper.updateById(config);
            }
        }
        log.info("推荐权重配置已更新: {}", weights);
    }

    @Override
    public RecommendItem explainRecommendation(Long userId, Long bookId) {
        // 未登录：无法提供个性化解释
        if (userId == null) {
            return null;
        }

        Books book = booksMapper.selectById(bookId);
        if (book == null) {
            return null;
        }

        // 已读图书：说明已被过滤
        Set<Long> ratedBookIds = getRatedBookIds(userId);
        if (ratedBookIds.contains(bookId)) {
            return buildBasicItem(book, 0.0, "READ",
                    "您已读过此书，因此不会出现在个性化推荐中",
                    Collections.emptyList());
        }

        // 在推荐列表中查找：返回合并后的完整解释
        List<RecommendItem> recommendations = getHomeRecommend(userId, 50);
        for (RecommendItem item : recommendations) {
            if (bookId.equals(item.getBookId())) {
                return item;
            }
        }

        // 未进入Top列表：提示关联度较低
        return buildBasicItem(book, 0.0, "NONE",
                "该书暂未进入您的个性化推荐Top列表（与已读图书关联度较低）",
                Collections.emptyList());
    }

    /**
     * 构建基础推荐项（用于解释场景）
     */
    private RecommendItem buildBasicItem(Books book, double score, String source,
                                         String reason, List<String> reasonPath) {
        RecommendItem item = new RecommendItem();
        item.setBookId(book.getBookId());
        item.setTitle(book.getTitle());
        item.setAuthor(book.getAuthor());
        item.setCoverImage(book.getCoverImage());
        item.setAvgRating(book.getAvgRating() != null ? book.getAvgRating().doubleValue() : 0.0);
        item.setScore(score);
        item.setSource(source);
        item.setReason(reason);
        item.setReasonPath(reasonPath);
        return item;
    }

    // ============ 私有方法 ============

    /**
     * 获取用户已评分的图书ID集合（用于过滤已读图书）
     */
    private Set<Long> getRatedBookIds(Long userId) {
        if (userId == null) {
            return Collections.emptySet();
        }
        QueryWrapper<BookRatings> wrapper = new QueryWrapper<>();
        wrapper.select("book_id").eq("user_id", userId);
        return bookRatingsMapper.selectList(wrapper).stream()
            .map(BookRatings::getBookId)
            .collect(Collectors.toSet());
    }

    private Map<String, Double> getWeights() {
        List<RecommendConfig> configs = configMapper.selectList(null);
        Map<String, Double> weights = new HashMap<>();
        weights.put("kg_weight", 0.40);
        weights.put("itemcf_weight", 0.30);
        weights.put("hot_weight", 0.15);
        weights.put("new_weight", 0.15);
        for (RecommendConfig c : configs) {
            weights.put(c.getConfigKey(), c.getConfigValue().doubleValue());
        }
        return weights;
    }

    private List<RecommendItem> weightedMerge(List<RecommendItem> kgItems, List<RecommendItem> cfItems,
                                               List<RecommendItem> hotItems, List<RecommendItem> newItems,
                                               Map<String, Double> weights, int limit, Long userId) {
        Map<Long, RecommendItem> merged = new LinkedHashMap<>();
        Map<Long, Double> scores = new HashMap<>();
        Map<Long, List<String>> reasons = new HashMap<>();

        // KG推荐加权
        for (RecommendItem item : kgItems) {
            double weighted = item.getScore() * weights.get("kg_weight");
            scores.merge(item.getBookId(), weighted, Double::sum);
            merged.putIfAbsent(item.getBookId(), item);
            reasons.computeIfAbsent(item.getBookId(), k -> new ArrayList<>()).add(item.getReason());
        }

        // ItemCF加权
        for (RecommendItem item : cfItems) {
            double weighted = item.getScore() * weights.get("itemcf_weight");
            scores.merge(item.getBookId(), weighted, Double::sum);
            merged.putIfAbsent(item.getBookId(), item);
            reasons.computeIfAbsent(item.getBookId(), k -> new ArrayList<>()).add(item.getReason());
        }

        // 热门加权
        for (RecommendItem item : hotItems) {
            double weighted = item.getScore() * weights.get("hot_weight");
            scores.merge(item.getBookId(), weighted, Double::sum);
            merged.putIfAbsent(item.getBookId(), item);
            reasons.computeIfAbsent(item.getBookId(), k -> new ArrayList<>()).add(item.getReason());
        }

        // 新书加权
        for (RecommendItem item : newItems) {
            double weighted = item.getScore() * weights.get("new_weight");
            scores.merge(item.getBookId(), weighted, Double::sum);
            merged.putIfAbsent(item.getBookId(), item);
            reasons.computeIfAbsent(item.getBookId(), k -> new ArrayList<>()).add(item.getReason());
        }

        // 设置最终分数和理由
        double maxScore = scores.values().stream().mapToDouble(Double::doubleValue).max().orElse(1.0);

        // 获取用户已评分的图书ID，用于过滤已读图书
        Set<Long> ratedBookIds = getRatedBookIds(userId);

        List<RecommendItem> result = new ArrayList<>();
        for (Map.Entry<Long, RecommendItem> entry : merged.entrySet()) {
            Long bookId = entry.getKey();
            // 过滤用户已读图书
            if (ratedBookIds.contains(bookId)) {
                continue;
            }
            RecommendItem item = entry.getValue();
            double finalScore = maxScore > 0 ? scores.get(bookId) / maxScore : 0;
            item.setScore(Math.round(finalScore * 100.0) / 100.0);
            item.setSource("HYBRID");
            List<String> reasonList = reasons.get(bookId);
            if (reasonList != null && !reasonList.isEmpty()) {
                item.setReason(String.join("；", reasonList));
            }
            result.add(item);
        }

        result.sort(Comparator.comparing(RecommendItem::getScore, Comparator.nullsLast(Comparator.reverseOrder())));
        return result.stream().limit(limit).collect(Collectors.toList());
    }

    private List<RecommendItem> mergeAndDedup(List<RecommendItem> list1, List<RecommendItem> list2, int limit) {
        Map<Long, RecommendItem> merged = new LinkedHashMap<>();
        for (RecommendItem item : list1) merged.putIfAbsent(item.getBookId(), item);
        for (RecommendItem item : list2) merged.putIfAbsent(item.getBookId(), item);
        return merged.values().stream().limit(limit).collect(Collectors.toList());
    }

    private double computeHotScore(Books b) {
        // 热度分：浏览量 50% + 评分 30% + 评分人数 20%
        double viewScore = Math.min((b.getViewCount() != null ? b.getViewCount() : 0) / 1000.0, 1.0);
        double ratingScore = b.getAvgRating() != null ? b.getAvgRating().doubleValue() / 5.0 : 0.3;
        double countScore = Math.min((b.getRatingCount() != null ? b.getRatingCount() : 0) / 100.0, 1.0);
        return Math.round((viewScore * 0.5 + ratingScore * 0.3 + countScore * 0.2) * 100.0) / 100.0;
    }
}
