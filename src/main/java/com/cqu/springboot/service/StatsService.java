package com.cqu.springboot.service;

import java.util.List;
import java.util.Map;

/**
 * 系统统计服务
 */
public interface StatsService {

    /**
     * 获取系统统计数据
     *
     * @return 包含总用户、图书、书评、评分、分类数等统计指标的 Map
     */
    Map<String, Object> getSystemStats();

    /**
     * 用户增长趋势（近30天每日新增用户）
     *
     * @return [{date, count}, ...]
     */
    List<Map<String, Object>> getUserGrowthTrend();

    /**
     * 图书评分分布（各评分段的图书数量）
     *
     * @return [{range, count}, ...]
     */
    List<Map<String, Object>> getRatingDistribution();

    /**
     * 热门图书 Top N（按浏览量排序）
     *
     * @param limit 返回数量
     * @return [{bookId, title, viewCount, avgRating}, ...]
     */
    List<Map<String, Object>> getTopBooks(int limit);

    /**
     * 行为类型分布（各行为类型的数量）
     *
     * @return [{type, count}, ...]
     */
    List<Map<String, Object>> getBehaviorDistribution();
}
