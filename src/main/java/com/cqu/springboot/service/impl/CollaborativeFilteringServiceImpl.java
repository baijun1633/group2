package com.cqu.springboot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cqu.springboot.dto.RecommendItem;
import com.cqu.springboot.entity.BookRatings;
import com.cqu.springboot.entity.Books;
import com.cqu.springboot.entity.ShelfBooks;
import com.cqu.springboot.mapper.BookRatingsMapper;
import com.cqu.springboot.mapper.BooksMapper;
import com.cqu.springboot.mapper.ShelfBooksMapper;
import com.cqu.springboot.mapper.ShelvesMapper;
import com.cqu.springboot.service.CollaborativeFilteringService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 协同过滤推荐实现（ItemCF）
 * <p>
 * 算法原理：
 * 1. 收集用户行为（评分 + 书架）构建用户-物品矩阵
 * 2. 计算物品-物品相似度（余弦相似度 + 共现次数）
 * 3. 根据用户历史行为物品，推荐相似物品
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CollaborativeFilteringServiceImpl implements CollaborativeFilteringService {

    private final BookRatingsMapper bookRatingsMapper;
    private final ShelfBooksMapper shelfBooksMapper;
    private final ShelvesMapper shelvesMapper;
    private final BooksMapper booksMapper;

    @Override
    public List<RecommendItem> recommendByItemCF(Long userId, int limit) {
        // 1. 获取用户交互过的图书（评分 + 书架）
        Map<Long, Double> userBookScores = getUserBookScores(userId);
        if (userBookScores.isEmpty()) {
            log.info("用户{}无行为数据，ItemCF返回空", userId);
            return Collections.emptyList();
        }

        // 2. 获取用户已交互的书ID列表
        Set<Long> userBookIds = userBookScores.keySet();

        // 3. 通过共现关系计算候选图书的推荐分数
        Map<Long, Double> candidateScores = new HashMap<>();
        Map<Long, Integer> candidateCoCounts = new HashMap<>();

        for (Long bookId : userBookIds) {
            // 查找与该图书共现的其他图书（被同一批用户交互过）
            List<Long> coOccurredBooks = getCoOccurredBooks(bookId);
            double userScore = userBookScores.get(bookId);

            for (Long coBookId : coOccurredBooks) {
                if (userBookIds.contains(coBookId)) continue; // 跳过已读
                candidateScores.merge(coBookId, userScore, Double::sum);
                candidateCoCounts.merge(coBookId, 1, Integer::sum);
            }
        }

        if (candidateScores.isEmpty()) {
            log.info("用户{}的ItemCF无候选图书", userId);
            return Collections.emptyList();
        }

        // 4. 归一化分数并排序
        double maxScore = candidateScores.values().stream().mapToDouble(Double::doubleValue).max().orElse(1.0);

        List<RecommendItem> result = candidateScores.entrySet().stream()
            .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
            .limit(limit)
            .map(entry -> {
                Long bookId = entry.getKey();
                Books book = booksMapper.selectById(bookId);
                if (book == null) return null;
                double normalizedScore = entry.getValue() / maxScore;
                int coCount = candidateCoCounts.get(bookId);
                return new RecommendItem(
                    bookId, book.getTitle(), book.getAuthor(), book.getCoverImage(),
                    book.getAvgRating() != null ? book.getAvgRating().doubleValue() : null,
                    Math.round(normalizedScore * 100.0) / 100.0,
                    String.format("与您读过的%d本书存在关联（共现%d次）", candidateCoCounts.get(bookId), coCount),
                    Collections.emptyList(),
                    "ITEMCF"
                );
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        return result;
    }

    /**
     * 获取用户交互过的图书及其评分（显式评分 + 隐式反馈）
     */
    private Map<Long, Double> getUserBookScores(Long userId) {
        Map<Long, Double> scores = new HashMap<>();

        // 显式评分（1-5分）
        QueryWrapper<BookRatings> ratingWrapper = new QueryWrapper<>();
        ratingWrapper.eq("user_id", userId);
        bookRatingsMapper.selectList(ratingWrapper).forEach(r -> {
            double score = r.getScore() != null ? r.getScore().doubleValue() : 3.0;
            scores.merge(r.getBookId(), score, Double::max);
        });

        // 隐式反馈（书架：已读=3分，在读=2分，未读=1分）
        QueryWrapper<com.cqu.springboot.entity.Shelves> shelfWrapper = new QueryWrapper<>();
        shelfWrapper.eq("user_id", userId);
        List<com.cqu.springboot.entity.Shelves> userShelves = shelvesMapper.selectList(shelfWrapper);

        for (com.cqu.springboot.entity.Shelves shelf : userShelves) {
            QueryWrapper<ShelfBooks> sbWrapper = new QueryWrapper<>();
            sbWrapper.eq("shelf_id", shelf.getShelfId());
            shelfBooksMapper.selectList(sbWrapper).forEach(sb -> {
                double implicitScore = sb.getReadingStatus() != null
                    ? (sb.getReadingStatus() == 2 ? 3.0 : sb.getReadingStatus() == 1 ? 2.0 : 1.0)
                    : 1.0;
                scores.merge(sb.getBookId(), implicitScore, Double::max);
            });
        }

        return scores;
    }

    /**
     * 获取与指定图书共现的其他图书（被同一批用户交互过）
     */
    private List<Long> getCoOccurredBooks(Long bookId) {
        List<Long> coOccurred = new ArrayList<>();

        // 从评分表找共现：找到评分过该书的用户，再找这些用户评分过的其他书
        QueryWrapper<BookRatings> ratingWrapper = new QueryWrapper<>();
        ratingWrapper.eq("book_id", bookId).select("user_id");
        List<Long> userIds = bookRatingsMapper.selectList(ratingWrapper)
            .stream().map(BookRatings::getUserId).collect(Collectors.toList());

        if (!userIds.isEmpty()) {
            QueryWrapper<BookRatings> coWrapper = new QueryWrapper<>();
            coWrapper.in("user_id", userIds).ne("book_id", bookId).select("book_id");
            coOccurred.addAll(
                bookRatingsMapper.selectList(coWrapper)
                    .stream().map(BookRatings::getBookId).collect(Collectors.toList())
            );
        }

        return coOccurred;
    }
}
