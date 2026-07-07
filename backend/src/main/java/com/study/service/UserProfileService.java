package com.study.service;

import com.study.dto.request.UpdateUserProfileRequest;
import com.study.vo.UserProfileVO;

/**
 * 用户画像服务接口
 *
 * <p>管理用户长期画像信息，支持用户手动设置和 AI 自动更新。
 */
public interface UserProfileService {

    /**
     * 获取当前用户的画像信息
     *
     * @param userId 用户 ID
     * @return 用户画像 VO（无记录时返回默认值）
     */
    UserProfileVO getProfile(Long userId);

    /**
     * 更新用户画像（用户手动设置）
     *
     * <p>空字段不覆盖已有值，实现部分更新。
     * 首次调用时自动创建画像记录。
     *
     * @param userId  用户 ID
     * @param request 更新请求
     */
    void updateProfile(Long userId, UpdateUserProfileRequest request);

    /**
     * 获取或创建用户画像（内部使用，确保记录存在）
     *
     * @param userId 用户 ID
     * @return 用户画像 VO
     */
    UserProfileVO getOrCreateProfile(Long userId);
}
