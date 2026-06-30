package com.cqu.springboot.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 图书封面图片服务接口
 */
public interface CoverImageService {

    /**
     * 上传图书封面图片并更新 books 表的 cover_image 字段
     *
     * @param bookId 图书ID
     * @param file   图片文件（jpg/jpeg/png/gif/webp）
     * @return 封面图片访问 URL
     */
    String uploadCover(Long bookId, MultipartFile file);
}
