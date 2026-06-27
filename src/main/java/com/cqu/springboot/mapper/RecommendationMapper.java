package com.cqu.springboot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cqu.springboot.entity.Recommendation;
import org.apache.ibatis.annotations.Mapper;

/**
 * 推荐记录 Mapper
 */
@Mapper
public interface RecommendationMapper extends BaseMapper<Recommendation> {
}
