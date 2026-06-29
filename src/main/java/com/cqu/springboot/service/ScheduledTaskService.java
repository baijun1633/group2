package com.cqu.springboot.service;

import com.cqu.springboot.entity.KgSyncLog;

import java.util.List;

/**
 * 定时任务服务
 */
public interface ScheduledTaskService {

    /**
     * 离线计算图书相似度矩阵（全量重算）
     * <p>基于 book_ratings + shelf_books 构建用户-物品矩阵，计算余弦相似度</p>
     *
     * @return 计算得到的相似度对数
     */
    int computeSimilarityMatrix();

    /**
     * 导出知识图谱数据供 Python 推荐引擎消费
     * <p>从 Neo4j 导出节点/关系到 MySQL 表或 JSON 文件</p>
     *
     * @return 同步的记录数
     */
    int syncKgData();

    /**
     * 查询同步日志列表（按时间倒序）
     *
     * @param limit 返回条数，默认20
     * @return 日志列表
     */
    List<KgSyncLog> getSyncLogs(int limit);
}
