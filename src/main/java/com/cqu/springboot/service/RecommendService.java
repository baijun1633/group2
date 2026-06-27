package com.cqu.springboot.service;

import com.cqu.springboot.dto.RecommendItem;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 混合推荐服务接口（统一入口）
 */
public interface RecommendService {

    /**
     * 首页推荐（混合策略）
     * 登录用户：KG + ItemCF + 热门 + 新书 加权
     * 未登录用户：热门 + 新书 兜底
     *
     * @param userId 用户ID（null表示未登录）
     * @param limit  返回数量
     * @return 推荐列表
     */
    List<RecommendItem> getHomeRecommend(Long userId, int limit);

    /**
     * 热门图书
     *
     * @param days  统计天数（最近N天的评分/行为）
     * @param limit 返回数量
     * @return 图书列表
     */
    List<RecommendItem> getHotBooks(int days, int limit);

    /**
     * 新书推荐
     *
     * @param months 最近N个月的新书
     * @param limit  返回数量
     * @return 图书列表
     */
    List<RecommendItem> getNewBooks(int months, int limit);

    /**
     * 获取推荐权重配置
     */
    Map<String, Object> getConfig();

    /**
     * 更新推荐权重配置
     *
     * @param weights 权重Map（kg_weight, itemcf_weight, hot_weight, new_weight）
     */
    void updateConfig(Map<String, BigDecimal> weights);

    /**
     * 推荐解释详情
     * <p>
     * 对当前登录用户，解释为什么推荐某本书：
     * - 已读图书：标记为 READ，说明已被过滤
     * - 在推荐列表中：返回合并后的 reason / source / score
     * - 未进入Top列表：标记为 NONE，提示关联度较低
     * </p>
     *
     * @param userId 用户ID（null表示未登录）
     * @param bookId 图书ID
     * @return 推荐解释项（null表示未登录或图书不存在）
     */
    RecommendItem explainRecommendation(Long userId, Long bookId);
}
