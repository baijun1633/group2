package com.cqu.springboot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cqu.springboot.common.BusinessException;
import com.cqu.springboot.common.ErrorCode;
import com.cqu.springboot.entity.Books;
import com.cqu.springboot.entity.ReadingProgress;
import com.cqu.springboot.mapper.BooksMapper;
import com.cqu.springboot.mapper.ReadingProgressMapper;
import com.cqu.springboot.service.ReadingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 阅读进度服务实现类
 */
@Service
@RequiredArgsConstructor
public class ReadingServiceImpl implements ReadingService {

    private final ReadingProgressMapper readingProgressMapper;
    private final BooksMapper booksMapper;

    @Override
    @Transactional
    public void syncProgress(Long userId, Long bookId, Integer currentPage, Integer totalPages, String device) {
        // 验证书籍存在
        Books book = booksMapper.selectById(bookId);
        if (book == null) {
            throw new BusinessException(ErrorCode.BOOK_NOT_FOUND);
        }

        // 计算百分比
        BigDecimal percentage = BigDecimal.ZERO;
        if (totalPages != null && totalPages > 0 && currentPage != null) {
            percentage = new BigDecimal(currentPage)
                    .divide(new BigDecimal(totalPages), 2, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal(100));
        }

        // 查询是否已有进度
        LambdaQueryWrapper<ReadingProgress> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReadingProgress::getUserId, userId)
                .eq(ReadingProgress::getBookId, bookId);
        ReadingProgress existing = readingProgressMapper.selectOne(wrapper);

        if (existing != null) {
            // 更新进度
            existing.setCurrentPage(currentPage);
            existing.setTotalPages(totalPages);
            existing.setPercentage(percentage);
            existing.setDevice(device);
            existing.setUpdateTime(LocalDateTime.now());
            readingProgressMapper.updateById(existing);
        } else {
            // 新增进度
            ReadingProgress progress = new ReadingProgress();
            progress.setUserId(userId);
            progress.setBookId(bookId);
            progress.setCurrentPage(currentPage);
            progress.setTotalPages(totalPages);
            progress.setPercentage(percentage);
            progress.setDevice(device);
            progress.setUpdateTime(LocalDateTime.now());
            readingProgressMapper.insert(progress);
        }
    }

    @Override
    public ReadingProgress getProgress(Long userId, Long bookId) {
        LambdaQueryWrapper<ReadingProgress> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReadingProgress::getUserId, userId)
                .eq(ReadingProgress::getBookId, bookId);
        return readingProgressMapper.selectOne(wrapper);
    }

    @Override
    public Map<String, Object> getReadingHistory(Long userId, int page, int size) {
        Page<ReadingProgress> pageParam = new Page<>(page, size);

        LambdaQueryWrapper<ReadingProgress> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReadingProgress::getUserId, userId)
                .orderByDesc(ReadingProgress::getUpdateTime);

        Page<ReadingProgress> result = readingProgressMapper.selectPage(pageParam, wrapper);

        // 获取图书详情
        List<Map<String, Object>> records = new ArrayList<>();
        if (!result.getRecords().isEmpty()) {
            List<Long> bookIds = result.getRecords().stream()
                    .map(ReadingProgress::getBookId)
                    .collect(Collectors.toList());
            Map<Long, Books> bookMap = booksMapper.selectBatchIds(bookIds).stream()
                    .collect(Collectors.toMap(Books::getBookId, b -> b));

            for (ReadingProgress progress : result.getRecords()) {
                Books book = bookMap.get(progress.getBookId());
                if (book != null) {
                    Map<String, Object> record = new HashMap<>();
                    record.put("bookId", book.getBookId());
                    record.put("title", book.getTitle());
                    record.put("author", book.getAuthor());
                    record.put("coverUrl", book.getCoverImage());
                    record.put("currentPage", progress.getCurrentPage());
                    record.put("totalPages", progress.getTotalPages());
                    record.put("percentage", progress.getPercentage());
                    record.put("device", progress.getDevice());
                    record.put("updateTime", progress.getUpdateTime());
                    records.add(record);
                }
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("records", records);
        response.put("total", result.getTotal());
        response.put("page", page);
        response.put("size", size);

        return response;
    }
}
