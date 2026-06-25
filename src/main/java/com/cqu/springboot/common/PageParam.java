package com.cqu.springboot.common;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 分页请求参数封装
 */
@Data
public class PageParam {

    @Min(value = 1, message = "页码最小为1")
    private int page = 1;

    @Min(value = 1, message = "每页条数最小为1")
    @Max(value = 100, message = "每页条数最大为100")
    private int size = 20;

    /**
     * 计算偏移量（用于SQL的OFFSET）
     */
    public long getOffset() {
        return (long) (page - 1) * size;
    }
}
