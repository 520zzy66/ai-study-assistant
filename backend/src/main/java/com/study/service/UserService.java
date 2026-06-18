package com.study.service;

import com.study.dto.request.ChangePasswordRequest;
import com.study.dto.request.UpdateProfileRequest;
import com.study.vo.UserVO;

/**
 * 用户服务接口
 */
public interface UserService {

    /**
     * 获取个人信息
     *
     * @param userId 用户 ID
     * @return 用户信息
     */
    UserVO getProfile(Long userId);

    /**
     * 修改个人信息
     *
     * @param userId  用户 ID
     * @param request 修改请求
     */
    void updateProfile(Long userId, UpdateProfileRequest request);

    /**
     * 修改密码
     *
     * @param userId  用户 ID
     * @param request 修改密码请求
     */
    void changePassword(Long userId, ChangePasswordRequest request);
}
