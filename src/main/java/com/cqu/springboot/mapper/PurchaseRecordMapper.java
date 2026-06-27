package com.cqu.springboot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cqu.springboot.entity.PurchaseRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 购买记录 Mapper
 */
@Mapper
public interface PurchaseRecordMapper extends BaseMapper<PurchaseRecord> {
}
