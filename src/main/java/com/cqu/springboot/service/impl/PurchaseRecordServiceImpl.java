package com.cqu.springboot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cqu.springboot.common.BusinessException;
import com.cqu.springboot.common.ErrorCode;
import com.cqu.springboot.common.PageResponse;
import com.cqu.springboot.dto.PurchaseRecordRequest;
import com.cqu.springboot.entity.PurchaseRecord;
import com.cqu.springboot.mapper.PurchaseRecordMapper;
import com.cqu.springboot.service.PurchaseRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 购买记录服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseRecordServiceImpl implements PurchaseRecordService {

    private final PurchaseRecordMapper purchaseRecordMapper;

    @Override
    public Long recordPurchase(Long userId, PurchaseRecordRequest request) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "请先登录");
        }
        if (request == null || request.getBookId() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "图书ID不能为空");
        }

        PurchaseRecord record = new PurchaseRecord();
        record.setUserId(userId);
        record.setBookId(request.getBookId());
        record.setPurchasePlatform(request.getPlatform());
        record.setPurchasePrice(request.getPrice());
        record.setPurchaseUrl(request.getUrl());
        record.setPurchaseTime(LocalDateTime.now());
        purchaseRecordMapper.insert(record);
        log.info("记录购买意向 userId={}, bookId={}, platform={}, purchaseId={}",
                userId, request.getBookId(), request.getPlatform(), record.getPurchaseId());
        return record.getPurchaseId();
    }

    @Override
    public PageResponse<PurchaseRecord> getUserPurchases(Long userId, Long bookId, String platform, int page, int size) {
        Page<PurchaseRecord> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<PurchaseRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PurchaseRecord::getUserId, userId);
        if (bookId != null) {
            wrapper.eq(PurchaseRecord::getBookId, bookId);
        }
        if (platform != null && !platform.isBlank()) {
            wrapper.eq(PurchaseRecord::getPurchasePlatform, platform);
        }
        wrapper.orderByDesc(PurchaseRecord::getPurchaseTime);

        Page<PurchaseRecord> result = purchaseRecordMapper.selectPage(pageParam, wrapper);
        return new PageResponse<>(result.getRecords(), result.getTotal(), page, size);
    }
}
