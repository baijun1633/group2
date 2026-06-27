package com.cqu.springboot.service;

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
}
