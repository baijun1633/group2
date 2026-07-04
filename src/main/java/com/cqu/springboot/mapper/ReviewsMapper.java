package com.cqu.springboot.mapper;

import com.cqu.springboot.entity.Reviews;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 书评表 Mapper 接口
 */
public interface ReviewsMapper extends BaseMapper<Reviews> {

    /**
     * 查询某本书的书评列表（带用户信息）
     */
    @Select("""
            SELECT r.review_id, r.book_id, r.user_id, r.content, r.markdown,
                   r.status, r.likes_count, r.replies_count, r.create_time, r.update_time,
                   u.username, u.nickname, u.avatar,
                   br.score
            FROM reviews r
            LEFT JOIN users u ON r.user_id = u.user_id
            LEFT JOIN book_ratings br ON r.user_id = br.user_id AND r.book_id = br.book_id
            WHERE r.book_id = #{bookId} AND r.status = 1
            ORDER BY ${sortBy} ${order}
            LIMIT #{offset}, #{size}
            """)
    List<Map<String, Object>> selectReviewsByBookId(@Param("bookId") Long bookId,
                                                     @Param("sortBy") String sortBy,
                                                     @Param("order") String order,
                                                     @Param("offset") int offset,
                                                     @Param("size") int size);

    /**
     * 统计某本书的书评数量
     */
    @Select("SELECT COUNT(*) FROM reviews WHERE book_id = #{bookId} AND status = 1")
    long countReviewsByBookId(@Param("bookId") Long bookId);

    /**
     * 查询待审核书评列表（管理员）
     */
    @Select("""
            SELECT r.review_id, r.book_id, r.user_id, r.content, r.markdown,
                   r.status, r.likes_count, r.replies_count, r.create_time, r.update_time,
                   u.username, u.nickname, u.avatar,
                   b.title as book_title
            FROM reviews r
            LEFT JOIN users u ON r.user_id = u.user_id
            LEFT JOIN books b ON r.book_id = b.book_id
            WHERE r.status = 0
            ORDER BY r.create_time ASC
            LIMIT #{offset}, #{size}
            """)
    List<Map<String, Object>> selectPendingReviews(@Param("offset") int offset,
                                                    @Param("size") int size);

    /**
     * 统计待审核书评数量
     */
    @Select("SELECT COUNT(*) FROM reviews WHERE status = 0")
    long countPendingReviews();
}
