package com.cqu.springboot.dto;

import lombok.Data;
import java.util.List;

/**
 * Python KG 微服务响应封装
 */
@Data
public class PythonKgResponse {

    private Integer code;
    private String msg;
    private List<KgRecommendItem> data;

    @Data
    public static class KgRecommendItem {
        private Long bookId;
        private String bookTitle;
        private Double matchScore;
        private String recommendReason;
        private Integer pathHops;
        private String recommendType;
    }
}
