package com.cqu.springboot.mapper;

import com.cqu.springboot.entity.BookRatings;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 图书评分表 Mapper 接口
 */
public interface BookRatingsMapper extends BaseMapper<BookRatings> {

    /**
     * 计算某本书的平均评分
     */
    @Select("SELECT AVG(score) as avg_rating, COUNT(*) as rating_count FROM book_ratings WHERE book_id = #{bookId}")
    Map<String, Object> selectRatingStatsByBookId(@Param("bookId") Long bookId);
}
