package com.cqu.springboot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cqu.springboot.common.BusinessException;
import com.cqu.springboot.common.ErrorCode;
import com.cqu.springboot.entity.BookRatings;
import com.cqu.springboot.entity.Books;
import com.cqu.springboot.mapper.BookRatingsMapper;
import com.cqu.springboot.mapper.BooksMapper;
import com.cqu.springboot.service.RatingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 图书评分服务实现类
 */
@Service
@RequiredArgsConstructor
public class RatingServiceImpl implements RatingService {

    private final BookRatingsMapper bookRatingsMapper;
    private final BooksMapper booksMapper;

    @Override
    @Transactional
    public void submitRating(Long userId, Long bookId, Byte score) {
        // 验证评分范围
        if (score < 1 || score > 5) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "评分必须在1-5之间");
        }

        // 验证书籍是否存在
        Books book = booksMapper.selectById(bookId);
        if (book == null) {
            throw new BusinessException(ErrorCode.BOOK_NOT_FOUND);
        }

        // 查询是否已评分
        LambdaQueryWrapper<BookRatings> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BookRatings::getUserId, userId)
                .eq(BookRatings::getBookId, bookId);
        BookRatings existing = bookRatingsMapper.selectOne(wrapper);

        if (existing != null) {
            // 更新评分
            existing.setScore(score);
            existing.setUpdateTime(LocalDateTime.now());
            bookRatingsMapper.updateById(existing);
        } else {
            // 新增评分
            BookRatings rating = new BookRatings();
            rating.setUserId(userId);
            rating.setBookId(bookId);
            rating.setScore(score);
            rating.setCreateTime(LocalDateTime.now());
            rating.setUpdateTime(LocalDateTime.now());
            bookRatingsMapper.insert(rating);
        }

        // 更新书籍的平均评分和评分人数
        updateBookRatingStats(bookId);
    }

    @Override
    public Map<String, Object> getRatingStats(Long bookId) {
        // 验证书籍是否存在
        Books book = booksMapper.selectById(bookId);
        if (book == null) {
            throw new BusinessException(ErrorCode.BOOK_NOT_FOUND);
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("bookId", bookId);
        stats.put("avgRating", book.getAvgRating() != null ? book.getAvgRating() : BigDecimal.ZERO);
        stats.put("ratingCount", book.getRatingCount() != null ? book.getRatingCount() : 0);

        // 获取评分分布
        Map<Integer, Long> distribution = new HashMap<>();
        for (int i = 1; i <= 5; i++) {
            LambdaQueryWrapper<BookRatings> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(BookRatings::getBookId, bookId)
                    .eq(BookRatings::getScore, (byte) i);
            distribution.put(i, bookRatingsMapper.selectCount(wrapper));
        }
        stats.put("distribution", distribution);

        return stats;
    }

    /**
     * 更新书籍的平均评分和评分人数
     */
    private void updateBookRatingStats(Long bookId) {
        Map<String, Object> ratingStats = bookRatingsMapper.selectRatingStatsByBookId(bookId);
        Books book = new Books();
        book.setBookId(bookId);

        if (ratingStats.get("avgRating") != null) {
            book.setAvgRating(new BigDecimal(ratingStats.get("avgRating").toString()).setScale(2, RoundingMode.HALF_UP));
        } else {
            book.setAvgRating(BigDecimal.ZERO);
        }

        if (ratingStats.get("ratingCount") != null) {
            book.setRatingCount(((Long) ratingStats.get("ratingCount")).intValue());
        } else {
            book.setRatingCount(0);
        }

        booksMapper.updateById(book);
    }
}
