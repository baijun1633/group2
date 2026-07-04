package com.cqu.springboot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cqu.springboot.dto.RecommendItem;
import com.cqu.springboot.entity.Books;
import com.cqu.springboot.entity.UserProfile;
import com.cqu.springboot.mapper.BooksMapper;
import com.cqu.springboot.mapper.UserProfileMapper;
import com.cqu.springboot.service.KgRecommendService;
import com.cqu.springboot.service.PythonKgClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 知识图谱推理推荐实现
 * <p>
 * 基于 Neo4j 用户个人图谱的多跳路径推理：
 * - 相似读物：1-2跳（同作者/同分类/同标签）
 * - 延伸阅读：3+跳（多跳路径发现间接关联）
 * - 个性化推荐：基于用户画像的图谱查询
 * <p>
 * 所有查询均按 userId 过滤，确保推荐结果来自用户个人图谱
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KgRecommendServiceImpl implements KgRecommendService {

    private final Neo4jClient neo4jClient;
    private final BooksMapper booksMapper;
    private final UserProfileMapper userProfileMapper;
    private final ObjectMapper objectMapper;
    private final PythonKgClient pythonKgClient;

    @Override
    public List<RecommendItem> recommendByKg(Long userId, int limit) {
        if (userId == null) {
            return Collections.emptyList();
        }

        // 1. 获取用户画像
        QueryWrapper<UserProfile> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        UserProfile profile = userProfileMapper.selectOne(wrapper);
        if (profile == null) {
            log.info("用户{}无画像数据，KG推荐返回空", userId);
            return Collections.emptyList();
        }

        // 2. 解析偏好
        List<Long> preferredCategoryIds = parseJsonList(profile.getPreferredCategories(), Long.class);
        List<String> preferredAuthors = parseJsonList(profile.getPreferredAuthors(), String.class);
        List<String> preferredTags = parseJsonArray(profile.getTagVector());

        // 3. 先尝试调用 Python KG 推荐服务
        try {
            List<RecommendItem> pythonResults = pythonKgClient.recommend(
                    preferredTags,
                    preferredCategoryIds.stream().limit(3).map(String::valueOf).collect(Collectors.toList()),
                    preferredCategoryIds.stream().limit(5).map(String::valueOf).collect(Collectors.toList()),
                    preferredAuthors,
                    Collections.emptyList(),
                    limit
            );
            if (pythonResults != null && !pythonResults.isEmpty()) {
                log.info("使用 Python KG 推荐服务返回 {} 条结果", pythonResults.size());
                return pythonResults;
            }
        } catch (Exception e) {
            log.warn("Python KG 推荐服务调用失败，回退到本地推荐: {}", e.getMessage());
        }

        // 4. 回退到本地 Neo4j 推荐
        List<RecommendItem> results = new ArrayList<>();

        // 3. 按偏好分类查询图谱（按用户过滤）
        if (!preferredCategoryIds.isEmpty()) {
            String cypher = "MATCH (b:Book)-[:BELONGS_TO]->(c:Category) " +
                "WHERE b.userId = $userId AND c.categoryId IN $categoryIds " +
                "RETURN b.bookId AS bookId, b.title AS title, b.avgRating AS avgRating, " +
                "c.name AS categoryName " +
                "LIMIT $limit";
            Collection<Map<String, Object>> records = neo4jClient.query(cypher)
                .bindAll(Map.of("userId", userId, "categoryIds", preferredCategoryIds, "limit", limit))
                .fetch().all();
            for (Map<String, Object> r : records) {
                results.add(buildItem(r, "KG",
                    String.format("属于您偏好的分类「%s」", r.get("categoryName")),
                    Arrays.asList("Category:" + r.get("categoryName"), "Book:" + r.get("bookId"))));
            }
        }

        // 4. 按偏好作者查询图谱（按用户过滤）
        if (!preferredAuthors.isEmpty()) {
            String cypher = "MATCH (b:Book)-[:WRITTEN_BY]->(a:Author) " +
                "WHERE b.userId = $userId AND a.name IN $authors " +
                "RETURN b.bookId AS bookId, b.title AS title, b.avgRating AS avgRating, " +
                "a.name AS authorName " +
                "LIMIT $limit";
            Collection<Map<String, Object>> records = neo4jClient.query(cypher)
                .bindAll(Map.of("userId", userId, "authors", preferredAuthors, "limit", limit))
                .fetch().all();
            for (Map<String, Object> r : records) {
                results.add(buildItem(r, "KG",
                    String.format("由您关注的作者「%s」撰写", r.get("authorName")),
                    Arrays.asList("Author:" + r.get("authorName"), "Book:" + r.get("bookId"))));
            }
        }

        // 5. 按偏好标签查询图谱（按用户过滤）
        if (!preferredTags.isEmpty()) {
            String cypher = "MATCH (b:Book)-[:TAGGED_AS]->(t:Tag) " +
                "WHERE b.userId = $userId AND t.name IN $tags " +
                "RETURN b.bookId AS bookId, b.title AS title, b.avgRating AS avgRating, " +
                "t.name AS tagName " +
                "LIMIT $limit";
            Collection<Map<String, Object>> records = neo4jClient.query(cypher)
                .bindAll(Map.of("userId", userId, "tags", preferredTags, "limit", limit))
                .fetch().all();
            for (Map<String, Object> r : records) {
                results.add(buildItem(r, "KG",
                    String.format("带有您喜欢的标签「%s」", r.get("tagName")),
                    Arrays.asList("Tag:" + r.get("tagName"), "Book:" + r.get("bookId"))));
            }
        }

        // 6. 去重 + 按评分排序 + 截取limit
        Map<Long, RecommendItem> dedup = new LinkedHashMap<>();
        for (RecommendItem item : results) {
            dedup.merge(item.getBookId(), item, (existing, incoming) -> {
                existing.setScore(Math.max(existing.getScore(), incoming.getScore()));
                return existing;
            });
        }

        return dedup.values().stream()
            .sorted(Comparator.comparing(RecommendItem::getScore, Comparator.nullsLast(Comparator.reverseOrder())))
            .limit(limit)
            .collect(Collectors.toList());
    }

    @Override
    public List<RecommendItem> getSimilarBooks(Long userId, Long bookId, int limit) {
        if (userId == null) {
            return Collections.emptyList();
        }
        Books sourceBook = booksMapper.selectById(bookId);
        if (sourceBook == null) return Collections.emptyList();

        List<RecommendItem> results = new ArrayList<>();
        int perType = limit / 3 + 1;

        // 1跳：同作者（在用户图谱中）
        String cypherAuthor = "MATCH (b1:Book {bookId: $bookId, userId: $userId})-[:WRITTEN_BY]->(a:Author)<-[:WRITTEN_BY]-(b2:Book {userId: $userId}) " +
            "WHERE b1.bookId <> b2.bookId " +
            "RETURN b2.bookId AS bookId, b2.title AS title, b2.avgRating AS avgRating, a.name AS authorName " +
            "LIMIT $limit";
        Collection<Map<String, Object>> authorBooks = neo4jClient.query(cypherAuthor)
            .bindAll(Map.of("bookId", bookId, "userId", userId, "limit", perType))
            .fetch().all();
        for (Map<String, Object> r : authorBooks) {
            results.add(buildSimilarItem(r, "同作者", sourceBook.getTitle(), r.get("authorName").toString(),
                Arrays.asList("Book:" + bookId, "Author:" + r.get("authorName"), "Book:" + r.get("bookId"))));
        }

        // 1跳：同分类（Category 全局共享，但 Book 需按用户过滤）
        String cypherCategory = "MATCH (b1:Book {bookId: $bookId, userId: $userId})-[:BELONGS_TO]->(c:Category)<-[:BELONGS_TO]-(b2:Book {userId: $userId}) " +
            "WHERE b1.bookId <> b2.bookId " +
            "RETURN b2.bookId AS bookId, b2.title AS title, b2.avgRating AS avgRating, c.name AS categoryName " +
            "LIMIT $limit";
        Collection<Map<String, Object>> categoryBooks = neo4jClient.query(cypherCategory)
            .bindAll(Map.of("bookId", bookId, "userId", userId, "limit", perType))
            .fetch().all();
        for (Map<String, Object> r : categoryBooks) {
            results.add(buildSimilarItem(r, "同分类", sourceBook.getTitle(), r.get("categoryName").toString(),
                Arrays.asList("Book:" + bookId, "Category:" + r.get("categoryName"), "Book:" + r.get("bookId"))));
        }

        // 1跳：同标签（在用户图谱中）
        String cypherTag = "MATCH (b1:Book {bookId: $bookId, userId: $userId})-[:TAGGED_AS]->(t:Tag)<-[:TAGGED_AS]-(b2:Book {userId: $userId}) " +
            "WHERE b1.bookId <> b2.bookId " +
            "RETURN b2.bookId AS bookId, b2.title AS title, b2.avgRating AS avgRating, t.name AS tagName " +
            "LIMIT $limit";
        Collection<Map<String, Object>> tagBooks = neo4jClient.query(cypherTag)
            .bindAll(Map.of("bookId", bookId, "userId", userId, "limit", perType))
            .fetch().all();
        for (Map<String, Object> r : tagBooks) {
            results.add(buildSimilarItem(r, "同标签", sourceBook.getTitle(), r.get("tagName").toString(),
                Arrays.asList("Book:" + bookId, "Tag:" + r.get("tagName"), "Book:" + r.get("bookId"))));
        }

        // 去重并按评分排序
        Map<Long, RecommendItem> dedup = new LinkedHashMap<>();
        for (RecommendItem item : results) {
            dedup.putIfAbsent(item.getBookId(), item);
        }

        return dedup.values().stream()
            .sorted(Comparator.comparing(RecommendItem::getAvgRating, Comparator.nullsLast(Comparator.reverseOrder())))
            .limit(limit)
            .collect(Collectors.toList());
    }

    @Override
    public List<RecommendItem> getExtendedBooks(Long userId, Long bookId, int limit) {
        if (userId == null) {
            return Collections.emptyList();
        }
        Books sourceBook = booksMapper.selectById(bookId);
        if (sourceBook == null) return Collections.emptyList();

        List<RecommendItem> results = new ArrayList<>();
        int perPath = limit / 3 + 1;

        // 3跳路径1：Book → Category → Book → Author → Book（所有Book按用户过滤）
        String cypher1 = "MATCH (b1:Book {bookId: $bookId, userId: $userId})-[:BELONGS_TO]->(c:Category)<-[:BELONGS_TO]-(b2:Book {userId: $userId})-[:WRITTEN_BY]->(a:Author)<-[:WRITTEN_BY]-(b3:Book {userId: $userId}) " +
            "WHERE b1.bookId <> b3.bookId AND b2.bookId <> b3.bookId " +
            "RETURN DISTINCT b3.bookId AS bookId, b3.title AS title, b3.avgRating AS avgRating, " +
            "c.name AS categoryName, a.name AS authorName " +
            "LIMIT $limit";
        Collection<Map<String, Object>> path1 = neo4jClient.query(cypher1)
            .bindAll(Map.of("bookId", bookId, "userId", userId, "limit", perPath))
            .fetch().all();
        for (Map<String, Object> r : path1) {
            RecommendItem item = new RecommendItem(
                toLong(r.get("bookId")), (String) r.get("title"), null, null,
                toDouble(r.get("avgRating")), 0.7,
                String.format("与《%s》同属「%s」，且为「%s」的另一作品",
                    sourceBook.getTitle(), r.get("categoryName"), r.get("authorName")),
                Arrays.asList(
                    "Book:" + bookId,
                    "Category:" + r.get("categoryName"),
                    "Book:(中间书)",
                    "Author:" + r.get("authorName"),
                    "Book:" + r.get("bookId")
                ),
                "KG"
            );
            enrichWithBookInfo(item);
            results.add(item);
        }

        // 3跳路径2：Book → Author → Book → Tag → Book（所有Book按用户过滤）
        String cypher2 = "MATCH (b1:Book {bookId: $bookId, userId: $userId})-[:WRITTEN_BY]->(a:Author)<-[:WRITTEN_BY]-(b2:Book {userId: $userId})-[:TAGGED_AS]->(t:Tag)<-[:TAGGED_AS]-(b3:Book {userId: $userId}) " +
            "WHERE b1.bookId <> b3.bookId AND b2.bookId <> b3.bookId " +
            "RETURN DISTINCT b3.bookId AS bookId, b3.title AS title, b3.avgRating AS avgRating, " +
            "a.name AS authorName, t.name AS tagName " +
            "LIMIT $limit";
        Collection<Map<String, Object>> path2 = neo4jClient.query(cypher2)
            .bindAll(Map.of("bookId", bookId, "userId", userId, "limit", perPath))
            .fetch().all();
        for (Map<String, Object> r : path2) {
            RecommendItem item = new RecommendItem(
                toLong(r.get("bookId")), (String) r.get("title"), null, null,
                toDouble(r.get("avgRating")), 0.65,
                String.format("与《%s》同作者「%s」的作品共享标签「%s」",
                    sourceBook.getTitle(), r.get("authorName"), r.get("tagName")),
                Arrays.asList(
                    "Book:" + bookId,
                    "Author:" + r.get("authorName"),
                    "Book:(中间书)",
                    "Tag:" + r.get("tagName"),
                    "Book:" + r.get("bookId")
                ),
                "KG"
            );
            enrichWithBookInfo(item);
            results.add(item);
        }

        // 3跳路径3：Book → Tag → Book → Category → Book（所有Book按用户过滤）
        String cypher3 = "MATCH (b1:Book {bookId: $bookId, userId: $userId})-[:TAGGED_AS]->(t:Tag)<-[:TAGGED_AS]-(b2:Book {userId: $userId})-[:BELONGS_TO]->(c:Category)<-[:BELONGS_TO]-(b3:Book {userId: $userId}) " +
            "WHERE b1.bookId <> b3.bookId AND b2.bookId <> b3.bookId " +
            "RETURN DISTINCT b3.bookId AS bookId, b3.title AS title, b3.avgRating AS avgRating, " +
            "t.name AS tagName, c.name AS categoryName " +
            "LIMIT $limit";
        Collection<Map<String, Object>> path3 = neo4jClient.query(cypher3)
            .bindAll(Map.of("bookId", bookId, "userId", userId, "limit", perPath))
            .fetch().all();
        for (Map<String, Object> r : path3) {
            RecommendItem item = new RecommendItem(
                toLong(r.get("bookId")), (String) r.get("title"), null, null,
                toDouble(r.get("avgRating")), 0.6,
                String.format("与《%s》共享标签「%s」，且属于「%s」分类",
                    sourceBook.getTitle(), r.get("tagName"), r.get("categoryName")),
                Arrays.asList(
                    "Book:" + bookId,
                    "Tag:" + r.get("tagName"),
                    "Book:(中间书)",
                    "Category:" + r.get("categoryName"),
                    "Book:" + r.get("bookId")
                ),
                "KG"
            );
            enrichWithBookInfo(item);
            results.add(item);
        }

        // 去重并按评分排序
        Map<Long, RecommendItem> dedup = new LinkedHashMap<>();
        for (RecommendItem item : results) {
            dedup.putIfAbsent(item.getBookId(), item);
        }

        return dedup.values().stream()
            .sorted(Comparator.comparing(RecommendItem::getScore, Comparator.nullsLast(Comparator.reverseOrder())))
            .limit(limit)
            .collect(Collectors.toList());
    }

    // ============ 辅助方法 ============

    /**
     * 从 MySQL 补充书籍详细信息（author、coverImage、avgRating）
     */
    private void enrichWithBookInfo(RecommendItem item) {
        if (item == null || item.getBookId() == null) return;
        Books book = booksMapper.selectById(item.getBookId());
        if (book == null) return;
        if (item.getTitle() == null) item.setTitle(book.getTitle());
        if (item.getAuthor() == null) item.setAuthor(book.getAuthor());
        if (item.getCoverImage() == null) item.setCoverImage(book.getCoverImage());
        if (item.getAvgRating() == null && book.getAvgRating() != null) {
            item.setAvgRating(book.getAvgRating().doubleValue());
        }
    }

    private RecommendItem buildItem(Map<String, Object> r, String source, String reason, List<String> path) {
        Long bid = toLong(r.get("bookId"));
        Books book = booksMapper.selectById(bid);
        Double avgRating = toDouble(r.get("avgRating"));
        double score = avgRating != null ? Math.min(avgRating / 5.0, 1.0) : 0.5;
        return new RecommendItem(
            bid,
            book != null ? book.getTitle() : (String) r.get("title"),
            book != null ? book.getAuthor() : null,
            book != null ? book.getCoverImage() : null,
            avgRating,
            Math.round(score * 100.0) / 100.0,
            reason, path, source
        );
    }

    private RecommendItem buildSimilarItem(Map<String, Object> r, String relationType,
                                           String sourceTitle, String relationValue, List<String> path) {
        Long bid = toLong(r.get("bookId"));
        Books book = booksMapper.selectById(bid);
        Double avgRating = toDouble(r.get("avgRating"));
        double score = avgRating != null ? Math.min(avgRating / 5.0, 1.0) : 0.5;
        return new RecommendItem(
            bid,
            book != null ? book.getTitle() : (String) r.get("title"),
            book != null ? book.getAuthor() : null,
            book != null ? book.getCoverImage() : null,
            avgRating,
            Math.round(score * 100.0) / 100.0,
            String.format("与《%s》%s「%s」", sourceTitle, relationType, relationValue),
            path,
            "KG"
        );
    }

    private Long toLong(Object val) {
        if (val == null) return null;
        if (val instanceof Long) return (Long) val;
        if (val instanceof Number) return ((Number) val).longValue();
        return Long.parseLong(val.toString());
    }

    private Double toDouble(Object val) {
        if (val == null) return null;
        if (val instanceof Double) return (Double) val;
        if (val instanceof Number) return ((Number) val).doubleValue();
        try { return Double.parseDouble(val.toString()); } catch (Exception e) { return null; }
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> parseJsonList(String json, Class<T> clazz) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, List.class);
        } catch (Exception e) {
            log.warn("解析JSON失败: {}", json, e);
            return Collections.emptyList();
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> parseJsonArray(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            Map<String, Object> map = objectMapper.readValue(json, Map.class);
            return new ArrayList<>(map.keySet());
        } catch (Exception e) {
            try {
                return objectMapper.readValue(json, List.class);
            } catch (Exception ex) {
                return Collections.emptyList();
            }
        }
    }
}
