package com.cqu.springboot.security;

import com.cqu.springboot.common.BusinessException;
import com.cqu.springboot.common.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 安全工具类 - 获取当前登录用户信息
 */
public class SecurityUtils {

    private SecurityUtils() {
    }

    /**
     * 获取当前登录用户ID
     *
     * @return 用户ID
     * @throws BusinessException 未登录时抛出异常
     */
    public static Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof Long userId) {
            return userId;
        }
        throw new BusinessException(ErrorCode.UNAUTHORIZED);
    }

    /**
     * 获取当前登录用户ID（可选，未登录返回null）
     */
    public static Long getCurrentUserIdOrNull() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return null;
            }
            Object principal = authentication.getPrincipal();
            if (principal instanceof Long userId) {
                return userId;
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}
