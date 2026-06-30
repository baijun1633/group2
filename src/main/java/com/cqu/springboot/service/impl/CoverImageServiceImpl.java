package com.cqu.springboot.service.impl;

import com.cqu.springboot.common.BusinessException;
import com.cqu.springboot.common.ErrorCode;
import com.cqu.springboot.config.FileConfig;
import com.cqu.springboot.entity.Books;
import com.cqu.springboot.service.BooksService;
import com.cqu.springboot.service.CoverImageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * 图书封面图片服务实现
 * <p>
 * 存储路径: d:/code/library_sys/uploads/covers/{book_id}.{ext}
 * 访问 URL: /uploads/covers/{book_id}.{ext}
 * </p>
 * <p>
 * 支持格式: jpg, jpeg, png, gif, webp
 * 最大大小: 5MB
 * 重复上传会覆盖旧文件（含不同扩展名的旧文件）
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CoverImageServiceImpl implements CoverImageService {

    private static final String UPLOAD_DIR = "d:/code/library_sys/uploads/covers";
    private static final String URL_PREFIX = "/covers/";
    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024; // 5MB

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(
            "jpg", "jpeg", "png", "gif", "webp"
    );

    private final BooksService booksService;
    private final FileConfig fileConfig;

    @Override
    public String uploadCover(Long bookId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "文件不能为空");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(ErrorCode.IMAGE_FILE_TOO_LARGE);
        }

        String originalName = file.getOriginalFilename();
        if (originalName == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "文件名不能为空");
        }
        String ext = extractExtension(originalName).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new BusinessException(ErrorCode.IMAGE_FORMAT_UNSUPPORTED);
        }

        Books book = booksService.getById(bookId);
        if (book == null) {
            throw new BusinessException(ErrorCode.BOOK_NOT_FOUND);
        }

        try {
            Path dir = Paths.get(UPLOAD_DIR);
            Files.createDirectories(dir);

            deleteOldCoverFile(bookId);

            String fileName = bookId + "." + ext;
            Path target = dir.resolve(fileName);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            String url = URL_PREFIX + fileName;
            book.setCoverImage(url);
            book.setUpdateTime(LocalDateTime.now());
            booksService.updateById(book);

            String fullUrl = fileConfig.getBaseUrl() + url;
            log.info("封面图片已保存: bookId={}, file={}, size={}bytes", bookId, fileName, file.getSize());
            return fullUrl;
        } catch (IOException e) {
            log.error("保存封面图片失败: bookId={}", bookId, e);
            throw new BusinessException(ErrorCode.OPERATION_FAILED, "保存文件失败: " + e.getMessage());
        }
    }

    private String extractExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot < 0 ? "" : fileName.substring(dot + 1);
    }

    /**
     * 删除指定 bookId 的旧封面文件（不论扩展名）
     */
    private void deleteOldCoverFile(Long bookId) {
        File dir = new File(UPLOAD_DIR);
        if (!dir.exists()) {
            return;
        }
        File[] files = dir.listFiles((d, name) -> name.startsWith(bookId + "."));
        if (files == null) {
            return;
        }
        for (File f : files) {
            if (f.delete()) {
                log.info("已删除旧封面图片: {}", f.getName());
            }
        }
    }
}
