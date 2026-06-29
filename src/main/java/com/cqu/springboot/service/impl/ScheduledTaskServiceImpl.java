package com.cqu.springboot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cqu.springboot.entity.BookRatings;
import com.cqu.springboot.entity.BookSimilarity;
import com.cqu.springboot.entity.KgSyncLog;
import com.cqu.springboot.entity.ShelfBooks;
import com.cqu.springboot.entity.Shelves;
import com.cqu.springboot.mapper.BookRatingsMapper;
import com.cqu.springboot.mapper.BookSimilarityMapper;
import com.cqu.springboot.mapper.KgSyncLogMapper;
import com.cqu.springboot.mapper.ShelfBooksMapper;
import com.cqu.springboot.mapper.ShelvesMapper;
import com.cqu.springboot.service.ScheduledTaskService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 定时任务服务实现
 * <p>
 * 1. 相似度矩阵计算：每天凌晨 2:00 全量重算
 * 2. 知识图谱数据同步：每天凌晨 3:00 导出供 Python 消费
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledTaskServiceImpl implements ScheduledTaskService {

    private final BookRatingsMapper bookRatingsMapper;
    private final ShelvesMapper shelvesMapper;
    private final ShelfBooksMapper shelfBooksMapper;
    private final BookSimilarityMapper bookSimilarityMapper;
    private final KgSyncLogMapper kgSyncLogMapper;
    private final Neo4jClient neo4jClient;
    private final ObjectMapper objectMapper;

    private static final String KG_EXPORT_DIR = "d:/code/library_sys/tmp/kg_export";

    /**
     * 定时任务：每天凌晨 2:00 计算相似度矩阵
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void scheduledComputeSimilarity() {
        log.info("[定时任务] 开始计算图书相似度矩阵...");
        try {
            int count = computeSimilarityMatrix();
            log.info("[定时任务] 相似度矩阵计算完成，共 {} 对", count);
        } catch (Exception e) {
            log.error("[定时任务] 相似度矩阵计算失败", e);
        }
    }

    /**
     * 定时任务：每天凌晨 3:00 同步知识图谱数据
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void scheduledSyncKgData() {
        log.info("[定时任务] 开始同步知识图谱数据...");
        try {
            int count = syncKgData();
            log.info("[定时任务] 知识图谱数据同步完成，共 {} 条记录", count);
        } catch (Exception e) {
            log.error("[定时任务] 知识图谱数据同步失败", e);
        }
    }

    @Override
    public int computeSimilarityMatrix() {
        long startTime = System.currentTimeMillis();

        // 1. 构建用户-物品评分矩阵
        Map<Long, Map<Long, Double>> userItemMatrix = buildUserItemMatrix();
        if (userItemMatrix.isEmpty()) {
            log.info("无用户行为数据，跳过相似度计算");
            return 0;
        }

        // 2. 构建物品-用户倒排表
        Map<Long, Set<Long>> itemUsers = new HashMap<>();
        for (Map.Entry<Long, Map<Long, Double>> entry : userItemMatrix.entrySet()) {
            for (Long bookId : entry.getValue().keySet()) {
                itemUsers.computeIfAbsent(bookId, k -> new HashSet<>()).add(entry.getKey());
            }
        }

        // 3. 计算余弦相似度
        List<Long> bookIds = new ArrayList<>(itemUsers.keySet());
        List<BookSimilarity> similarities = new ArrayList<>();

        for (int i = 0; i < bookIds.size(); i++) {
            Long bookA = bookIds.get(i);
            Set<Long> usersA = itemUsers.get(bookA);

            for (int j = i + 1; j < bookIds.size(); j++) {
                Long bookB = bookIds.get(j);
                Set<Long> usersB = itemUsers.get(bookB);

                // 找共同用户
                Set<Long> commonUsers = new HashSet<>(usersA);
                commonUsers.retainAll(usersB);

                if (commonUsers.size() < 1) continue; // 至少1个共同用户

                // 余弦相似度
                double dotProduct = 0, normA = 0, normB = 0;
                for (Long userId : commonUsers) {
                    double scoreA = userItemMatrix.get(userId).getOrDefault(bookA, 0.0);
                    double scoreB = userItemMatrix.get(userId).getOrDefault(bookB, 0.0);
                    dotProduct += scoreA * scoreB;
                    normA += scoreA * scoreA;
                    normB += scoreB * scoreB;
                }

                double denominator = Math.sqrt(normA) * Math.sqrt(normB);
                if (denominator == 0) continue;

                double cosineSim = dotProduct / denominator;
                if (cosineSim < 0.01) continue; // 过滤极低相似度

                BookSimilarity sim = new BookSimilarity();
                sim.setBookIdA(Math.min(bookA, bookB)); // 保证 a < b 避免重复
                sim.setBookIdB(Math.max(bookA, bookB));
                sim.setSimilarity(BigDecimal.valueOf(cosineSim).setScale(6, RoundingMode.HALF_UP));
                sim.setCoCount(commonUsers.size());
                sim.setComputeTime(LocalDateTime.now());
                similarities.add(sim);
            }
        }

        // 4. 清空旧数据，插入新数据（分批）
        bookSimilarityMapper.delete(null);
        int batchSize = 500;
        int inserted = 0;
        for (int i = 0; i < similarities.size(); i += batchSize) {
            List<BookSimilarity> batch = similarities.subList(i, Math.min(i + batchSize, similarities.size()));
            for (BookSimilarity sim : batch) {
                bookSimilarityMapper.insert(sim);
                inserted++;
            }
        }

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("相似度矩阵计算完成: 用户数={}, 图书数={}, 相似度对数={}, 耗时={}ms",
                userItemMatrix.size(), bookIds.size(), inserted, elapsed);
        return inserted;
    }

    @Override
    public int syncKgData() {
        LocalDateTime now = LocalDateTime.now();

        // 记录同步日志
        KgSyncLog syncLog = new KgSyncLog();
        syncLog.setSyncType("full");
        syncLog.setStatus("running");
        syncLog.setStartTime(now);
        kgSyncLogMapper.insert(syncLog);

        try {
            File exportDir = new File(KG_EXPORT_DIR);
            if (!exportDir.exists()) {
                exportDir.mkdirs();
            }

            int totalRecords = 0;

            // 导出图书节点
            Collection<Map<String, Object>> books = neo4jClient
                    .query("MATCH (b:Book) RETURN b.bookId AS bookId, b.title AS title, b.isbn AS isbn, "
                            + "b.description AS description, b.avgRating AS avgRating, b.coverImage AS coverImage")
                    .fetch().all();
            writeJson(new File(exportDir, "books.json"), books);
            totalRecords += books.size();

            // 导出作者节点
            Collection<Map<String, Object>> authors = neo4jClient
                    .query("MATCH (a:Author) RETURN a.name AS name")
                    .fetch().all();
            writeJson(new File(exportDir, "authors.json"), authors);
            totalRecords += authors.size();

            // 导出分类节点
            Collection<Map<String, Object>> categories = neo4jClient
                    .query("MATCH (c:Category) RETURN c.categoryId AS categoryId, c.name AS name")
                    .fetch().all();
            writeJson(new File(exportDir, "categories.json"), categories);
            totalRecords += categories.size();

            // 导出出版社节点
            Collection<Map<String, Object>> publishers = neo4jClient
                    .query("MATCH (p:Publisher) RETURN p.name AS name")
                    .fetch().all();
            writeJson(new File(exportDir, "publishers.json"), publishers);
            totalRecords += publishers.size();

            // 导出标签节点
            Collection<Map<String, Object>> tags = neo4jClient
                    .query("MATCH (t:Tag) RETURN t.name AS name")
                    .fetch().all();
            writeJson(new File(exportDir, "tags.json"), tags);
            totalRecords += tags.size();

            // 更新日志
            syncLog.setStatus("success");
            syncLog.setRecordsSynced(totalRecords);
            syncLog.setEndTime(LocalDateTime.now());
            kgSyncLogMapper.updateById(syncLog);

            log.info("知识图谱数据同步完成: 图书={}, 作者={}, 分类={}, 出版社={}, 标签={}",
                    books.size(), authors.size(), categories.size(), publishers.size(), tags.size());
            return totalRecords;

        } catch (Exception e) {
            syncLog.setStatus("failed");
            syncLog.setErrorMessage(e.getMessage());
            syncLog.setEndTime(LocalDateTime.now());
            kgSyncLogMapper.updateById(syncLog);
            log.error("知识图谱数据同步失败", e);
            throw new RuntimeException("同步失败: " + e.getMessage(), e);
        }
    }

    @Override
    public List<KgSyncLog> getSyncLogs(int limit) {
        QueryWrapper<KgSyncLog> wrapper = new QueryWrapper<>();
        wrapper.orderByDesc("start_time").last("LIMIT " + limit);
        return kgSyncLogMapper.selectList(wrapper);
    }

    // ============ 私有方法 ============

    /**
     * 构建用户-物品评分矩阵（显式评分 + 隐式反馈）
     */
    private Map<Long, Map<Long, Double>> buildUserItemMatrix() {
        Map<Long, Map<Long, Double>> matrix = new HashMap<>();

        // 显式评分
        List<BookRatings> allRatings = bookRatingsMapper.selectList(null);
        for (BookRatings r : allRatings) {
            double score = r.getScore() != null ? r.getScore().doubleValue() : 3.0;
            matrix.computeIfAbsent(r.getUserId(), k -> new HashMap<>())
                  .merge(r.getBookId(), score, Double::max);
        }

        // 隐式反馈（书架）
        List<Shelves> allShelves = shelvesMapper.selectList(null);
        for (Shelves shelf : allShelves) {
            QueryWrapper<ShelfBooks> sbWrapper = new QueryWrapper<>();
            sbWrapper.eq("shelf_id", shelf.getShelfId());
            shelfBooksMapper.selectList(sbWrapper).forEach(sb -> {
                double implicitScore = sb.getReadingStatus() != null
                        ? (sb.getReadingStatus() == 2 ? 3.0 : sb.getReadingStatus() == 1 ? 2.0 : 1.0)
                        : 1.0;
                matrix.computeIfAbsent(shelf.getUserId(), k -> new HashMap<>())
                      .merge(sb.getBookId(), implicitScore, Double::max);
            });
        }

        return matrix;
    }

    private void writeJson(File file, Object data) {
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, data);
        } catch (IOException e) {
            log.error("写入 JSON 文件失败: {}", file.getAbsolutePath(), e);
        }
    }
}
