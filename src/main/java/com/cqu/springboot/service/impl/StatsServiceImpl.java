package com.cqu.springboot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cqu.springboot.entity.BookRatings;
import com.cqu.springboot.entity.Books;
import com.cqu.springboot.entity.Reviews;
import com.cqu.springboot.entity.UserBehavior;
import com.cqu.springboot.entity.Users;
import com.cqu.springboot.mapper.BookRatingsMapper;
import com.cqu.springboot.mapper.BooksMapper;
import com.cqu.springboot.mapper.CategoriesMapper;
import com.cqu.springboot.mapper.ReviewsMapper;
import com.cqu.springboot.mapper.UserBehaviorMapper;
import com.cqu.springboot.mapper.UsersMapper;
import com.cqu.springboot.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
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
    private final UserBehaviorMapper userBehaviorMapper;

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

    @Override
    public List<Map<String, Object>> getUserGrowthTrend() {
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(29);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        // 查询近30天每日新增用户数
        QueryWrapper<Users> qw = new QueryWrapper<>();
        qw.select("DATE(create_time) AS date", "COUNT(*) AS count")
          .ge("create_time", startDate.atStartOfDay())
          .groupBy("DATE(create_time)")
          .orderByAsc("date");
        List<Map<String, Object>> raw = usersMapper.selectMaps(qw);

        // 转成 Map<date, count> 方便填充
        Map<String, Long> dateCountMap = new HashMap<>();
        for (Map<String, Object> row : raw) {
            String date = row.get("date").toString();
            Long count = ((Number) row.get("count")).longValue();
            dateCountMap.put(date, count);
        }

        // 填充完整30天（无数据的日期补0）
        List<Map<String, Object>> result = new ArrayList<>();
        for (LocalDate d = startDate; !d.isAfter(today); d = d.plusDays(1)) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("date", d.format(fmt));
            item.put("count", dateCountMap.getOrDefault(d.toString(), 0L));
            result.add(item);
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> getRatingDistribution() {
        // 评分分布：0-1, 1-2, 2-3, 3-4, 4-5 五个区间
        String[] ranges = {"0-1", "1-2", "2-3", "3-4", "4-5"};
        double[][] bounds = {{0, 1}, {1, 2}, {2, 3}, {3, 4}, {4, 5}};

        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 0; i < ranges.length; i++) {
            LambdaQueryWrapper<Books> wrapper = new LambdaQueryWrapper<>();
            wrapper.ge(Books::getAvgRating, bounds[i][0])
                   .lt(Books::getAvgRating, bounds[i][1]);
            long count = booksMapper.selectCount(wrapper);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("range", ranges[i]);
            item.put("count", count);
            result.add(item);
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> getTopBooks(int limit) {
        // 按浏览量排序取 Top N
        QueryWrapper<Books> qw = new QueryWrapper<>();
        qw.select("book_id", "title", "view_count", "avg_rating")
          .eq("status", 1)
          .orderByDesc("view_count")
          .last("LIMIT " + limit);
        List<Map<String, Object>> raw = booksMapper.selectMaps(qw);

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : raw) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("bookId", row.get("book_id"));
            item.put("title", row.get("title"));
            item.put("viewCount", row.get("view_count"));
            item.put("avgRating", row.get("avg_rating"));
            result.add(item);
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> getBehaviorDistribution() {
        QueryWrapper<UserBehavior> qw = new QueryWrapper<>();
        qw.select("behavior_type AS type", "COUNT(*) AS count")
          .groupBy("behavior_type")
          .orderByDesc("count");
        List<Map<String, Object>> raw = userBehaviorMapper.selectMaps(qw);

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : raw) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("type", row.get("type"));
            item.put("count", row.get("count"));
            result.add(item);
        }
        return result;
    }
}
