package com.cqu.springboot.security;

import com.cqu.springboot.common.ApiResponse;
import com.cqu.springboot.common.BusinessException;
import com.cqu.springboot.common.ErrorCode;
import com.cqu.springboot.common.RequireSecondFactor;
import com.cqu.springboot.entity.Users;
import com.cqu.springboot.service.UsersService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 高危操作二级验证拦截器
 * <p>
 * 仅对标注了 {@link RequireSecondFactor} 的 Controller 方法生效。
 * 校验请求头 {@code X-Second-Password} 与当前登录用户 users.second_password 字段是否匹配（BCrypt 比对）。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SecondFactorInterceptor implements HandlerInterceptor {

    public static final String SECOND_PASSWORD_HEADER = "X-Second-Password";

    private final UsersService usersService;
    private final ObjectMapper objectMapper;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        // 仅拦截标注了 @RequireSecondFactor 的方法
        RequireSecondFactor annotation = handlerMethod.getMethodAnnotation(RequireSecondFactor.class);
        if (annotation == null) {
            return true;
        }

        // 获取当前登录用户
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Long userId)) {
            writeError(response, ErrorCode.UNAUTHORIZED);
            return false;
        }

        Users user = usersService.getById(userId);
        if (user == null) {
            writeError(response, ErrorCode.USERNAME_NOT_FOUND);
            return false;
        }

        // 检查是否已设置二级密码
        if (!StringUtils.hasText(user.getSecondPassword())) {
            log.warn("高危操作二次验证失败：用户 {} 未设置二级密码，操作={}", user.getUsername(), annotation.value());
            writeError(response, ErrorCode.SECOND_FACTOR_NOT_SET);
            return false;
        }

        // 读取请求头中的二级密码
        String providedPassword = request.getHeader(SECOND_PASSWORD_HEADER);
        if (!StringUtils.hasText(providedPassword)) {
            log.warn("高危操作二次验证失败：用户 {} 未提供二级密码，操作={}", user.getUsername(), annotation.value());
            writeError(response, ErrorCode.SECOND_FACTOR_REQUIRED);
            return false;
        }

        // BCrypt 比对
        if (!passwordEncoder.matches(providedPassword, user.getSecondPassword())) {
            log.warn("高危操作二次验证失败：用户 {} 二级密码不正确，操作={}", user.getUsername(), annotation.value());
            writeError(response, ErrorCode.SECOND_FACTOR_INVALID);
            return false;
        }

        log.info("高危操作二次验证通过：用户 {}，操作={}", user.getUsername(), annotation.value());
        return true;
    }

    /**
     * 写入错误响应（JSON 格式，与全局异常处理一致）
     */
    private void writeError(HttpServletResponse response, ErrorCode errorCode) throws java.io.IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");
        ApiResponse<?> apiResponse = ApiResponse.error(errorCode);
        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }
}
