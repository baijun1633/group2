package com.cqu.springboot.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Token刷新请求
 */
@Data
public class RefreshRequest {

    @NotBlank(message = "refreshToken不能为空")
    private String refreshToken;
}
