package com.cqu.springboot.service.impl;

import com.cqu.springboot.common.BusinessException;
import com.cqu.springboot.common.ErrorCode;
import com.cqu.springboot.dto.EbookChapter;
import com.cqu.springboot.dto.EbookInfo;
import com.cqu.springboot.entity.Books;
import com.cqu.springboot.service.BooksService;
import com.cqu.springboot.service.EbookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.siegmann.epublib.domain.SpineReference;
import nl.siegmann.epublib.domain.Resource;
import nl.siegmann.epublib.epub.EpubReader;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 电子书服务实现
 * <p>
 * 存储路径: d:/code/library_sys/uploads/ebooks/{book_id}.{ext}
 * 访问 URL: /uploads/ebooks/{book_id}.{ext}
 * </p>
 * <p>
 * 试读权限:
 * - 未登录用户: 前 3 章
 * - 登录用户: 前 10 章
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EbookServiceImpl implements EbookService {

    private static final String UPLOAD_DIR = "d:/code/library_sys/uploads/ebooks";
    private static final String URL_PREFIX = "/uploads/ebooks/";
    private static final long MAX_FILE_SIZE = 100L * 1024 * 1024; // 100MB

    /** 未登录可试读章节数 */
    private static final int PREVIEW_CHAPTERS_ANONYMOUS = 3;
    /** 登录用户可试读章节数 */
    private static final int PREVIEW_CHAPTERS_LOGGED_IN = 10;

    /** 从 HTML 中提取 <title> 或 <h1> 的正则 */
    private static final Pattern TITLE_PATTERN = Pattern.compile(
            "<title[^>]*>([^<]+)</title>", Pattern.CASE_INSENSITIVE);
    private static final Pattern H1_PATTERN = Pattern.compile(
            "<h1[^>]*>([^<]+)</h1>", Pattern.CASE_INSENSITIVE);

    private final BooksService booksService;

    @Override
    public EbookInfo getEbookPreview(Long bookId, Long userId) {
        Books book = booksService.getById(bookId);
        if (book == null) {
            throw new BusinessException(ErrorCode.BOOK_NOT_FOUND);
        }
        if (book.getEbookUrl() == null || book.getEbookType() == null) {
            throw new BusinessException(ErrorCode.EBOOK_NOT_UPLOADED);
        }

        // 解析本地文件路径
        String relativePath = book.getEbookUrl().replace(URL_PREFIX, "");
        File ebookFile = new File(UPLOAD_DIR, relativePath);
        if (!ebookFile.exists()) {
            log.warn("电子书文件不存在: bookId={}, path={}", bookId, ebookFile.getAbsolutePath());
            throw new BusinessException(ErrorCode.EBOOK_FILE_NOT_FOUND);
        }

        boolean isLogin = userId != null;
        int previewChapters = isLogin ? PREVIEW_CHAPTERS_LOGGED_IN : PREVIEW_CHAPTERS_ANONYMOUS;

        List<EbookChapter> allChapters;
        try {
            String type = book.getEbookType().toLowerCase();
            if ("epub".equals(type)) {
                allChapters = parseEpub(ebookFile);
            } else if ("pdf".equals(type)) {
                allChapters = parsePdf(ebookFile);
            } else {
                throw new BusinessException(ErrorCode.EBOOK_FORMAT_UNSUPPORTED);
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("电子书解析失败: bookId={}, type={}", bookId, book.getEbookType(), e);
            throw new BusinessException(ErrorCode.EBOOK_PARSE_ERROR, e.getMessage());
        }

        int totalChapters = allChapters.size();
        int actualPreview = Math.min(previewChapters, totalChapters);
        List<EbookChapter> previewList = allChapters.subList(0, actualPreview);

        EbookInfo info = new EbookInfo();
        info.setBookId(bookId);
        info.setTitle(book.getTitle());
        info.setEbookType(book.getEbookType());
        info.setTotalChapters(totalChapters);
        info.setPreviewChapters(actualPreview);
        info.setIsLogin(isLogin);
        info.setTruncated(actualPreview < totalChapters);
        info.setChapters(previewList);
        if (actualPreview < totalChapters) {
            info.setMessage(isLogin
                    ? "登录用户可试读前 " + PREVIEW_CHAPTERS_LOGGED_IN + " 章，完整阅读需购买"
                    : "登录后可试读前 " + PREVIEW_CHAPTERS_LOGGED_IN + " 章");
        }
        return info;
    }

    @Override
    public String saveEbookFile(Long bookId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "文件不能为空");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(ErrorCode.EBOOK_FILE_TOO_LARGE);
        }

        String originalName = file.getOriginalFilename();
        if (originalName == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "文件名不能为空");
        }
        String ext = extractExtension(originalName).toLowerCase();
        if (!"epub".equals(ext) && !"pdf".equals(ext)) {
            throw new BusinessException(ErrorCode.EBOOK_FORMAT_UNSUPPORTED);
        }

        Books book = booksService.getById(bookId);
        if (book == null) {
            throw new BusinessException(ErrorCode.BOOK_NOT_FOUND);
        }

        try {
            // 确保目录存在
            Path dir = Paths.get(UPLOAD_DIR);
            Files.createDirectories(dir);

            // 删除旧文件（如果存在不同扩展名的旧文件）
            deleteOldEbookFile(bookId);

            // 保存新文件
            String fileName = bookId + "." + ext;
            Path target = dir.resolve(fileName);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            // 更新 books 表
            String url = URL_PREFIX + fileName;
            book.setEbookUrl(url);
            book.setEbookType(ext);
            book.setEbookSize(file.getSize());
            book.setUpdateTime(LocalDateTime.now());
            booksService.updateById(book);

            log.info("电子书文件已保存: bookId={}, file={}, size={}bytes", bookId, fileName, file.getSize());
            return url;
        } catch (IOException e) {
            log.error("保存电子书文件失败: bookId={}", bookId, e);
            throw new BusinessException(ErrorCode.OPERATION_FAILED, "保存文件失败: " + e.getMessage());
        }
    }

    // ============ EPUB 解析 ============

    private List<EbookChapter> parseEpub(File file) throws Exception {
        List<EbookChapter> chapters = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(file)) {
            EpubReader reader = new EpubReader();
            nl.siegmann.epublib.domain.Book epubBook = reader.readEpub(fis);

            List<SpineReference> spineRefs = epubBook.getSpine().getSpineReferences();
            for (int i = 0; i < spineRefs.size(); i++) {
                Resource resource = spineRefs.get(i).getResource();
                byte[] data = resource.getData();
                String html = data == null ? "" : new String(data, StandardCharsets.UTF_8);

                // 跳过空白章节（如封面、版权页等只有图片或极少文本的）
                String title = extractTitleFromHtml(html, "第" + (i + 1) + "节");
                String cleanHtml = cleanEpubHtml(html);

                EbookChapter chapter = new EbookChapter();
                chapter.setIndex(i + 1);
                chapter.setTitle(title);
                chapter.setContent(cleanHtml);
                chapter.setLength(cleanHtml.length());
                chapters.add(chapter);
            }
        }
        return chapters;
    }

    /**
     * 清理 EPUB HTML 内容（移除 head/script/style，仅保留 body 内容）
     */
    private String cleanEpubHtml(String html) {
        if (html == null || html.isEmpty()) {
            return "";
        }
        // 移除 head 部分
        String result = html.replaceAll("(?is)<head[^>]*>.*?</head>", "");
        // 移除 script 和 style
        result = result.replaceAll("(?is)<script[^>]*>.*?</script>", "");
        result = result.replaceAll("(?is)<style[^>]*>.*?</style>", "");
        // 提取 body 内容（如果有）
        Matcher bodyMatcher = Pattern.compile("(?is)<body[^>]*>(.*?)</body>").matcher(result);
        if (bodyMatcher.find()) {
            result = bodyMatcher.group(1);
        }
        // 移除 XML 声明和 DOCTYPE
        result = result.replaceAll("(?is)<\\?xml[^>]*\\?>", "");
        result = result.replaceAll("(?is)<!DOCTYPE[^>]*>", "");
        return result.trim();
    }

    /**
     * 从 HTML 中提取标题（优先 <title>，其次 <h1>）
     */
    private String extractTitleFromHtml(String html, String defaultTitle) {
        if (html == null || html.isEmpty()) {
            return defaultTitle;
        }
        Matcher m = TITLE_PATTERN.matcher(html);
        if (m.find() && !m.group(1).isBlank()) {
            return m.group(1).trim();
        }
        m = H1_PATTERN.matcher(html);
        if (m.find() && !m.group(1).isBlank()) {
            return m.group(1).trim();
        }
        return defaultTitle;
    }

    // ============ PDF 解析 ============

    private List<EbookChapter> parsePdf(File file) throws Exception {
        List<EbookChapter> chapters = new ArrayList<>();
        try (PDDocument document = Loader.loadPDF(file)) {
            int totalPages = document.getNumberOfPages();
            PDFTextStripper stripper = new PDFTextStripper();
            for (int i = 1; i <= totalPages; i++) {
                stripper.setStartPage(i);
                stripper.setEndPage(i);
                String text = stripper.getText(document);
                if (text == null) {
                    text = "";
                }
                text = text.trim();
                // 把纯文本包成 HTML（用 <pre> 保留换行）
                String html = text.isEmpty()
                        ? ""
                        : "<pre style=\"white-space: pre-wrap;\">" + escapeHtml(text) + "</pre>";

                EbookChapter chapter = new EbookChapter();
                chapter.setIndex(i);
                chapter.setTitle("第 " + i + " 页");
                chapter.setContent(html);
                chapter.setLength(text.length());
                chapters.add(chapter);
            }
        }
        return chapters;
    }

    private String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    // ============ 工具方法 ============

    private String extractExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot < 0 ? "" : fileName.substring(dot + 1);
    }

    /**
     * 删除指定 bookId 的旧电子书文件（不论扩展名）
     */
    private void deleteOldEbookFile(Long bookId) {
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
                log.info("已删除旧电子书文件: {}", f.getName());
            }
        }
    }
}
