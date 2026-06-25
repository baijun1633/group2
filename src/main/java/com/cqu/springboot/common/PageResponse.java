package com.cqu.springboot.common;

import lombok.Data;

import java.util.List;

/**
 * 分页响应体
 */
@Data
public class PageResponse<T> {

    private List<T> items;
    private long total;
    private int page;
    private int size;

    public PageResponse(List<T> items, long total, int page, int size) {
        this.items = items;
        this.total = total;
        this.page = page;
        this.size = size;
    }
}
