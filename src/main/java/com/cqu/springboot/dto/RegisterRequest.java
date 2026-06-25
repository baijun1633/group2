package com.cqu.springboot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 用户注册请求
 */
@Data
public class RegisterRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 30, message = "用户名长度为3-30个字符")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 50, message = "密码长度为6-50个字符")
    private String password;

    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;

    /**
     * 昵称（可选）
     */
    private String nickname;

    /**
     * 偏好标签（可选）
     */
    private List<String> preferenceTags;
}
