package com.cqu.springboot.mapper;

import com.cqu.springboot.entity.ReviewReplies;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 书评回复表 Mapper 接口
 */
public interface ReviewRepliesMapper extends BaseMapper<ReviewReplies> {

    /**
     * 查询某条书评的回复列表（带用户信息）
     */
    @Select("""
            SELECT rp.reply_id, rp.review_id, rp.user_id, rp.content, rp.create_time,
                   u.username, u.nickname, u.avatar
            FROM review_replies rp
            LEFT JOIN users u ON rp.user_id = u.user_id
            WHERE rp.review_id = #{reviewId}
            ORDER BY rp.create_time ASC
            """)
    List<Map<String, Object>> selectRepliesByReviewId(@Param("reviewId") Long reviewId);
}
