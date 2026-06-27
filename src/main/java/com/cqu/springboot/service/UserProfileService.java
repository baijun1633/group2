package com.cqu.springboot.service;

import java.util.Map;

/**
 * 用户画像服务接口
 */
public interface UserProfileService {

    /**
     * 获取用户画像
     */
    Map<String, Object> getUserProfile(Long userId);

    /**
     * 刷新用户画像（根据书架和评分计算）
     */
    void refreshProfile(Long userId);
}
