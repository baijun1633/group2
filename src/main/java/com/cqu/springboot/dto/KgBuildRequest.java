package com.cqu.springboot.dto;

import java.util.List;
import lombok.Data;

/**
 * 知识图谱构建请求
 */
@Data
public class KgBuildRequest {

    /**
     * 指定书籍ID列表，为空则全量构建
     */
    private List<Long> bookIds;

    /**
     * 是否强制全量重建（清空已有图谱数据后重新构建）
     */
    private Boolean forceRebuild = false;
}
