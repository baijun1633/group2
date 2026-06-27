package com.cqu.springboot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cqu.springboot.common.BusinessException;
import com.cqu.springboot.common.ErrorCode;
import com.cqu.springboot.entity.Books;
import com.cqu.springboot.entity.ShelfBooks;
import com.cqu.springboot.entity.Shelves;
import com.cqu.springboot.mapper.BooksMapper;
import com.cqu.springboot.mapper.ShelfBooksMapper;
import com.cqu.springboot.mapper.ShelvesMapper;
import com.cqu.springboot.service.ShelvesService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 书架管理服务实现类
 */
@Service
@RequiredArgsConstructor
public class ShelvesServiceImpl implements ShelvesService {

    private final ShelvesMapper shelvesMapper;
    private final ShelfBooksMapper shelfBooksMapper;
    private final BooksMapper booksMapper;

    @Override
    @Transactional
    public Shelves createShelf(Long userId, String name, String description) {
        // 检查书架名称是否重复
        LambdaQueryWrapper<Shelves> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Shelves::getUserId, userId)
                .eq(Shelves::getName, name);
        if (shelvesMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "书架名称已存在");
        }

        Shelves shelf = new Shelves();
        shelf.setUserId(userId);
        shelf.setName(name);
        shelf.setDescription(description);
        shelf.setCreateTime(LocalDateTime.now());
        shelf.setUpdateTime(LocalDateTime.now());
        shelvesMapper.insert(shelf);

        return shelf;
    }

    @Override
    public List<Map<String, Object>> getUserShelves(Long userId) {
        // 查询用户的所有书架
        LambdaQueryWrapper<Shelves> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Shelves::getUserId, userId)
                .orderByDesc(Shelves::getCreateTime);
        List<Shelves> shelves = shelvesMapper.selectList(wrapper);

        if (shelves.isEmpty()) {
            return new ArrayList<>();
        }

        // 查询每个书架的图书数量
        List<Long> shelfIds = shelves.stream()
                .map(Shelves::getShelfId)
                .collect(Collectors.toList());

        LambdaQueryWrapper<ShelfBooks> countWrapper = new LambdaQueryWrapper<>();
        countWrapper.in(ShelfBooks::getShelfId, shelfIds);
        List<ShelfBooks> allShelfBooks = shelfBooksMapper.selectList(countWrapper);

        Map<Long, Long> countMap = allShelfBooks.stream()
                .collect(Collectors.groupingBy(ShelfBooks::getShelfId, Collectors.counting()));

        // 组装结果
        List<Map<String, Object>> result = new ArrayList<>();
        for (Shelves shelf : shelves) {
            Map<String, Object> item = new HashMap<>();
            item.put("shelfId", shelf.getShelfId());
            item.put("name", shelf.getName());
            item.put("description", shelf.getDescription());
            item.put("bookCount", countMap.getOrDefault(shelf.getShelfId(), 0L));
            item.put("createTime", shelf.getCreateTime());
            result.add(item);
        }

        return result;
    }

    @Override
    public Map<String, Object> getShelfDetail(Long userId, Long shelfId) {
        // 验证书架归属
        Shelves shelf = getAndValidateShelf(userId, shelfId);

        // 查询书架中的图书
        LambdaQueryWrapper<ShelfBooks> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShelfBooks::getShelfId, shelfId)
                .orderByDesc(ShelfBooks::getAddTime);
        List<ShelfBooks> shelfBooks = shelfBooksMapper.selectList(wrapper);

        // 获取图书详情
        List<Map<String, Object>> books = new ArrayList<>();
        if (!shelfBooks.isEmpty()) {
            List<Long> bookIds = shelfBooks.stream()
                    .map(ShelfBooks::getBookId)
                    .collect(Collectors.toList());
            Map<Long, Books> bookMap = booksMapper.selectBatchIds(bookIds).stream()
                    .collect(Collectors.toMap(Books::getBookId, b -> b));

            for (ShelfBooks sb : shelfBooks) {
                Books book = bookMap.get(sb.getBookId());
                if (book != null) {
                    Map<String, Object> bookInfo = new HashMap<>();
                    bookInfo.put("bookId", book.getBookId());
                    bookInfo.put("title", book.getTitle());
                    bookInfo.put("author", book.getAuthor());
                    bookInfo.put("coverUrl", book.getCoverImage());
                    bookInfo.put("readingStatus", sb.getReadingStatus());
                    bookInfo.put("addTime", sb.getAddTime());
                    books.add(bookInfo);
                }
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("shelfId", shelf.getShelfId());
        result.put("name", shelf.getName());
        result.put("description", shelf.getDescription());
        result.put("createTime", shelf.getCreateTime());
        result.put("books", books);

        return result;
    }

    @Override
    @Transactional
    public void addBookToShelf(Long userId, Long shelfId, Long bookId, Integer readingStatus) {
        // 验证书架归属
        getAndValidateShelf(userId, shelfId);

        // 验证书籍存在
        Books book = booksMapper.selectById(bookId);
        if (book == null) {
            throw new BusinessException(ErrorCode.BOOK_NOT_FOUND);
        }

        // 检查是否已在书架中
        LambdaQueryWrapper<ShelfBooks> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShelfBooks::getShelfId, shelfId)
                .eq(ShelfBooks::getBookId, bookId);
        if (shelfBooksMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "该图书已在书架中");
        }

        ShelfBooks shelfBook = new ShelfBooks();
        shelfBook.setShelfId(shelfId);
        shelfBook.setBookId(bookId);
        shelfBook.setReadingStatus(readingStatus != null ? readingStatus : 0);
        shelfBook.setAddTime(LocalDateTime.now());
        shelfBooksMapper.insert(shelfBook);
    }

    @Override
    @Transactional
    public void updateBookReadingStatus(Long userId, Long shelfId, Long bookId, Integer readingStatus) {
        // 验证书架归属
        getAndValidateShelf(userId, shelfId);

        LambdaQueryWrapper<ShelfBooks> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShelfBooks::getShelfId, shelfId)
                .eq(ShelfBooks::getBookId, bookId);
        ShelfBooks shelfBook = shelfBooksMapper.selectOne(wrapper);

        if (shelfBook == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "该图书不在书架中");
        }

        shelfBook.setReadingStatus(readingStatus);
        shelfBooksMapper.updateById(shelfBook);
    }

    @Override
    @Transactional
    public void removeBookFromShelf(Long userId, Long shelfId, Long bookId) {
        // 验证书架归属
        getAndValidateShelf(userId, shelfId);

        LambdaQueryWrapper<ShelfBooks> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShelfBooks::getShelfId, shelfId)
                .eq(ShelfBooks::getBookId, bookId);
        shelfBooksMapper.delete(wrapper);
    }

    @Override
    @Transactional
    public void deleteShelf(Long userId, Long shelfId) {
        // 验证书架归属
        getAndValidateShelf(userId, shelfId);

        // 删除书架中的所有图书
        LambdaQueryWrapper<ShelfBooks> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShelfBooks::getShelfId, shelfId);
        shelfBooksMapper.delete(wrapper);

        // 删除书架
        shelvesMapper.deleteById(shelfId);
    }

    /**
     * 验证书架归属并返回书架
     */
    private Shelves getAndValidateShelf(Long userId, Long shelfId) {
        Shelves shelf = shelvesMapper.selectById(shelfId);
        if (shelf == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "书架不存在");
        }
        if (!shelf.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED, "无权操作此书架");
        }
        return shelf;
    }
}
