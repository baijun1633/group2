package com.cqu.springboot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cqu.springboot.common.BusinessException;
import com.cqu.springboot.common.ErrorCode;
import com.cqu.springboot.dto.KgBuildRequest;
import com.cqu.springboot.dto.KgQueryRequest;
import com.cqu.springboot.dto.KgRelationRequest;
import com.cqu.springboot.entity.Books;
import com.cqu.springboot.entity.Categories;
import com.cqu.springboot.entity.kg.GraphData;
import com.cqu.springboot.mapper.BooksMapper;
import com.cqu.springboot.mapper.CategoriesMapper;
import com.cqu.springboot.service.KnowledgeGraphService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
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
    private final ObjectMapper objectMapper;

    /** 构建任务状态存储（内存，重启后丢失） */
    private final ConcurrentHashMap<String, Map<String, Object>> buildTasks = new ConcurrentHashMap<>();

    // ==================== 图谱构建 ====================

    @Override
    public Map<String, Object> buildGraph(KgBuildRequest request) {
        String taskId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        long bookCount;
        if (request.getBookIds() != null && !request.getBookIds().isEmpty()) {
            bookCount = request.getBookIds().size();
        } else {
            bookCount = booksMapper.selectCount(null);
        }

        Map<String, Object> taskInfo = new HashMap<>();
        taskInfo.put("status", "RUNNING");
        taskInfo.put("message", "构建任务已启动");
        taskInfo.put("startTime", System.currentTimeMillis());
        taskInfo.put("bookCount", bookCount);
        taskInfo.put("processedCount", 0);
        buildTasks.put(taskId, taskInfo);

        CompletableFuture.runAsync(() -> {
            try {
                doBuildGraph(request, taskId);
            } catch (Exception e) {
                log.error("图谱构建失败", e);
                Map<String, Object> info = buildTasks.get(taskId);
                if (info != null) {
                    info.put("status", "FAILED");
                    info.put("message", "构建失败: " + e.getMessage());
                    info.put("endTime", System.currentTimeMillis());
                }
            }
        });

        Map<String, Object> result = new HashMap<>();
        result.put("taskId", taskId);
        result.put("estimatedTimeMs", Math.max(500, bookCount * 50));
        result.put("bookCount", bookCount);
        return result;
    }

    private void doBuildGraph(KgBuildRequest request, String taskId) {
        // 全量重建时先清空
        if (Boolean.TRUE.equals(request.getForceRebuild())) {
            log.info("全量重建：清空已有图谱数据");
            neo4jClient.query("MATCH (n) DETACH DELETE n").run();
        }

        // 1. 创建所有 Category 节点
        List<Categories> categories = categoriesMapper.selectList(null);
        for (Categories cat : categories) {
            neo4jClient.query(
                    "MERGE (c:Category {categoryId: $categoryId}) SET c.name = $name")
                    .bind(cat.getCategoryId()).to("categoryId")
                    .bind(cat.getName()).to("name")
                    .run();
        }
        log.info("创建 {} 个 Category 节点", categories.size());

        // 2. 查询书籍
        List<Books> books;
        if (request.getBookIds() != null && !request.getBookIds().isEmpty()) {
            books = booksMapper.selectList(
                    new QueryWrapper<Books>().in("book_id", request.getBookIds()));
        } else {
            books = booksMapper.selectList(null);
        }

        // 3. 为每本书创建节点和关系
        int processed = 0;
        for (Books book : books) {
            try {
                createBookNodeAndRelations(book);
                processed++;
                Map<String, Object> info = buildTasks.get(taskId);
                if (info != null) {
                    info.put("processedCount", processed);
                }
            } catch (Exception e) {
                log.error("创建书籍图谱节点失败, bookId={}", book.getBookId(), e);
            }
        }

        Map<String, Object> info = buildTasks.get(taskId);
        if (info != null) {
            info.put("status", "COMPLETED");
            info.put("message", "构建完成");
            info.put("endTime", System.currentTimeMillis());
            info.put("processedCount", processed);
        }
        log.info("图谱构建完成，共处理 {} 本书", processed);
    }

    private void createBookNodeAndRelations(Books book) {
        Double avgRating = book.getAvgRating() != null ? book.getAvgRating().doubleValue() : null;

        // 创建 Book 节点
        neo4jClient.query(
                "MERGE (b:Book {bookId: $bookId}) "
                        + "SET b.title = $title, b.isbn = $isbn, b.description = $description, "
                        + "b.avgRating = $avgRating, b.coverImage = $coverImage")
                .bind(book.getBookId()).to("bookId")
                .bind(book.getTitle()).to("title")
                .bind(book.getIsbn()).to("isbn")
                .bind(book.getDescription()).to("description")
                .bind(avgRating).to("avgRating")
                .bind(book.getCoverImage()).to("coverImage")
                .run();

        // Author 节点 + WRITTEN_BY 关系
        if (book.getAuthor() != null && !book.getAuthor().isBlank()) {
            neo4jClient.query(
                    "MATCH (b:Book {bookId: $bookId}) "
                            + "MERGE (a:Author {name: $authorName}) "
                            + "MERGE (b)-[:WRITTEN_BY]->(a)")
                    .bind(book.getBookId()).to("bookId")
                    .bind(book.getAuthor()).to("authorName")
                    .run();
        }

        // Publisher 节点 + PUBLISHED_BY 关系
        if (book.getPublisher() != null && !book.getPublisher().isBlank()) {
            neo4jClient.query(
                    "MATCH (b:Book {bookId: $bookId}) "
                            + "MERGE (p:Publisher {name: $publisherName}) "
                            + "MERGE (b)-[:PUBLISHED_BY]->(p)")
                    .bind(book.getBookId()).to("bookId")
                    .bind(book.getPublisher()).to("publisherName")
                    .run();
        }

        // Category 节点 + BELONGS_TO 关系
        if (book.getCategoryId() != null) {
            neo4jClient.query(
                    "MATCH (b:Book {bookId: $bookId}) "
                            + "MERGE (c:Category {categoryId: $categoryId}) "
                            + "MERGE (b)-[:BELONGS_TO]->(c)")
                    .bind(book.getBookId()).to("bookId")
                    .bind(book.getCategoryId()).to("categoryId")
                    .run();
        }

        // Tag 节点 + TAGGED_AS 关系
        List<String> tags = parseTags(book.getTags());
        for (String tag : tags) {
            if (tag != null && !tag.isBlank()) {
                neo4jClient.query(
                        "MATCH (b:Book {bookId: $bookId}) "
                                + "MERGE (t:Tag {name: $tagName}) "
                                + "MERGE (b)-[:TAGGED_AS]->(t)")
                        .bind(book.getBookId()).to("bookId")
                        .bind(tag.trim()).to("tagName")
                        .run();
            }
        }
    }

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

    // ==================== Cypher 查询 ====================

    @Override
    public List<Map<String, Object>> query(KgQueryRequest request) {
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

        Collection<Map<String, Object>> result = neo4jClient.query(finalCypher).fetch().all();
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
    public GraphData getGraph(String entityType, String entityId, Integer depth) {
        if (depth == null || depth < 1) {
            depth = 2;
        }
        if (depth > 5) {
            depth = 5;
        }

        String label = capitalize(entityType);
        String matchProperty;
        Object matchValue;
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

        // 查询所有相关节点
        String nodeCypher = String.format(
                "MATCH (n:%s {%s: $entityId})-[*0..%d]-(m) "
                        + "RETURN DISTINCT id(m) AS nodeId, labels(m) AS labels, properties(m) AS props",
                label, matchProperty, depth);
        Collection<Map<String, Object>> nodeResults = neo4jClient.query(nodeCypher)
                .bind(matchValue).to("entityId")
                .fetch().all();

        // 查询所有相关关系
        String relCypher = String.format(
                "MATCH (n:%s {%s: $entityId})-[*0..%d]-(m) "
                        + "WITH collect(DISTINCT m) AS nodes "
                        + "UNWIND nodes AS node "
                        + "MATCH (node)-[r]-(other) "
                        + "WHERE other IN nodes "
                        + "RETURN DISTINCT id(startNode(r)) AS sourceId, id(endNode(r)) AS targetId, type(r) AS relType",
                label, matchProperty, depth);
        Collection<Map<String, Object>> relResults = neo4jClient.query(relCypher)
                .bind(matchValue).to("entityId")
                .fetch().all();

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

    // ==================== 关系编辑 ====================

    @Override
    public void editRelation(KgRelationRequest request) {
        String action = request.getAction();
        if (action == null || (!action.equals("add") && !action.equals("delete"))) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "action必须为add或delete");
        }

        String relType = request.getRelationType();
        if (relType == null || !relType.matches("^[A-Z_]+$")) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "关系类型只能包含大写字母和下划线");
        }

        String sourceLabel = capitalize(request.getSourceType());
        String targetLabel = capitalize(request.getTargetType());
        String sourceProp = determineIdProperty(sourceLabel);
        String targetProp = determineIdProperty(targetLabel);
        Object sourceId = convertId(sourceLabel, request.getSourceId());
        Object targetId = convertId(targetLabel, request.getTargetId());

        if ("add".equals(action)) {
            String cypher = String.format(
                    "MATCH (s:%s {%s: $sourceId}), (t:%s {%s: $targetId}) MERGE (s)-[:%s]->(t)",
                    sourceLabel, sourceProp, targetLabel, targetProp, relType);
            neo4jClient.query(cypher)
                    .bind(sourceId).to("sourceId")
                    .bind(targetId).to("targetId")
                    .run();
        } else {
            String cypher = String.format(
                    "MATCH (s:%s {%s: $sourceId})-[r:%s]->(t:%s {%s: $targetId}) DELETE r",
                    sourceLabel, sourceProp, relType, targetLabel, targetProp);
            neo4jClient.query(cypher)
                    .bind(sourceId).to("sourceId")
                    .bind(targetId).to("targetId")
                    .run();
        }
    }

    @Override
    public Map<String, Object> batchImportRelations(List<Map<String, String>> rows) {
        int success = 0;
        int failed = 0;
        List<Map<String, Object>> errors = new ArrayList<>();

        for (int i = 0; i < rows.size(); i++) {
            Map<String, String> row = rows.get(i);
            int rowNum = i + 2; // +2: 表头1行 + 索引从0开始
            try {
                String sourceType = row.get("sourceType");
                String sourceId = row.get("sourceId");
                String targetType = row.get("targetType");
                String targetId = row.get("targetId");
                String relationType = row.get("relationType");
                String action = row.getOrDefault("action", "add");

                if (sourceType == null || sourceType.isBlank()
                        || sourceId == null || sourceId.isBlank()
                        || targetType == null || targetType.isBlank()
                        || targetId == null || targetId.isBlank()
                        || relationType == null || relationType.isBlank()) {
                    throw new IllegalArgumentException("sourceType/sourceId/targetType/targetId/relationType 均不能为空");
                }

                KgRelationRequest req = new KgRelationRequest();
                req.setAction(action);
                req.setSourceType(sourceType);
                req.setSourceId(sourceId);
                req.setTargetType(targetType);
                req.setTargetId(targetId);
                req.setRelationType(relationType);
                editRelation(req);
                success++;
            } catch (Exception e) {
                failed++;
                Map<String, Object> err = new LinkedHashMap<>();
                err.put("row", rowNum);
                err.put("message", e.getMessage());
                errors.add(err);
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", rows.size());
        result.put("success", success);
        result.put("failed", failed);
        result.put("errors", errors);
        return result;
    }

    @Override
    public List<Map<String, Object>> executeCypher(String cypher, Map<String, Object> params, Integer limit) {
        if (cypher == null || cypher.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Cypher语句不能为空");
        }
        // 限制最大返回行数（防止意外全表扫描）
        // 注意：写操作（MERGE/CREATE/DELETE/SET）不允许以 LIMIT 结尾
        int maxRows = limit != null ? Math.min(limit, 1000) : 1000;
        String upper = cypher.toUpperCase();
        String finalCypher = cypher;
        if (upper.contains("RETURN") && !upper.contains("LIMIT")) {
            finalCypher = cypher + " LIMIT " + maxRows;
        }

        // RunnableSpec extends BindSpec<RunnableSpec>，bindAll 返回 RunnableSpec
        // UnboundRunnableSpec extends RunnableSpec，向上转型后调用 bindAll 安全
        Neo4jClient.RunnableSpec spec = neo4jClient.query(finalCypher);
        if (params != null && !params.isEmpty()) {
            spec = spec.bindAll(params);
        }
        Collection<Map<String, Object>> result = spec.fetch().all();
        return new ArrayList<>(result);
    }

    private String determineIdProperty(String label) {
        switch (label) {
            case "Book": return "bookId";
            case "Category": return "categoryId";
            default: return "name";
        }
    }

    private Object convertId(String label, String id) {
        switch (label) {
            case "Book":
            case "Category":
                return Long.parseLong(id);
            default:
                return id;
        }
    }

    // ==================== 图谱实体 CRUD（P3） ====================

    /** 实体类型 → Neo4j 标签 映射 */
    private static final Map<String, String> ENTITY_LABELS = Map.of(
            "author", "Author",
            "publisher", "Publisher",
            "tag", "Tag",
            "series", "Series"
    );

    /** 校验实体类型并返回 Neo4j 标签名 */
    private String resolveEntityLabel(String type) {
        if (type == null || type.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "实体类型不能为空");
        }
        String label = ENTITY_LABELS.get(type.toLowerCase());
        if (label == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "不支持的实体类型: " + type + "，支持: author/publisher/tag/series");
        }
        return label;
    }

    @Override
    public Map<String, Object> createEntity(String type, String name) {
        String label = resolveEntityLabel(type);
        if (name == null || name.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "实体名称不能为空");
        }
        name = name.trim();

        // MERGE 幂等：已存在则不报错，返回 existed=true
        Collection<Map<String, Object>> existing = neo4jClient.query(
                "MATCH (n:" + label + " {name: $name}) RETURN n.name AS name LIMIT 1")
                .bind(name).to("name")
                .fetch().all();
        boolean existed = !existing.isEmpty();
        if (!existed) {
            neo4jClient.query("MERGE (n:" + label + " {name: $name})")
                    .bind(name).to("name")
                    .run();
            log.info("创建图谱实体: {}:{}", label, name);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("type", type.toLowerCase());
        result.put("name", name);
        result.put("existed", existed);
        return result;
    }

    @Override
    public void deleteEntity(String type, String name) {
        String label = resolveEntityLabel(type);
        if (name == null || name.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "实体名称不能为空");
        }

        // 先检查是否存在
        Collection<Map<String, Object>> existing = neo4jClient.query(
                "MATCH (n:" + label + " {name: $name}) RETURN n.name AS name LIMIT 1")
                .bind(name).to("name")
                .fetch().all();
        if (existing.isEmpty()) {
            throw new BusinessException(ErrorCode.KG_NODE_NOT_FOUND,
                    "实体不存在: " + type + "/" + name);
        }

        // DETACH DELETE 同时删除节点和关系
        neo4jClient.query("MATCH (n:" + label + " {name: $name}) DETACH DELETE n")
                .bind(name).to("name")
                .run();
        log.info("删除图谱实体（含关系）: {}:{}", label, name);
    }

    @Override
    public Map<String, Object> listEntities(String type, int page, int size) {
        String label = resolveEntityLabel(type);
        if (page < 1) page = 1;
        if (size < 1 || size > 100) size = 20;
        int skip = (page - 1) * size;

        // 查询总数
        Collection<Map<String, Object>> countResult = neo4jClient.query(
                "MATCH (n:" + label + ") RETURN count(n) AS cnt")
                .fetch().all();
        long total = countResult.isEmpty() ? 0
                : ((Number) countResult.iterator().next().get("cnt")).longValue();

        // 分页查询 + 关联图书数
        Collection<Map<String, Object>> rows = neo4jClient.query(
                "MATCH (n:" + label + ") "
                        + "OPTIONAL MATCH (n)<-[r]-(b:Book) "
                        + "RETURN n.name AS name, count(DISTINCT b) AS bookCount "
                        + "ORDER BY n.name SKIP $skip LIMIT $limit")
                .bind(skip).to("skip")
                .bind(size).to("limit")
                .fetch().all();

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
    public Map<String, Object> getEntity(String type, String name) {
        String label = resolveEntityLabel(type);
        if (name == null || name.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "实体名称不能为空");
        }

        Collection<Map<String, Object>> rows = neo4jClient.query(
                "MATCH (n:" + label + " {name: $name}) "
                        + "OPTIONAL MATCH (n)<-[r]-(b:Book) "
                        + "RETURN n.name AS name, "
                        + "collect(DISTINCT {bookId: b.bookId, title: b.title}) AS books")
                .bind(name).to("name")
                .fetch().all();

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
    public void renameEntity(String type, String oldName, String newName) {
        String label = resolveEntityLabel(type);
        if (oldName == null || oldName.isBlank() || newName == null || newName.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "旧名称和新名称均不能为空");
        }
        if (oldName.equals(newName)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "新名称与旧名称相同");
        }

        // 检查旧实体存在
        Collection<Map<String, Object>> existing = neo4jClient.query(
                "MATCH (n:" + label + " {name: $oldName}) RETURN n.name AS name LIMIT 1")
                .bind(oldName).to("oldName")
                .fetch().all();
        if (existing.isEmpty()) {
            throw new BusinessException(ErrorCode.KG_NODE_NOT_FOUND,
                    "实体不存在: " + type + "/" + oldName);
        }

        // 检查新名称是否已存在
        Collection<Map<String, Object>> conflict = neo4jClient.query(
                "MATCH (n:" + label + " {name: $newName}) RETURN n.name AS name LIMIT 1")
                .bind(newName).to("newName")
                .fetch().all();
        if (!conflict.isEmpty()) {
            throw new BusinessException(ErrorCode.KG_RELATION_EXISTS,
                    "目标名称已存在: " + type + "/" + newName);
        }

        // 重命名：直接 SET name 属性即可保留所有关系（Neo4j 节点属性可变）
        // 生产环境若需保留 @Id 唯一性约束，推荐 APOC 的 apoc.refactor.renameNode
        neo4jClient.query(
                "MATCH (n:" + label + " {name: $oldName}) SET n.name = $newName")
                .bind(oldName).to("oldName")
                .bind(newName).to("newName")
                .run();

        log.info("重命名图谱实体: {}:{} -> {}:{}", label, oldName, label, newName);
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
    public Map<String, Object> getGraphStats() {
        Map<String, Object> stats = new HashMap<>();
        String[] nodeTypes = {"Book", "Author", "Publisher", "Category", "Tag"};
        for (String type : nodeTypes) {
            Collection<Map<String, Object>> result = neo4jClient.query(
                    "MATCH (n:" + type + ") RETURN count(n) AS count").fetch().all();
            if (!result.isEmpty()) {
                stats.put(type + "Count", result.iterator().next().get("count"));
            }
        }
        String[] relTypes = {"WRITTEN_BY", "PUBLISHED_BY", "BELONGS_TO", "TAGGED_AS"};
        for (String type : relTypes) {
            Collection<Map<String, Object>> result = neo4jClient.query(
                    "MATCH ()-[r:" + type + "]->() RETURN count(r) AS count").fetch().all();
            if (!result.isEmpty()) {
                stats.put(type + "Count", result.iterator().next().get("count"));
            }
        }
        return stats;
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }
}
