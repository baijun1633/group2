package com.cqu.springboot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cqu.springboot.entity.BookRatings;
import com.cqu.springboot.entity.Books;
import com.cqu.springboot.entity.Reviews;
import com.cqu.springboot.mapper.BookRatingsMapper;
import com.cqu.springboot.mapper.BooksMapper;
import com.cqu.springboot.mapper.CategoriesMapper;
import com.cqu.springboot.mapper.ReviewsMapper;
import com.cqu.springboot.mapper.UsersMapper;
import com.cqu.springboot.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 系统统计服务实现类
 */
@Service
@RequiredArgsConstructor
public class StatsServiceImpl implements StatsService {

    private final BooksMapper booksMapper;
    private final UsersMapper usersMapper;
    private final ReviewsMapper reviewsMapper;
    private final BookRatingsMapper bookRatingsMapper;
    private final CategoriesMapper categoriesMapper;

    @Override
    public Map<String, Object> getSystemStats() {
        Map<String, Object> stats = new LinkedHashMap<>();

        // 总用户数
        stats.put("totalUsers", usersMapper.selectCount(null));

        // 总图书数
        stats.put("totalBooks", booksMapper.selectCount(null));

        // 上架图书数（status=1）
        stats.put("activeBooks", booksMapper.selectCount(
                new LambdaQueryWrapper<Books>().eq(Books::getStatus, (byte) 1)));

        // 总书评数
        stats.put("totalReviews", reviewsMapper.selectCount(null));

        // 待审核书评数（status=0）
        stats.put("pendingReviews", reviewsMapper.selectCount(
                new LambdaQueryWrapper<Reviews>().eq(Reviews::getStatus, (byte) 0)));

        // 总评分数
        stats.put("totalRatings", bookRatingsMapper.selectCount(null));

        // 总分类数
        stats.put("totalCategories", categoriesMapper.selectCount(null));

        // 近7天活跃用户数（最近7天在 book_ratings 表中有评分记录的不同 user_id 数）
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        List<BookRatings> recentRatings = bookRatingsMapper.selectList(
                new LambdaQueryWrapper<BookRatings>()
                        .select(BookRatings::getUserId)
                        .ge(BookRatings::getCreateTime, sevenDaysAgo));
        long recentActiveUsers = recentRatings.stream()
                .map(BookRatings::getUserId)
                .distinct()
                .count();
        stats.put("recentActiveUsers", recentActiveUsers);

        return stats;
    }
}
