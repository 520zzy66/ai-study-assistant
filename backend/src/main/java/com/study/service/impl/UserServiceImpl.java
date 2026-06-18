package com.study.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.study.common.BusinessException;
import com.study.common.UserConverter;
import com.study.dto.request.ChangePasswordRequest;
import com.study.dto.request.UpdateProfileRequest;
import com.study.entity.User;
import com.study.mapper.UserMapper;
import com.study.service.UserService;
import com.study.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserVO getProfile(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }

        return UserConverter.toVO(user);
    }

    @Override
    @Transactional
    public void updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }

        LambdaUpdateWrapper<User> wrapper = new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId);
        boolean hasUpdate = false;

        if (StrUtil.isNotBlank(request.getNickname())) {
            wrapper.set(User::getNickname, request.getNickname());
            hasUpdate = true;
        }
        if (StrUtil.isNotBlank(request.getEmail())) {
            wrapper.set(User::getEmail, request.getEmail());
            hasUpdate = true;
        }
        if (StrUtil.isNotBlank(request.getAvatar())) {
            wrapper.set(User::getAvatar, request.getAvatar());
            hasUpdate = true;
        }

        if (!hasUpdate) {
            return;
        }

        userMapper.update(wrapper);
        log.info("用户信息更新成功，userId: {}", userId);
    }

    @Override
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }

        // 验证旧密码
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BusinessException(400, "当前密码不正确");
        }

        // 更新新密码
        String encodedPassword = passwordEncoder.encode(request.getNewPassword());
        LambdaUpdateWrapper<User> wrapper = new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .set(User::getPassword, encodedPassword);
        userMapper.update(wrapper);

        log.info("密码修改成功，userId: {}", userId);
    }
}
