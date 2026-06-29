package com.cqu.springboot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cqu.springboot.common.BusinessException;
import com.cqu.springboot.common.ErrorCode;
import com.cqu.springboot.entity.Books;
import com.cqu.springboot.entity.ReviewLikes;
import com.cqu.springboot.entity.ReviewReplies;
import com.cqu.springboot.entity.Reviews;
import com.cqu.springboot.mapper.BooksMapper;
import com.cqu.springboot.mapper.ReviewLikesMapper;
import com.cqu.springboot.mapper.ReviewRepliesMapper;
import com.cqu.springboot.mapper.ReviewsMapper;
import com.cqu.springboot.service.ReviewsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 书评服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewsServiceImpl implements ReviewsService {

    private final ReviewsMapper reviewsMapper;
    private final ReviewRepliesMapper reviewRepliesMapper;
    private final ReviewLikesMapper reviewLikesMapper;
    private final BooksMapper booksMapper;

    @Override
    @Transactional
    public Long createReview(Long userId, Long bookId, String content, String markdown) {
        // 验证书籍是否存在
        Books book = booksMapper.selectById(bookId);
        if (book == null) {
            throw new BusinessException(ErrorCode.BOOK_NOT_FOUND);
        }

        // 校验内容非空
        if (!StringUtils.hasText(content)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "书评内容不能为空");
        }

        Reviews review = new Reviews();
        review.setBookId(bookId);
        review.setUserId(userId);
        review.setContent(content);
        review.setMarkdown(markdown);
        review.setStatus((byte) 0); // 待审核
        review.setLikesCount(0);
        review.setRepliesCount(0);
        review.setCreateTime(LocalDateTime.now());
        review.setUpdateTime(LocalDateTime.now());
        reviewsMapper.insert(review);

        return review.getReviewId();
    }

    @Override
    public Map<String, Object> getBookReviews(Long bookId, String sortBy, int page, int size) {
        // 验证书籍是否存在
        Books book = booksMapper.selectById(bookId);
        if (book == null) {
            throw new BusinessException(ErrorCode.BOOK_NOT_FOUND);
        }

        // 排序字段白名单校验（防SQL注入）
        String sortColumn = switch (sortBy == null ? "likes" : sortBy) {
            case "likes" -> "likes_count";
            case "time" -> "create_time";
            default -> "likes_count";
        };
        String order = "DESC";

        int offset = (page - 1) * size;
        List<Map<String, Object>> records = reviewsMapper.selectReviewsByBookId(bookId, sortColumn, order, offset, size);
        long total = reviewsMapper.countReviewsByBookId(bookId);

        Map<String, Object> result = new HashMap<>();
        result.put("records", records);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        return result;
    }

    @Override
    @Transactional
    public void deleteReview(Long userId, Long reviewId) {
        Reviews review = reviewsMapper.selectById(reviewId);
        if (review == null || review.getStatus() == 3) {
            throw new BusinessException(ErrorCode.REVIEW_NOT_FOUND);
        }

        // 只能删除自己的书评
        if (!review.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED);
        }

        // 软删除：状态改为3
        review.setStatus((byte) 3);
        review.setUpdateTime(LocalDateTime.now());
        reviewsMapper.updateById(review);
    }

    @Override
    @Transactional
    public boolean toggleLike(Long userId, Long reviewId) {
        Reviews review = reviewsMapper.selectById(reviewId);
        if (review == null || review.getStatus() != 1) {
            throw new BusinessException(ErrorCode.REVIEW_NOT_FOUND);
        }

        // 查询是否已点赞
        LambdaQueryWrapper<ReviewLikes> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReviewLikes::getUserId, userId)
                .eq(ReviewLikes::getReviewId, reviewId);
        ReviewLikes existing = reviewLikesMapper.selectOne(wrapper);

        if (existing != null) {
            // 已点赞，取消点赞
            reviewLikesMapper.deleteById(existing.getId());
            review.setLikesCount(review.getLikesCount() - 1);
            reviewsMapper.updateById(review);
            return false;
        } else {
            // 未点赞，添加点赞
            ReviewLikes like = new ReviewLikes();
            like.setReviewId(reviewId);
            like.setUserId(userId);
            like.setCreateTime(LocalDateTime.now());
            reviewLikesMapper.insert(like);
            review.setLikesCount(review.getLikesCount() + 1);
            reviewsMapper.updateById(review);
            return true;
        }
    }

    @Override
    @Transactional
    public Long replyReview(Long userId, Long reviewId, String content) {
        Reviews review = reviewsMapper.selectById(reviewId);
        if (review == null || review.getStatus() != 1) {
            throw new BusinessException(ErrorCode.REVIEW_NOT_FOUND);
        }

        if (!StringUtils.hasText(content)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "回复内容不能为空");
        }

        ReviewReplies reply = new ReviewReplies();
        reply.setReviewId(reviewId);
        reply.setUserId(userId);
        reply.setContent(content);
        reply.setCreateTime(LocalDateTime.now());
        reviewRepliesMapper.insert(reply);

        // 更新回复数
        review.setRepliesCount(review.getRepliesCount() + 1);
        review.setUpdateTime(LocalDateTime.now());
        reviewsMapper.updateById(review);

        return reply.getReplyId();
    }

    @Override
    public List<Map<String, Object>> getReviewReplies(Long reviewId) {
        Reviews review = reviewsMapper.selectById(reviewId);
        if (review == null) {
            throw new BusinessException(ErrorCode.REVIEW_NOT_FOUND);
        }
        return reviewRepliesMapper.selectRepliesByReviewId(reviewId);
    }

    @Override
    public Map<String, Object> getPendingReviews(int page, int size) {
        int offset = (page - 1) * size;
        List<Map<String, Object>> records = reviewsMapper.selectPendingReviews(offset, size);
        long total = reviewsMapper.countPendingReviews();

        Map<String, Object> result = new HashMap<>();
        result.put("records", records);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        return result;
    }

    @Override
    @Transactional
    public void auditReview(Long reviewId, String action) {
        Reviews review = reviewsMapper.selectById(reviewId);
        if (review == null || review.getStatus() == 3) {
            throw new BusinessException(ErrorCode.REVIEW_NOT_FOUND);
        }

        // 只有待审核(0)状态的书评可以审核
        if (review.getStatus() != 0) {
            throw new BusinessException(ErrorCode.REVIEW_AUDITED);
        }

        byte newStatus = switch (action) {
            case "approve" -> 1;  // 已通过
            case "reject" -> 2;   // 已拒绝
            case "delete" -> 3;   // 已删除
            default -> throw new BusinessException(ErrorCode.PARAM_ERROR, "审核操作必须是 approve/reject/delete");
        };

        review.setStatus(newStatus);
        review.setUpdateTime(LocalDateTime.now());
        reviewsMapper.updateById(review);
    }

    @Override
    @Transactional
    public void markFeatured(Long reviewId, boolean featured) {
        Reviews review = reviewsMapper.selectById(reviewId);
        if (review == null || review.getStatus() == 3) {
            throw new BusinessException(ErrorCode.REVIEW_NOT_FOUND);
        }
        // 只有已通过的书评可以标记优质
        if (review.getStatus() != 1) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "只有已通过审核的书评才能标记为优质");
        }
        review.setIsFeatured((byte) (featured ? 1 : 0));
        review.setUpdateTime(LocalDateTime.now());
        reviewsMapper.updateById(review);
        log.info("书评 {} 标记优质状态: {}", reviewId, featured);
        // TODO: 同步至 Python 推荐引擎（当前纯 Java 实现，预留接口）
    }

    @Override
    public Map<String, Object> getFeaturedReviews(int page, int size) {
        int offset = (page - 1) * size;
        LambdaQueryWrapper<Reviews> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Reviews::getIsFeatured, 1)
               .eq(Reviews::getStatus, 1)
               .orderByDesc(Reviews::getUpdateTime);
        wrapper.last("LIMIT " + offset + ", " + size);
        List<Reviews> records = reviewsMapper.selectList(wrapper);

        // 统计总数
        LambdaQueryWrapper<Reviews> countWrapper = new LambdaQueryWrapper<>();
        countWrapper.eq(Reviews::getIsFeatured, 1).eq(Reviews::getStatus, 1);
        long total = reviewsMapper.selectCount(countWrapper);

        Map<String, Object> result = new HashMap<>();
        result.put("records", records);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        return result;
    }

    @Override
    @Transactional
    public int batchDeleteReviews(List<Long> reviewIds) {
        if (reviewIds == null || reviewIds.isEmpty()) {
            return 0;
        }
        int deleted = 0;
        for (Long reviewId : reviewIds) {
            Reviews review = reviewsMapper.selectById(reviewId);
            if (review != null && review.getStatus() != 3) {
                review.setStatus((byte) 3);
                review.setUpdateTime(LocalDateTime.now());
                reviewsMapper.updateById(review);
                deleted++;
            }
        }
        log.info("批量软删除书评: 请求 {} 条, 实际删除 {} 条", reviewIds.size(), deleted);
        return deleted;
    }

    @Override
    public Map<String, Object> searchReviews(String keyword, Byte status, int page, int size) {
        int offset = (page - 1) * size;
        LambdaQueryWrapper<Reviews> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(Reviews::getStatus, status);
        } else {
            // 排除已删除的
            wrapper.ne(Reviews::getStatus, 3);
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(Reviews::getContent, keyword)
                             .or()
                             .like(Reviews::getMarkdown, keyword));
        }
        wrapper.orderByDesc(Reviews::getCreateTime);
        wrapper.last("LIMIT " + offset + ", " + size);
        List<Reviews> records = reviewsMapper.selectList(wrapper);

        // 统计总数
        LambdaQueryWrapper<Reviews> countWrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            countWrapper.eq(Reviews::getStatus, status);
        } else {
            countWrapper.ne(Reviews::getStatus, 3);
        }
        if (keyword != null && !keyword.isBlank()) {
            countWrapper.and(w -> w.like(Reviews::getContent, keyword)
                                   .or()
                                   .like(Reviews::getMarkdown, keyword));
        }
        long total = reviewsMapper.selectCount(countWrapper);

        Map<String, Object> result = new HashMap<>();
        result.put("records", records);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        return result;
    }
}
