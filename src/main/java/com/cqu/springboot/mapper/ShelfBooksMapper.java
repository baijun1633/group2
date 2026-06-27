package com.cqu.springboot.mapper;

import com.cqu.springboot.entity.ShelfBooks;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 书架图书关联表 Mapper 接口
 */
public interface ShelfBooksMapper extends BaseMapper<ShelfBooks> {

    /**
     * 查询书架中的图书数量
     */
    @Select("SELECT COUNT(*) FROM shelf_books WHERE shelf_id = #{shelfId}")
    int countByShelfId(@Param("shelfId") Long shelfId);

    /**
     * 查询用户所有书架的图书数量统计
     */
    @Select("SELECT shelf_id as shelfId, COUNT(*) as bookCount FROM shelf_books WHERE shelf_id IN (SELECT shelf_id FROM shelves WHERE user_id = #{userId}) GROUP BY shelf_id")
    List<Map<String, Object>> countByUserId(@Param("userId") Long userId);
}
