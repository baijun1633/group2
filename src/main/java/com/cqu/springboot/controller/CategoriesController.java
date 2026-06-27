package com.cqu.springboot.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cqu.springboot.common.ApiResponse;
import com.cqu.springboot.entity.Categories;
import com.cqu.springboot.mapper.CategoriesMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 图书分类表 前端控制器
 */
@Tag(name = "图书分类", description = "图书分类查询接口")
@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoriesController {

    private final CategoriesMapper categoriesMapper;

    /**
     * 获取所有分类列表
     * GET /api/v1/categories
     */
    @Operation(summary = "获取所有分类", description = "获取图书分类列表，可用于筛选图书时确定分类ID")
    @GetMapping
    public ApiResponse<List<Categories>> getCategories() {
        List<Categories> list = categoriesMapper.selectList(
                new LambdaQueryWrapper<Categories>().orderByAsc(Categories::getCategoryId));
        return ApiResponse.success(list);
    }

    /**
     * 根据ID获取分类详情
     * GET /api/v1/categories/{categoryId}
     */
    @Operation(summary = "获取分类详情", description = "根据分类ID获取分类详细信息")
    @GetMapping("/{categoryId}")
    public ApiResponse<Categories> getCategoryById(@Parameter(description = "分类ID", example = "1") @PathVariable Long categoryId) {
        Categories category = categoriesMapper.selectById(categoryId);
        if (category == null) {
            return ApiResponse.error(4002, "分类不存在");
        }
        return ApiResponse.success(category);
    }
}
