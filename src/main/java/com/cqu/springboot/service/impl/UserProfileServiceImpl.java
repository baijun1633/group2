package com.cqu.springboot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cqu.springboot.entity.*;
import com.cqu.springboot.mapper.*;
import com.cqu.springboot.service.UserProfileService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 用户画像服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {

    private final UserProfileMapper userProfileMapper;
    private final ShelvesMapper shelvesMapper;
    private final ShelfBooksMapper shelfBooksMapper;
    private final BookRatingsMapper bookRatingsMapper;
    private final BooksMapper booksMapper;
    private final ObjectMapper objectMapper;

    @Override
    public Map<String, Object> getUserProfile(Long userId) {
        LambdaQueryWrapper<UserProfile> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserProfile::getUserId, userId);
        UserProfile profile = userProfileMapper.selectOne(wrapper);

        Map<String, Object> result = new HashMap<>();
        result.put("userId", userId);

        if (profile != null) {
            // 解析JSON字段
            result.put("tagVector", parseJsonToMap(profile.getTagVector()));
            result.put("preferredAuthors", parseJsonToList(profile.getPreferredAuthors()));
            result.put("preferredCategories", parseJsonToList(profile.getPreferredCategories()));
            result.put("lastUpdated", profile.getLastUpdated());
        } else {
            result.put("tagVector", new HashMap<>());
            result.put("preferredAuthors", new ArrayList<>());
            result.put("preferredCategories", new ArrayList<>());
            result.put("lastUpdated", null);
        }

        return result;
    }

    @Override
    @Transactional
    public void refreshProfile(Long userId) {
        log.info("开始刷新用户画像: userId={}", userId);

        // 1. 统计标签权重（来自书架中的图书）
        Map<String, Double> tagWeights = new HashMap<>();

        // 获取用户所有书架
        LambdaQueryWrapper<Shelves> shelfWrapper = new LambdaQueryWrapper<>();
        shelfWrapper.eq(Shelves::getUserId, userId);
        List<Shelves> shelves = shelvesMapper.selectList(shelfWrapper);

        for (Shelves shelf : shelves) {
            // 获取书架中的图书
            LambdaQueryWrapper<ShelfBooks> sbWrapper = new LambdaQueryWrapper<>();
            sbWrapper.eq(ShelfBooks::getShelfId, shelf.getShelfId());
            List<ShelfBooks> shelfBooks = shelfBooksMapper.selectList(sbWrapper);

            for (ShelfBooks sb : shelfBooks) {
                Books book = booksMapper.selectById(sb.getBookId());
                if (book != null && book.getTags() != null && !book.getTags().isEmpty()) {
                    // 解析图书标签
                    List<String> bookTags = Arrays.asList(book.getTags().split(","));
                    for (String tag : bookTags) {
                        tag = tag.trim();
                        if (!tag.isEmpty()) {
                            // 根据阅读状态加权
                            double weight = getWeightByStatus(sb.getReadingStatus());
                            tagWeights.merge(tag, weight, Double::sum);
                        }
                    }
                }
            }
        }

        // 2. 统计评分较高的图书的标签（权重更高）
        LambdaQueryWrapper<BookRatings> ratingWrapper = new LambdaQueryWrapper<>();
        ratingWrapper.eq(BookRatings::getUserId, userId)
                .ge(BookRatings::getScore, (byte) 4); // 4分及以上的评分
        List<BookRatings> highRatings = bookRatingsMapper.selectList(ratingWrapper);

        for (BookRatings rating : highRatings) {
            Books book = booksMapper.selectById(rating.getBookId());
            if (book != null && book.getTags() != null && !book.getTags().isEmpty()) {
                List<String> bookTags = Arrays.asList(book.getTags().split(","));
                for (String tag : bookTags) {
                    tag = tag.trim();
                    if (!tag.isEmpty()) {
                        // 高评分图书权重更高
                        double weight = rating.getScore() * 0.5;
                        tagWeights.merge(tag, weight, Double::sum);
                    }
                }
            }
        }

        // 3. 归一化权重
        double maxWeight = tagWeights.values().stream()
                .mapToDouble(Double::doubleValue)
                .max()
                .orElse(1.0);
        Map<String, Double> normalizedTags = new HashMap<>();
        for (Map.Entry<String, Double> entry : tagWeights.entrySet()) {
            normalizedTags.put(entry.getKey(), Math.round(entry.getValue() / maxWeight * 100) / 100.0);
        }

        // 4. 统计偏好作者
        List<String> preferredAuthors = getPreferredAuthors(userId);

        // 5. 统计偏好分类
        List<String> preferredCategories = getPreferredCategories(userId);

        // 6. 保存或更新画像
        LambdaQueryWrapper<UserProfile> profileWrapper = new LambdaQueryWrapper<>();
        profileWrapper.eq(UserProfile::getUserId, userId);
        UserProfile existing = userProfileMapper.selectOne(profileWrapper);

        try {
            if (existing != null) {
                existing.setTagVector(objectMapper.writeValueAsString(normalizedTags));
                existing.setPreferredAuthors(objectMapper.writeValueAsString(preferredAuthors));
                existing.setPreferredCategories(objectMapper.writeValueAsString(preferredCategories));
                existing.setLastUpdated(LocalDateTime.now());
                userProfileMapper.updateById(existing);
            } else {
                UserProfile profile = new UserProfile();
                profile.setUserId(userId);
                profile.setTagVector(objectMapper.writeValueAsString(normalizedTags));
                profile.setPreferredAuthors(objectMapper.writeValueAsString(preferredAuthors));
                profile.setPreferredCategories(objectMapper.writeValueAsString(preferredCategories));
                profile.setLastUpdated(LocalDateTime.now());
                userProfileMapper.insert(profile);
            }
        } catch (JsonProcessingException e) {
            log.error("序列化用户画像失败", e);
        }

        log.info("用户画像刷新完成: userId={}, tagCount={}", userId, normalizedTags.size());
    }

    /**
     * 根据阅读状态返回权重
     */
    private double getWeightByStatus(Integer status) {
        if (status == null) return 1.0;
        return switch (status) {
            case 2 -> 3.0; // 已读 - 高权重
            case 1 -> 2.0; // 在读 - 中权重
            default -> 1.0; // 未读 - 低权重
        };
    }

    /**
     * 获取用户偏好作者
     */
    private List<String> getPreferredAuthors(Long userId) {
        // 从书架中统计作者出现次数
        Map<String, Integer> authorCount = new HashMap<>();

        LambdaQueryWrapper<Shelves> shelfWrapper = new LambdaQueryWrapper<>();
        shelfWrapper.eq(Shelves::getUserId, userId);
        List<Shelves> shelves = shelvesMapper.selectList(shelfWrapper);

        for (Shelves shelf : shelves) {
            LambdaQueryWrapper<ShelfBooks> sbWrapper = new LambdaQueryWrapper<>();
            sbWrapper.eq(ShelfBooks::getShelfId, shelf.getShelfId())
                    .eq(ShelfBooks::getReadingStatus, 2); // 只统计已读
            List<ShelfBooks> shelfBooks = shelfBooksMapper.selectList(sbWrapper);

            for (ShelfBooks sb : shelfBooks) {
                Books book = booksMapper.selectById(sb.getBookId());
                if (book != null && book.getAuthor() != null) {
                    authorCount.merge(book.getAuthor(), 1, Integer::sum);
                }
            }
        }

        // 返回出现次数最多的前5个作者
        return authorCount.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(5)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /**
     * 获取用户偏好分类
     */
    private List<String> getPreferredCategories(Long userId) {
        Map<String, Integer> categoryCount = new HashMap<>();

        LambdaQueryWrapper<Shelves> shelfWrapper = new LambdaQueryWrapper<>();
        shelfWrapper.eq(Shelves::getUserId, userId);
        List<Shelves> shelves = shelvesMapper.selectList(shelfWrapper);

        for (Shelves shelf : shelves) {
            LambdaQueryWrapper<ShelfBooks> sbWrapper = new LambdaQueryWrapper<>();
            sbWrapper.eq(ShelfBooks::getShelfId, shelf.getShelfId());
            List<ShelfBooks> shelfBooks = shelfBooksMapper.selectList(sbWrapper);

            for (ShelfBooks sb : shelfBooks) {
                Books book = booksMapper.selectById(sb.getBookId());
                if (book != null && book.getCategoryId() != null) {
                    // 这里简化处理，实际应该查询categories表获取分类名
                    categoryCount.merge(String.valueOf(book.getCategoryId()), 1, Integer::sum);
                }
            }
        }

        return categoryCount.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(5)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    private Map<String, Object> parseJsonToMap(String json) {
        if (json == null || json.isEmpty()) return new HashMap<>();
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (JsonProcessingException e) {
            return new HashMap<>();
        }
    }

    private List<String> parseJsonToList(String json) {
        if (json == null || json.isEmpty()) return new ArrayList<>();
        try {
            return objectMapper.readValue(json, List.class);
        } catch (JsonProcessingException e) {
            return new ArrayList<>();
        }
    }
}
