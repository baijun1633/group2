package com.cqu.springboot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cqu.springboot.common.BusinessException;
import com.cqu.springboot.common.ErrorCode;
import com.cqu.springboot.dto.KgBuildRequest;
import com.cqu.springboot.dto.KgQueryRequest;
import com.cqu.springboot.entity.Books;
import com.cqu.springboot.entity.BookRatings;
import com.cqu.springboot.entity.Categories;
import com.cqu.springboot.entity.Reviews;
import com.cqu.springboot.entity.ShelfBooks;
import com.cqu.springboot.entity.Shelves;
import com.cqu.springboot.entity.UserKgMetadata;
import com.cqu.springboot.entity.kg.GraphData;
import com.cqu.springboot.mapper.BooksMapper;
import com.cqu.springboot.mapper.BookRatingsMapper;
import com.cqu.springboot.mapper.CategoriesMapper;
import com.cqu.springboot.mapper.ReviewsMapper;
import com.cqu.springboot.mapper.ShelfBooksMapper;
import com.cqu.springboot.mapper.ShelvesMapper;
import com.cqu.springboot.mapper.UserKgMetadataMapper;
import com.cqu.springboot.service.KnowledgeGraphService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KnowledgeGraphServiceImpl implements KnowledgeGraphService {

    private final Neo4jClient neo4jClient;
    private final BooksMapper booksMapper;
    private final CategoriesMapper categoriesMapper;
    private final ShelfBooksMapper shelfBooksMapper;
    private final BookRatingsMapper bookRatingsMapper;
    private final ReviewsMapper reviewsMapper;
    private final UserKgMetadataMapper userKgMetadataMapper;
    private final ShelvesMapper shelvesMapper;
    private final ObjectMapper objectMapper;

    /** 构建任务状态存储（内存，重启后丢失） */
    private final ConcurrentHashMap<String, Map<String, Object>> buildTasks = new ConcurrentHashMap<>();

    // ==================== 图谱构建 ====================

    @Override
    public Map<String, Object> buildGraphForUser(Long userId, KgBuildRequest request) {
        if (request == null) {
            request = new KgBuildRequest();
        }
        String taskId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        // 获取用户关联的书籍
        List<Long> userBookIds = getUserBookIds(userId);
        if (request.getBookIds() != null && !request.getBookIds().isEmpty()) {
            // 取交集：只构建用户拥有的书
            userBookIds.retainAll(new HashSet<>(request.getBookIds()));
        }

        long bookCount = userBookIds.size();

        Map<String, Object> taskInfo = new HashMap<>();
        taskInfo.put("status", "RUNNING");
        taskInfo.put("message", "构建任务已启动");
        taskInfo.put("startTime", System.currentTimeMillis());
        taskInfo.put("bookCount", bookCount);
        taskInfo.put("processedCount", 0);
        taskInfo.put("userId", userId);
        buildTasks.put(taskId, taskInfo);

        // 更新 metadata 状态
        updateMetadataStatus(userId, "building", taskId);
        
        final Boolean forceRebuild = request.getForceRebuild();
        final Long finalUserId = userId;
        final String finalTaskId = taskId;

        CompletableFuture.runAsync(() -> {
            try {
                doBuildUserGraph(finalUserId, userBookIds, forceRebuild, finalTaskId);
            } catch (Exception e) {
                log.error("用户{}图谱构建失败", finalUserId, e);
                Map<String, Object> info = buildTasks.get(finalTaskId);
                if (info != null) {
                    info.put("status", "FAILED");
                    info.put("message", "构建失败: " + e.getMessage());
                    info.put("endTime", System.currentTimeMillis());
                }
                updateMetadataStatus(finalUserId, "failed", null);
            }
        });

        Map<String, Object> result = new HashMap<>();
        result.put("taskId", taskId);
        result.put("estimatedTimeMs", Math.max(500, bookCount * 50));
        result.put("bookCount", bookCount);
        return result;
    }

    @Override
    public Map<String, Object> buildGraph(KgBuildRequest request) {
        if (request == null) {
            request = new KgBuildRequest();
        }
        String taskId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        List<Long> targetUserIds = request.getTargetUserIds();
        if (targetUserIds == null || targetUserIds.isEmpty()) {
            // 获取所有有 metadata 的用户
            List<UserKgMetadata> allMetas = userKgMetadataMapper.selectList(null);
            targetUserIds = allMetas.stream().map(UserKgMetadata::getUserId).collect(Collectors.toList());
        }

        long userCount = targetUserIds.size();
        Map<String, Object> taskInfo = new HashMap<>();
        taskInfo.put("status", "RUNNING");
        taskInfo.put("message", "管理员构建任务已启动");
        taskInfo.put("startTime", System.currentTimeMillis());
        taskInfo.put("userCount", userCount);
        taskInfo.put("processedCount", 0);
        buildTasks.put(taskId, taskInfo);

        final List<Long> finalTargetUserIds = targetUserIds;
        final Boolean forceRebuild = request.getForceRebuild();
        final Set<Long> bookIds = request.getBookIds() != null ? new HashSet<>(request.getBookIds()) : null;
        final String finalTaskId = taskId;

        CompletableFuture.runAsync(() -> {
            int processed = 0;
            for (Long uid : finalTargetUserIds) {
                try {
                    List<Long> userBookIds = getUserBookIds(uid);
                    if (bookIds != null && !bookIds.isEmpty()) {
                        userBookIds.retainAll(bookIds);
                    }
                    doBuildUserGraph(uid, userBookIds, forceRebuild, null);
                    processed++;
                    Map<String, Object> info = buildTasks.get(finalTaskId);
                    if (info != null) {
                        info.put("processedCount", processed);
                    }
                } catch (Exception e) {
                    log.error("用户{}图谱构建失败", uid, e);
                }
            }
            Map<String, Object> info = buildTasks.get(finalTaskId);
            if (info != null) {
                info.put("status", "COMPLETED");
                info.put("message", "构建完成");
                info.put("endTime", System.currentTimeMillis());
                info.put("processedCount", processed);
            }
            log.info("管理员批量构建完成，共处理 {} 个用户", processed);
        });

        Map<String, Object> result = new HashMap<>();
        result.put("taskId", taskId);
        result.put("estimatedTimeMs", Math.max(500, userCount * 200));
        result.put("userCount", userCount);
        return result;
    }

    /**
     * 实际执行用户图谱构建
     */
    private void doBuildUserGraph(Long userId, List<Long> bookIds, Boolean forceRebuild, String taskId) {
        // 全量重建时先清空该用户的数据
        if (Boolean.TRUE.equals(forceRebuild)) {
            log.info("用户{}全量重建：清空已有图谱数据", userId);
            neo4jClient.query("MATCH (n) WHERE n.userId = $userId DETACH DELETE n")
                    .bind(userId).to("userId").run();
        }

        // 1. 确保全局 Category 节点存在
        ensureCategoryNodes();

        // 2. 逐本创建节点和关系
        int processed = 0;
        for (Long bookId : bookIds) {
            try {
                Books book = booksMapper.selectById(bookId);
                if (book != null) {
                    createBookNodeAndRelations(userId, book);
                    processed++;
                }
            } catch (Exception e) {
                log.error("创建用户{}书籍图谱节点失败, bookId={}", userId, bookId, e);
            }
            // 更新进度
            if (taskId != null) {
                Map<String, Object> info = buildTasks.get(taskId);
                if (info != null) {
                    info.put("processedCount", processed);
                }
            }
        }

        // 3. 统计实体数量
        long entityCount = countUserEntities(userId);

        // 4. 更新 metadata
        UserKgMetadata meta = getOrCreateMetadata(userId);
        meta.setEntityCount((int) entityCount);
        meta.setStatus("idle");
        meta.setLastBuildTime(LocalDateTime.now());
        meta.setUpdatedAt(LocalDateTime.now());
        if (taskId != null) {
            meta.setTaskId(taskId);
        }
        userKgMetadataMapper.updateById(meta);

        Map<String, Object> info = taskId != null ? buildTasks.get(taskId) : null;
        if (info != null) {
            info.put("status", "COMPLETED");
            info.put("message", "构建完成");
            info.put("endTime", System.currentTimeMillis());
            info.put("processedCount", processed);
            info.put("entityCount", entityCount);
        }
        log.info("用户{}图谱构建完成，共处理 {} 本书，{} 个实体", userId, processed, entityCount);
    }

    /**
     * 确保全局 Category 节点存在（共享，无 userId）
     */
    private void ensureCategoryNodes() {
        List<Categories> categories = categoriesMapper.selectList(null);
        for (Categories cat : categories) {
            neo4jClient.query(
                    "MERGE (c:Category {categoryId: $categoryId}) SET c.name = $name")
                    .bind(cat.getCategoryId()).to("categoryId")
                    .bind(cat.getName()).to("name")
                    .run();
        }
    }

    /**
     * 为用户创建书籍节点及关联节点
     */
    private void createBookNodeAndRelations(Long userId, Books book) {
        Double avgRating = book.getAvgRating() != null ? book.getAvgRating().doubleValue() : null;

        // 创建 Book 节点（带 userId）
        neo4jClient.query(
                "MERGE (b:Book {bookId: $bookId, userId: $userId}) "
                        + "SET b.title = $title, b.isbn = $isbn, b.description = $description, "
                        + "b.avgRating = $avgRating, b.coverImage = $coverImage")
                .bind(book.getBookId()).to("bookId")
                .bind(userId).to("userId")
                .bind(book.getTitle()).to("title")
                .bind(book.getIsbn()).to("isbn")
                .bind(book.getDescription()).to("description")
                .bind(avgRating).to("avgRating")
                .bind(book.getCoverImage()).to("coverImage")
                .run();

        // Author 节点（带 userId）+ WRITTEN_BY 关系
        if (book.getAuthor() != null && !book.getAuthor().isBlank()) {
            neo4jClient.query(
                    "MATCH (b:Book {bookId: $bookId, userId: $userId}) "
                            + "MERGE (a:Author {name: $authorName, userId: $userId}) "
                            + "MERGE (b)-[:WRITTEN_BY]->(a)")
                    .bind(book.getBookId()).to("bookId")
                    .bind(userId).to("userId")
                    .bind(book.getAuthor()).to("authorName")
                    .run();
        }

        // Publisher 节点（带 userId）+ PUBLISHED_BY 关系
        if (book.getPublisher() != null && !book.getPublisher().isBlank()) {
            neo4jClient.query(
                    "MATCH (b:Book {bookId: $bookId, userId: $userId}) "
                            + "MERGE (p:Publisher {name: $publisherName, userId: $userId}) "
                            + "MERGE (b)-[:PUBLISHED_BY]->(p)")
                    .bind(book.getBookId()).to("bookId")
                    .bind(userId).to("userId")
                    .bind(book.getPublisher()).to("publisherName")
                    .run();
        }

        // Category 节点（全局共享，无 userId）+ BELONGS_TO 关系
        if (book.getCategoryId() != null) {
            neo4jClient.query(
                    "MATCH (b:Book {bookId: $bookId, userId: $userId}) "
                            + "MATCH (c:Category {categoryId: $categoryId}) "
                            + "MERGE (b)-[:BELONGS_TO]->(c)")
                    .bind(book.getBookId()).to("bookId")
                    .bind(userId).to("userId")
                    .bind(book.getCategoryId()).to("categoryId")
                    .run();
        }

        // Tag 节点（带 userId）+ TAGGED_AS 关系
        List<String> tags = parseTags(book.getTags());
        for (String tag : tags) {
            if (tag != null && !tag.isBlank()) {
                neo4jClient.query(
                        "MATCH (b:Book {bookId: $bookId, userId: $userId}) "
                                + "MERGE (t:Tag {name: $tagName, userId: $userId}) "
                                + "MERGE (b)-[:TAGGED_AS]->(t)")
                        .bind(book.getBookId()).to("bookId")
                        .bind(userId).to("userId")
                        .bind(tag.trim()).to("tagName")
                        .run();
            }
        }
    }

    /**
     * 解析标签 JSON 字符串
     */
    private List<String> parseTags(String tagsJson) {
        if (tagsJson == null || tagsJson.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(tagsJson, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            String cleaned = tagsJson.replace("[", "").replace("]", "").replace("\"", "");
            if (cleaned.isBlank()) {
                return Collections.emptyList();
            }
            return Arrays.asList(cleaned.split(","));
        }
    }

    // ==================== 用户书籍查询 ====================

    /**
     * 获取用户关联的所有书籍ID（书架 + 评分 + 书评）
     */
    private List<Long> getUserBookIds(Long userId) {
        Set<Long> bookIds = new HashSet<>();

        // 1. 书架中的书：shelves → shelf_books → book_id
        LambdaQueryWrapper<Shelves> shelfQuery = new LambdaQueryWrapper<>();
        shelfQuery.eq(Shelves::getUserId, userId);
        List<Shelves> userShelves = shelvesMapper.selectList(shelfQuery);
        if (!userShelves.isEmpty()) {
            List<Long> shelfIds = userShelves.stream()
                    .map(Shelves::getShelfId)
                    .collect(Collectors.toList());
            LambdaQueryWrapper<ShelfBooks> shelfBookQuery = new LambdaQueryWrapper<>();
            shelfBookQuery.in(ShelfBooks::getShelfId, shelfIds);
            shelfBookQuery.select(ShelfBooks::getBookId);
            List<ShelfBooks> shelfBooks = shelfBooksMapper.selectList(shelfBookQuery);
            shelfBooks.forEach(sb -> bookIds.add(sb.getBookId()));
        }

        // 2. 用户评分过的书
        LambdaQueryWrapper<BookRatings> ratingQuery = new LambdaQueryWrapper<>();
        ratingQuery.eq(BookRatings::getUserId, userId);
        ratingQuery.select(BookRatings::getBookId);
        ratingQuery.groupBy(BookRatings::getBookId);
        List<BookRatings> ratings = bookRatingsMapper.selectList(ratingQuery);
        ratings.forEach(r -> bookIds.add(r.getBookId()));

        // 3. 用户写过书评的书
        LambdaQueryWrapper<Reviews> reviewQuery = new LambdaQueryWrapper<>();
        reviewQuery.eq(Reviews::getUserId, userId);
        reviewQuery.select(Reviews::getBookId);
        reviewQuery.groupBy(Reviews::getBookId);
        List<Reviews> reviews = reviewsMapper.selectList(reviewQuery);
        reviews.forEach(r -> bookIds.add(r.getBookId()));

        return new ArrayList<>(bookIds);
    }

    // ==================== Cypher 查询 ====================

    @Override
    public List<Map<String, Object>> query(Long userId, KgQueryRequest request) {
        String cypher = request.getCypher();
        if (cypher == null || cypher.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Cypher查询语句不能为空");
        }
        validateCypher(cypher);

        Integer limit = request.getLimit() != null ? request.getLimit() : 100;
        String finalCypher = cypher;
        if (!cypher.toUpperCase().contains("LIMIT")) {
            finalCypher = cypher + " LIMIT " + Math.min(limit, 1000);
        }

        // 注入 userId 参数到 Cypher 查询中
        // 注意：用户传入的 Cypher 应当使用 $userId 参数
        Collection<Map<String, Object>> result = neo4jClient.query(finalCypher)
                .bind(userId).to("userId")
                .fetch().all();
        return new ArrayList<>(result);
    }

    private void validateCypher(String cypher) {
        String upper = cypher.toUpperCase();
        String[] forbidden = {"CREATE", "DELETE", "DROP", "REMOVE", "MERGE", "DETACH", "CALL", "FOREACH"};
        for (String word : forbidden) {
            if (upper.matches(".*\\b" + word + "\\b.*")) {
                throw new BusinessException(ErrorCode.KG_QUERY_INVALID,
                        "查询语句不允许包含修改操作: " + word);
            }
        }
    }

    // ==================== 图谱可视化 ====================

    @Override
    public GraphData getGraphOverview(Long userId) {
        // 查询用户所有 Book + Author + Category + Tag + Publisher 及其关系
        String cypher =
                "MATCH (b:Book {userId: $userId}) " +
                "OPTIONAL MATCH (b)-[r1:WRITTEN_BY]->(a:Author {userId: $userId}) " +
                "OPTIONAL MATCH (b)-[r2:BELONGS_TO]->(c:Category) " +
                "OPTIONAL MATCH (b)-[r3:TAGGED_AS]->(t:Tag {userId: $userId}) " +
                "OPTIONAL MATCH (b)-[r4:PUBLISHED_BY]->(p:Publisher {userId: $userId}) " +
                "RETURN " +
                "  collect(DISTINCT {id: id(b), label: b.title, type: 'Book', props: properties(b)}) AS bookNodes, " +
                "  collect(DISTINCT {id: id(a), label: a.name, type: 'Author', props: properties(a)}) AS authorNodes, " +
                "  collect(DISTINCT {id: id(c), label: c.name, type: 'Category', props: properties(c)}) AS categoryNodes, " +
                "  collect(DISTINCT {id: id(t), label: t.name, type: 'Tag', props: properties(t)}) AS tagNodes, " +
                "  collect(DISTINCT {id: id(p), label: p.name, type: 'Publisher', props: properties(p)}) AS publisherNodes";

        Collection<Map<String, Object>> results = neo4jClient.query(cypher)
                .bind(userId).to("userId")
                .fetch().all();

        GraphData graphData = new GraphData();
        List<GraphData.GraphNode> nodes = new ArrayList<>();
        List<GraphData.GraphEdge> edges = new ArrayList<>();
        Map<Long, String> nodeIdMap = new HashMap<>();

        if (!results.isEmpty()) {
            Map<String, Object> row = results.iterator().next();

            addNodesFromList(row, "bookNodes", nodes, nodeIdMap);
            addNodesFromList(row, "authorNodes", nodes, nodeIdMap);
            addNodesFromList(row, "categoryNodes", nodes, nodeIdMap);
            addNodesFromList(row, "tagNodes", nodes, nodeIdMap);
            addNodesFromList(row, "publisherNodes", nodes, nodeIdMap);
        }

        // 查询用户所有直接关系
        String relCypher =
                "MATCH (b:Book {userId: $userId})-[r]->(n) " +
                "WHERE (n:Author AND n.userId = $userId) OR n:Category OR (n:Tag AND n.userId = $userId) OR (n:Publisher AND n.userId = $userId) " +
                "RETURN id(startNode(r)) AS sourceId, id(endNode(r)) AS targetId, type(r) AS relType";
        Collection<Map<String, Object>> relResults = neo4jClient.query(relCypher)
                .bind(userId).to("userId")
                .fetch().all();

        for (Map<String, Object> row : relResults) {
            Long sourceId = ((Number) row.get("sourceId")).longValue();
            Long targetId = ((Number) row.get("targetId")).longValue();
            String relType = (String) row.get("relType");

            String source = nodeIdMap.get(sourceId);
            String target = nodeIdMap.get(targetId);
            if (source != null && target != null) {
                edges.add(new GraphData.GraphEdge(source, target, relType));
            }
        }

        graphData.setNodes(nodes);
        graphData.setEdges(edges);
        return graphData;
    }

    @SuppressWarnings("unchecked")
    private void addNodesFromList(Map<String, Object> row, String key,
                                  List<GraphData.GraphNode> nodes, Map<Long, String> nodeIdMap) {
        Object raw = row.get(key);
        if (raw == null) return;
        List<Map<String, Object>> nodeList = (List<Map<String, Object>>) raw;
        for (Map<String, Object> item : nodeList) {
            if (item == null) continue;
            Number nId = (Number) item.get("id");
            if (nId == null) continue;
            Long nodeId = nId.longValue();
            if (nodeIdMap.containsKey(nodeId)) continue;

            String label = (String) item.get("label");
            String type = (String) item.get("type");
            Map<String, Object> props = (Map<String, Object>) item.get("props");

            String graphNodeId = type + ":" + nodeId;
            GraphData.GraphNode node = new GraphData.GraphNode(graphNodeId, label, type);
            if (props != null) {
                node.getProperties().putAll(props);
            }
            nodes.add(node);
            nodeIdMap.put(nodeId, graphNodeId);
        }
    }

    @Override
    public GraphData getGraph(Long userId, String entityType, String entityId, Integer depth) {
        if (depth == null || depth < 1) {
            depth = 2;
        }
        if (depth > 5) {
            depth = 5;
        }

        String label = capitalize(entityType);
        String matchProperty;
        Object matchValue;
        boolean isCategory = "Category".equals(label);
        switch (label) {
            case "Book":
                matchProperty = "bookId";
                matchValue = Long.parseLong(entityId);
                break;
            case "Category":
                matchProperty = "categoryId";
                matchValue = Long.parseLong(entityId);
                break;
            case "Author":
            case "Publisher":
            case "Tag":
                matchProperty = "name";
                matchValue = entityId;
                break;
            default:
                throw new BusinessException(ErrorCode.PARAM_ERROR, "不支持的实体类型: " + entityType);
        }

        // 根据是否 Category 决定是否添加 userId 过滤
        String userIdFilter = isCategory ? "" : " AND n.userId = $userId";
        String userIdFilterM = isCategory ? "" : " AND m.userId = $userId";

        // 查询所有相关节点
        String nodeCypher = String.format(
                "MATCH (n:%s {%s: $entityId}%s)-[*0..%d]-(m) "
                        + "RETURN DISTINCT id(m) AS nodeId, labels(m) AS labels, properties(m) AS props",
                label, matchProperty, userIdFilter, depth);
        Collection<Map<String, Object>> nodeResults;
        if (isCategory) {
            nodeResults = neo4jClient.query(nodeCypher)
                    .bind(matchValue).to("entityId")
                    .fetch().all();
        } else {
            nodeResults = neo4jClient.query(nodeCypher)
                    .bind(matchValue).to("entityId")
                    .bind(userId).to("userId")
                    .fetch().all();
        }

        // 查询所有相关关系
        String relCypher = String.format(
                "MATCH (n:%s {%s: $entityId}%s)-[*0..%d]-(m) "
                        + "WHERE (m:Category) OR (m.userId = $userId) "
                        + "WITH collect(DISTINCT m) AS nodes "
                        + "UNWIND nodes AS node "
                        + "MATCH (node)-[r]-(other) "
                        + "WHERE other IN nodes "
                        + "RETURN DISTINCT id(startNode(r)) AS sourceId, id(endNode(r)) AS targetId, type(r) AS relType",
                label, matchProperty, userIdFilter, depth);
        Collection<Map<String, Object>> relResults;
        if (isCategory) {
            relResults = neo4jClient.query(relCypher)
                    .bind(matchValue).to("entityId")
                    .bind(userId).to("userId")
                    .fetch().all();
        } else {
            relResults = neo4jClient.query(relCypher)
                    .bind(matchValue).to("entityId")
                    .bind(userId).to("userId")
                    .fetch().all();
        }

        // 构建 GraphData
        GraphData graphData = new GraphData();
        List<GraphData.GraphNode> nodes = new ArrayList<>();
        List<GraphData.GraphEdge> edges = new ArrayList<>();
        Map<Long, String> nodeIdMap = new HashMap<>();

        for (Map<String, Object> row : nodeResults) {
            Long nodeId = ((Number) row.get("nodeId")).longValue();
            @SuppressWarnings("unchecked")
            List<String> labels = (List<String>) row.get("labels");
            @SuppressWarnings("unchecked")
            Map<String, Object> props = (Map<String, Object>) row.get("props");

            String type = labels != null && !labels.isEmpty() ? labels.get(0) : "Unknown";
            String displayLabel = determineLabel(type, props);
            String graphNodeId = type + ":" + nodeId;

            GraphData.GraphNode node = new GraphData.GraphNode(graphNodeId, displayLabel, type);
            if (props != null) {
                node.getProperties().putAll(props);
            }
            nodes.add(node);
            nodeIdMap.put(nodeId, graphNodeId);
        }

        for (Map<String, Object> row : relResults) {
            Long sourceId = ((Number) row.get("sourceId")).longValue();
            Long targetId = ((Number) row.get("targetId")).longValue();
            String relType = (String) row.get("relType");

            String source = nodeIdMap.get(sourceId);
            String target = nodeIdMap.get(targetId);
            if (source != null && target != null) {
                edges.add(new GraphData.GraphEdge(source, target, relType));
            }
        }

        graphData.setNodes(nodes);
        graphData.setEdges(edges);
        return graphData;
    }

    private String determineLabel(String type, Map<String, Object> props) {
        if (props == null) return type;
        switch (type) {
            case "Book":
                Object title = props.get("title");
                return title != null ? title.toString() : "Book";
            case "Author":
            case "Publisher":
            case "Tag":
                Object name = props.get("name");
                return name != null ? name.toString() : type;
            case "Category":
                Object catName = props.get("name");
                return catName != null ? catName.toString() : "Category";
            default:
                return type;
        }
    }

    // ==================== 图谱实体操作 ====================

    /** 实体类型 -> Neo4j 标签映射 */
    private static final Map<String, String> ENTITY_LABELS = Map.of(
            "author", "Author",
            "publisher", "Publisher",
            "tag", "Tag",
            "category", "Category"
    );

    /** 校验实体类型并返回 Neo4j 标签名 */
    private String resolveEntityLabel(String type) {
        if (type == null || type.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "实体类型不能为空");
        }
        String label = ENTITY_LABELS.get(type.toLowerCase());
        if (label == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "不支持的实体类型: " + type + "，支持: author/publisher/tag/category");
        }
        return label;
    }

    @Override
    public Map<String, Object> listEntities(Long userId, String type, int page, int size) {
        String label = resolveEntityLabel(type);
        if (page < 1) page = 1;
        if (size < 1 || size > 100) size = 20;
        int skip = (page - 1) * size;

        boolean isCategory = "Category".equals(label);
        String userIdCondition = isCategory ? "" : " WHERE n.userId = $userId";

        // 查询总数
        String countCypher = "MATCH (n:" + label + ")" + userIdCondition + " RETURN count(n) AS cnt";
        Collection<Map<String, Object>> countResult;
        if (isCategory) {
            countResult = neo4jClient.query(countCypher).fetch().all();
        } else {
            countResult = neo4jClient.query(countCypher)
                    .bind(userId).to("userId")
                    .fetch().all();
        }
        long total = countResult.isEmpty() ? 0
                : ((Number) countResult.iterator().next().get("cnt")).longValue();

        // 分页查询 + 关联图书数
        String bookMatch = isCategory
                ? "OPTIONAL MATCH (n)<-[r]-(b:Book)"
                : "OPTIONAL MATCH (n)<-[r]-(b:Book) WHERE b.userId = $userId";
        String listCypher = "MATCH (n:" + label + ")" + userIdCondition + " "
                + bookMatch + " "
                + "RETURN n.name AS name, count(DISTINCT b) AS bookCount "
                + "ORDER BY n.name SKIP $skip LIMIT $limit";

        Collection<Map<String, Object>> rows;
        if (isCategory) {
            rows = neo4jClient.query(listCypher)
                    .bind(skip).to("skip")
                    .bind(size).to("limit")
                    .fetch().all();
        } else {
            rows = neo4jClient.query(listCypher)
                    .bind(userId).to("userId")
                    .bind(skip).to("skip")
                    .bind(size).to("limit")
                    .fetch().all();
        }

        List<Map<String, Object>> items = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("type", type.toLowerCase());
            item.put("name", row.get("name"));
            item.put("bookCount", ((Number) row.get("bookCount")).intValue());
            items.add(item);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("type", type.toLowerCase());
        result.put("label", label);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        result.put("items", items);
        return result;
    }

    @Override
    public Map<String, Object> getEntity(Long userId, String type, String name) {
        String label = resolveEntityLabel(type);
        if (name == null || name.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "实体名称不能为空");
        }

        boolean isCategory = "Category".equals(label);
        String detailCypher;
        Collection<Map<String, Object>> rows;

        if (isCategory) {
            detailCypher = "MATCH (n:" + label + " {name: $name}) "
                    + "OPTIONAL MATCH (n)<-[r]-(b:Book) "
                    + "RETURN n.name AS name, "
                    + "collect(DISTINCT {bookId: b.bookId, title: b.title}) AS books";
            rows = neo4jClient.query(detailCypher)
                    .bind(name).to("name")
                    .fetch().all();
        } else {
            detailCypher = "MATCH (n:" + label + " {name: $name, userId: $userId}) "
                    + "OPTIONAL MATCH (n)<-[r]-(b:Book {userId: $userId}) "
                    + "RETURN n.name AS name, "
                    + "collect(DISTINCT {bookId: b.bookId, title: b.title}) AS books";
            rows = neo4jClient.query(detailCypher)
                    .bind(name).to("name")
                    .bind(userId).to("userId")
                    .fetch().all();
        }

        if (rows.isEmpty()) {
            throw new BusinessException(ErrorCode.KG_NODE_NOT_FOUND,
                    "实体不存在: " + type + "/" + name);
        }

        Map<String, Object> row = rows.iterator().next();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("type", type.toLowerCase());
        result.put("label", label);
        result.put("name", row.get("name"));
        Object booksObj = row.get("books");
        List<Object> books = booksObj instanceof List ? (List<Object>) booksObj : Collections.emptyList();
        result.put("bookCount", books.size());
        result.put("books", books);
        return result;
    }

    @Override
    public void removeEntity(Long userId, String type, String name) {
        String label = resolveEntityLabel(type);
        if (name == null || name.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "实体名称不能为空");
        }

        // 不允许移除 Category 类型（全局共享）
        if ("Category".equals(label)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "不允许移除 Category 类型节点");
        }

        // 检查是否存在
        Collection<Map<String, Object>> existing = neo4jClient.query(
                "MATCH (n:" + label + " {name: $name, userId: $userId}) RETURN n.name AS name LIMIT 1")
                .bind(name).to("name")
                .bind(userId).to("userId")
                .fetch().all();
        if (existing.isEmpty()) {
            throw new BusinessException(ErrorCode.KG_NODE_NOT_FOUND,
                    "实体不存在: " + type + "/" + name);
        }

        // 删除节点及其关系
        neo4jClient.query("MATCH (n:" + label + " {name: $name, userId: $userId}) DETACH DELETE n")
                .bind(name).to("name")
                .bind(userId).to("userId")
                .run();

        // 清理孤立的 Author/Publisher/Tag 节点（没有 Book 连接的）
        cleanupOrphanNodes(userId);

        // 更新 metadata
        long entityCount = countUserEntities(userId);
        UserKgMetadata meta = getOrCreateMetadata(userId);
        meta.setEntityCount((int) entityCount);
        meta.setUpdatedAt(LocalDateTime.now());
        userKgMetadataMapper.updateById(meta);

        log.info("移除用户{}图谱实体（含关系）: {}:{}", userId, label, name);
    }

    /**
     * 清理用户图谱中没有 Book 连接的孤立 Author/Publisher/Tag 节点
     */
    private void cleanupOrphanNodes(Long userId) {
        String[] orphanLabels = {"Author", "Publisher", "Tag"};
        for (String label : orphanLabels) {
            neo4jClient.query(
                    "MATCH (n:" + label + " {userId: $userId}) "
                            + "WHERE NOT (n)<-[:WRITTEN_BY]-(:Book {userId: $userId}) "
                            + "AND NOT (n)<-[:PUBLISHED_BY]-(:Book {userId: $userId}) "
                            + "AND NOT (n)<-[:TAGGED_AS]-(:Book {userId: $userId}) "
                            + "DETACH DELETE n")
                    .bind(userId).to("userId")
                    .run();
        }
    }

    // ==================== 用户图谱增量操作 ====================

    @Override
    public void addBookToUserGraph(Long userId, Long bookId) {
        Books book = booksMapper.selectById(bookId);
        if (book == null) {
            throw new BusinessException(ErrorCode.BOOK_NOT_FOUND, "书籍不存在: " + bookId);
        }

        // 确保全局 Category 节点存在
        ensureCategoryNodes();

        // 创建书籍节点和关联
        createBookNodeAndRelations(userId, book);

        // 更新 metadata
        long entityCount = countUserEntities(userId);
        UserKgMetadata meta = getOrCreateMetadata(userId);
        meta.setEntityCount((int) entityCount);
        meta.setUpdatedAt(LocalDateTime.now());
        userKgMetadataMapper.updateById(meta);

        log.info("将书籍{}添加到用户{}图谱", bookId, userId);
    }

    @Override
    public void removeBookFromUserGraph(Long userId, Long bookId) {
        // 检查书籍节点是否存在
        Collection<Map<String, Object>> existing = neo4jClient.query(
                "MATCH (b:Book {bookId: $bookId, userId: $userId}) RETURN b.bookId AS bookId LIMIT 1")
                .bind(bookId).to("bookId")
                .bind(userId).to("userId")
                .fetch().all();
        if (existing.isEmpty()) {
            throw new BusinessException(ErrorCode.KG_NODE_NOT_FOUND,
                    "用户图谱中不存在该书籍: " + bookId);
        }

        // 删除书籍节点及其关系
        neo4jClient.query("MATCH (b:Book {bookId: $bookId, userId: $userId}) DETACH DELETE b")
                .bind(bookId).to("bookId")
                .bind(userId).to("userId")
                .run();

        // 清理孤立节点
        cleanupOrphanNodes(userId);

        // 更新 metadata
        long entityCount = countUserEntities(userId);
        UserKgMetadata meta = getOrCreateMetadata(userId);
        meta.setEntityCount((int) entityCount);
        meta.setUpdatedAt(LocalDateTime.now());
        userKgMetadataMapper.updateById(meta);

        log.info("从用户{}图谱中移除书籍{}", userId, bookId);
    }

    // ==================== 构建状态和统计 ====================

    @Override
    public Map<String, Object> getBuildStatus(String taskId) {
        Map<String, Object> status = buildTasks.get(taskId);
        if (status == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "构建任务不存在: " + taskId);
        }
        return status;
    }

    @Override
    public Map<String, Object> getGraphStats(Long userId) {
        Map<String, Object> stats = new HashMap<>();
        String[] nodeTypes = {"Book", "Author", "Publisher", "Tag"};
        for (String type : nodeTypes) {
            Collection<Map<String, Object>> result = neo4jClient.query(
                    "MATCH (n:" + type + " {userId: $userId}) RETURN count(n) AS count")
                    .bind(userId).to("userId")
                    .fetch().all();
            if (!result.isEmpty()) {
                stats.put(type + "Count", result.iterator().next().get("count"));
            }
        }
        // Category 是全局共享的，需要特殊处理
        Collection<Map<String, Object>> catResult = neo4jClient.query(
                "MATCH (c:Category)<-[:BELONGS_TO]-(b:Book {userId: $userId}) RETURN count(DISTINCT c) AS count")
                .bind(userId).to("userId")
                .fetch().all();
        if (!catResult.isEmpty()) {
            stats.put("CategoryCount", catResult.iterator().next().get("count"));
        }

        String[] relTypes = {"WRITTEN_BY", "PUBLISHED_BY", "BELONGS_TO", "TAGGED_AS"};
        for (String relType : relTypes) {
            Collection<Map<String, Object>> result = neo4jClient.query(
                    "MATCH (b:Book {userId: $userId})-[r:" + relType + "]->() RETURN count(r) AS count")
                    .bind(userId).to("userId")
                    .fetch().all();
            if (!result.isEmpty()) {
                stats.put(relType + "Count", result.iterator().next().get("count"));
            }
        }
        return stats;
    }

    @Override
    public Map<String, Object> getMetadata(Long userId) {
        UserKgMetadata meta = getOrCreateMetadata(userId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("userId", meta.getUserId());
        result.put("entityCount", meta.getEntityCount());
        result.put("status", meta.getStatus());
        result.put("taskId", meta.getTaskId());
        result.put("lastBuildTime", meta.getLastBuildTime());
        result.put("createdAt", meta.getCreatedAt());
        result.put("updatedAt", meta.getUpdatedAt());
        return result;
    }

    @Override
    public Map<String, Object> getAllUserKgStats() {
        List<UserKgMetadata> allMetas = userKgMetadataMapper.selectList(null);
        List<Map<String, Object>> userStats = new ArrayList<>();

        for (UserKgMetadata meta : allMetas) {
            Map<String, Object> stat = new LinkedHashMap<>();
            stat.put("userId", meta.getUserId());
            stat.put("entityCount", meta.getEntityCount());
            stat.put("status", meta.getStatus());
            stat.put("lastBuildTime", meta.getLastBuildTime());
            userStats.add(stat);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalUsers", allMetas.size());
        result.put("users", userStats);
        return result;
    }

    // ==================== 辅助方法 ====================

    /**
     * 统计用户在 Neo4j 中的实体数量
     */
    private long countUserEntities(Long userId) {
        long count = 0;
        String[] nodeTypes = {"Book", "Author", "Publisher", "Tag"};
        for (String type : nodeTypes) {
            Collection<Map<String, Object>> result = neo4jClient.query(
                    "MATCH (n:" + type + " {userId: $userId}) RETURN count(n) AS count")
                    .bind(userId).to("userId")
                    .fetch().all();
            if (!result.isEmpty()) {
                count += ((Number) result.iterator().next().get("count")).longValue();
            }
        }
        return count;
    }

    /**
     * 获取或创建用户 KG 元数据
     */
    private UserKgMetadata getOrCreateMetadata(Long userId) {
        LambdaQueryWrapper<UserKgMetadata> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserKgMetadata::getUserId, userId);
        UserKgMetadata meta = userKgMetadataMapper.selectOne(wrapper);
        if (meta == null) {
            meta = new UserKgMetadata();
            meta.setUserId(userId);
            meta.setEntityCount(0);
            meta.setStatus("idle");
            meta.setCreatedAt(LocalDateTime.now());
            meta.setUpdatedAt(LocalDateTime.now());
            userKgMetadataMapper.insert(meta);
        }
        return meta;
    }

    /**
     * 更新用户 metadata 状态
     */
    private void updateMetadataStatus(Long userId, String status, String taskId) {
        UserKgMetadata meta = getOrCreateMetadata(userId);
        meta.setStatus(status);
        meta.setTaskId(taskId);
        meta.setUpdatedAt(LocalDateTime.now());
        userKgMetadataMapper.updateById(meta);
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }
}
