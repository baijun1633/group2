package com.cqu.springboot.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cqu.springboot.common.ApiResponse;
import com.cqu.springboot.common.BusinessException;
import com.cqu.springboot.common.ErrorCode;
import com.cqu.springboot.common.PageResponse;
import com.cqu.springboot.common.RequireSecondFactor;
import com.cqu.springboot.entity.Books;
import com.cqu.springboot.entity.ReadingProgress;
import com.cqu.springboot.entity.Users;
import com.cqu.springboot.service.BooksService;
import com.cqu.springboot.service.ReadingProgressService;
import com.cqu.springboot.service.UsersService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 管理员用户管理控制器
 * 路径: /api/v1/admin/users
 * 注：SecurityConfig 已配置 /api/v1/admin/** 需 ADMIN 角色，无需再加权限注解
 */
@Tag(name = "用户管理", description = "管理员用户CRUD")
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class AdminUsersController {

    private final UsersService usersService;
    private final ReadingProgressService readingProgressService;
    private final BooksService booksService;

    /**
     * 分页查询用户列表
     * GET /api/v1/admin/users
     */
    @Operation(summary = "分页查询用户列表", description = "支持按用户名/昵称关键词、角色、状态筛选")
    @GetMapping
    public ApiResponse<PageResponse<Users>> listUsers(
            @Parameter(description = "页码（从1开始）", example = "1") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量", example = "10") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "关键词（匹配用户名或昵称）", example = "admin") @RequestParam(required = false) String keyword,
            @Parameter(description = "角色（ADMIN/USER）", example = "USER") @RequestParam(required = false) String role,
            @Parameter(description = "状态（0-禁用, 1-启用）", example = "1") @RequestParam(required = false) Byte status) {

        Page<Users> result = usersService.listUsers(page, size, keyword, role, status);
        // password 字段已在 service 中置为 null
        PageResponse<Users> response = new PageResponse<>(
                result.getRecords(), result.getTotal(), page, size);
        return ApiResponse.success(response);
    }

    /**
     * 新增用户
     * POST /api/v1/admin/users
     */
    @Operation(summary = "新增用户", description = "管理员创建用户，密码会被加密存储，角色默认 USER")
    @PostMapping
    public ApiResponse<Void> createUser(@RequestBody Map<String, Object> request) {
        String username = (String) request.get("username");
        String password = (String) request.get("password");
        String nickname = (String) request.get("nickname");
        String email = (String) request.get("email");
        String phone = (String) request.get("phone");
        String role = (String) request.get("role");
        // role 默认 USER
        if (role == null || role.trim().isEmpty()) {
            role = "USER";
        }

        if (username == null || username.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "用户名不能为空");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "密码不能为空");
        }

        usersService.createUser(username.trim(), password, nickname, email, phone, role);
        return ApiResponse.success("新增成功", null);
    }

    /**
     * 修改用户
     * PUT /api/v1/admin/users/{userId}
     */
    @Operation(summary = "修改用户", description = "更新用户昵称、邮箱、手机号、角色、状态、密码（仅传字段才更新）")
    @PutMapping("/{userId}")
    @RequireSecondFactor("修改用户信息/权限")
    public ApiResponse<Void> updateUser(
            @Parameter(description = "用户ID", example = "1") @PathVariable Long userId,
            @RequestBody Map<String, Object> request) {

        String nickname = (String) request.get("nickname");
        String email = (String) request.get("email");
        String phone = (String) request.get("phone");
        String role = (String) request.get("role");
        String password = (String) request.get("password");

        Byte status = null;
        if (request.get("status") != null) {
            Object statusVal = request.get("status");
            if (statusVal instanceof Number) {
                status = ((Number) statusVal).byteValue();
            } else {
                status = Byte.valueOf(statusVal.toString());
            }
        }

        usersService.updateUser(userId, nickname, email, phone, role, status, password);
        return ApiResponse.success("更新成功", null);
    }

    /**
     * 删除用户
     * DELETE /api/v1/admin/users/{userId}
     */
    @Operation(summary = "删除用户", description = "根据用户ID删除用户")
    @DeleteMapping("/{userId}")
    @RequireSecondFactor("删除用户")
    public ApiResponse<Void> deleteUser(
            @Parameter(description = "用户ID", example = "1") @PathVariable Long userId) {

        Users user = usersService.getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        usersService.removeById(userId);
        return ApiResponse.success("删除成功", null);
    }

    /**
     * 查看用户阅读统计
     * GET /api/v1/admin/users/{userId}/reading/stats
     */
    @Operation(summary = "用户阅读统计", description = "运营管理员查看指定用户的阅读统计：周/月时长、已读完/在读数量")
    @GetMapping("/{userId}/reading/stats")
    public ApiResponse<Map<String, Object>> getUserReadingStats(
            @Parameter(description = "用户ID", example = "9") @PathVariable Long userId) {
        Users user = usersService.getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return ApiResponse.success(readingProgressService.getReadingStats(userId));
    }

    /**
     * 查看用户阅读历史
     * GET /api/v1/admin/users/{userId}/reading/history
     */
    @Operation(summary = "用户阅读历史", description = "运营管理员查看指定用户的阅读历史，支持状态过滤（reading/finished），返回含图书标题")
    @GetMapping("/{userId}/reading/history")
    public ApiResponse<PageResponse<Map<String, Object>>> getUserReadingHistory(
            @Parameter(description = "用户ID", example = "9") @PathVariable Long userId,
            @Parameter(description = "状态：reading=在读, finished=已读, 不传=全部") @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Users user = usersService.getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        PageResponse<ReadingProgress> result = readingProgressService.getReadingHistory(userId, status, page, size);
        // 批量查询图书标题
        Set<Long> bookIds = result.getItems().stream()
                .map(ReadingProgress::getBookId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> bookTitleMap = new HashMap<>();
        if (!bookIds.isEmpty()) {
            List<Books> books = booksService.listByIds(bookIds);
            for (Books b : books) {
                bookTitleMap.put(b.getBookId(), b.getTitle());
            }
        }
        // 组装返回（附加图书标题）
        List<Map<String, Object>> items = new ArrayList<>();
        for (ReadingProgress rp : result.getItems()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", rp.getId());
            item.put("userId", rp.getUserId());
            item.put("bookId", rp.getBookId());
            item.put("bookTitle", bookTitleMap.get(rp.getBookId()));
            item.put("currentPage", rp.getCurrentPage());
            item.put("chapter", rp.getChapter());
            item.put("totalPages", rp.getTotalPages());
            item.put("percentage", rp.getPercentage());
            item.put("readDuration", rp.getReadDuration());
            item.put("device", rp.getDevice());
            item.put("updateTime", rp.getUpdateTime());
            items.add(item);
        }
        return ApiResponse.success(new PageResponse<>(items, result.getTotal(), page, size));
    }
}
