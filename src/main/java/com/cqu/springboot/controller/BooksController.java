package com.cqu.springboot.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cqu.springboot.entity.Books;
import com.cqu.springboot.service.BooksService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * <p>
 * 书籍表 前端控制器
 * </p>
 *
 * @author MisterDong
 * @since 2026-06-23
 */
@RestController
@RequestMapping("/books")
public class BooksController {

    @GetMapping("/test")
    public String test() {
        return "Library System is running!";
    }

    @Resource
    private BooksService booksService;

    @GetMapping("/getallbooks")
    public List<Books> getallbooks() {
        List<Books> books = booksService.findall();
        System.err.println("书籍数量:"+books.size());
        return booksService.findall();
    }




}
