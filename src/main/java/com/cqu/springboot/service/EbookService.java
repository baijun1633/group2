package com.cqu.springboot.service;

import com.cqu.springboot.dto.EbookInfo;
import org.springframework.web.multipart.MultipartFile;

/**
 * 电子书服务接口
 */
public interface EbookService {

    /**
     * 获取电子书试读内容（按登录状态截取章节）
     *
     * @param bookId 图书ID
     * @param userId 当前用户ID（null 表示未登录）
     * @return 电子书试读信息（含可试读章节列表）
     */
    EbookInfo getEbookPreview(Long bookId, Long userId);

    /**
     * 保存上传的电子书文件并更新 books 表
     *
     * @param bookId 图书ID
     * @param file   上传的电子书文件（epub/pdf）
     * @return 电子书文件访问 URL
     */
    String saveEbookFile(Long bookId, MultipartFile file);
}
