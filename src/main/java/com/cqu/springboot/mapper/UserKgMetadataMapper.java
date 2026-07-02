package com.cqu.springboot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cqu.springboot.entity.UserKgMetadata;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户知识图谱元数据 Mapper
 */
@Mapper
public interface UserKgMetadataMapper extends BaseMapper<UserKgMetadata> {
}
