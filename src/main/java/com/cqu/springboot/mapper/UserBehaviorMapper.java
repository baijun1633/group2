package com.cqu.springboot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cqu.springboot.entity.UserBehavior;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户行为记录 Mapper
 */
@Mapper
public interface UserBehaviorMapper extends BaseMapper<UserBehavior> {
}
