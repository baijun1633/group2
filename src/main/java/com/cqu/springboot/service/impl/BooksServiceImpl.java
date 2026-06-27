package com.cqu.springboot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cqu.springboot.common.BusinessException;
import com.cqu.springboot.common.ErrorCode;
import com.cqu.springboot.entity.Books;
import com.cqu.springboot.entity.PurchaseLinks;
import com.cqu.springboot.mapper.BooksMapper;
import com.cqu.springboot.mapper.PurchaseLinksMapper;
import com.cqu.springboot.service.BooksService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 书籍表 服务实现类
 */
@Service
@RequiredArgsConstructor
public class BooksServiceImpl extends ServiceImpl<BooksMapper, Books> implements BooksService {

    private final PurchaseLinksMapper purchaseLinksMapper;
    private final ObjectMapper objectMapper;

    @Override
    public Page<Books> searchBooks(String keyword, int page, int size) {
        Page<Books> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Books> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(keyword)) {
            // 按书名、作者、ISBN 模糊匹配
            wrapper.and(w -> w
                    .like(Books::getTitle, keyword)
                    .or().like(Books::getAuthor, keyword)
                    .or().like(Books::getIsbn, keyword)
            );
        }

        // 只查询上架的书籍
        wrapper.eq(Books::getStatus, (byte) 1);
        wrapper.orderByDesc(Books::getCreateTime);

        return this.baseMapper.selectPage(pageParam, wrapper);
    }

    @Override
    public Page<Books> filterBooks(Long categoryId, String tag, String sortBy, String order, int page, int size) {
        Page<Books> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Books> wrapper = new LambdaQueryWrapper<>();

        // 只查询上架的书籍
        wrapper.eq(Books::getStatus, (byte) 1);

        // 按分类筛选
        if (categoryId != null) {
            wrapper.eq(Books::getCategoryId, categoryId);
        }

        // 按标签筛选（JSON字段模糊匹配）
        if (StringUtils.hasText(tag)) {
            wrapper.like(Books::getTags, tag);
        }

        // 排序
        if ("rating".equals(sortBy)) {
            wrapper.orderByDesc("avg_rating".equals(sortBy) ? Books::getAvgRating : Books::getAvgRating);
        } else if ("publish_date".equals(sortBy)) {
            wrapper.orderByDesc(Books::getPublishDate);
        } else {
            wrapper.orderByDesc(Books::getCreateTime);
        }

        return this.baseMapper.selectPage(pageParam, wrapper);
    }

    @Override
    public Map<String, Object> getBookDetail(Long bookId) {
        Books book = this.baseMapper.selectById(bookId);
        if (book == null) {
            throw new BusinessException(ErrorCode.BOOK_NOT_FOUND);
        }

        Map<String, Object> detail = new HashMap<>();
        detail.put("bookId", book.getBookId());
        detail.put("title", book.getTitle());
        detail.put("author", book.getAuthor());
        detail.put("isbn", book.getIsbn());
        detail.put("publisher", book.getPublisher());
        detail.put("publishDate", book.getPublishDate());
        detail.put("categoryId", book.getCategoryId());
        detail.put("price", book.getPrice());
        detail.put("stock", book.getStock());
        detail.put("pages", book.getPages());
        detail.put("avgRating", book.getAvgRating());
        detail.put("ratingCount", book.getRatingCount());
        detail.put("viewCount", book.getViewCount());
        detail.put("collectCount", book.getCollectCount());
        detail.put("seriesInfo", book.getSeriesInfo());
        detail.put("description", book.getDescription());
        detail.put("coverImage", book.getCoverImage());
        detail.put("status", book.getStatus());
        detail.put("createdAt", book.getCreateTime());

        // 解析标签
        if (StringUtils.hasText(book.getTags())) {
            try {
                List<String> tags = objectMapper.readValue(book.getTags(), List.class);
                detail.put("tags", tags);
            } catch (Exception e) {
                detail.put("tags", List.of());
            }
        } else {
            detail.put("tags", List.of());
        }

        // 获取购书链接
        LambdaQueryWrapper<PurchaseLinks> linkWrapper = new LambdaQueryWrapper<>();
        linkWrapper.eq(PurchaseLinks::getBookId, bookId);
        List<PurchaseLinks> links = purchaseLinksMapper.selectList(linkWrapper);
        detail.put("purchaseLinks", links);

        return detail;
    }
}
