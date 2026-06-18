package com.study.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.study.common.BusinessException;
import com.study.common.UserConverter;
import com.study.dto.request.LoginRequest;
import com.study.dto.request.RegisterRequest;
import com.study.entity.User;
import com.study.mapper.UserMapper;
import com.study.security.JwtUtil;
import com.study.service.AuthService;
import com.study.service.LoginAttemptService;
import com.study.vo.LoginVO;
import com.study.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 认证服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final LoginAttemptService loginAttemptService;

    @Override
    @Transactional
    public void register(RegisterRequest request) {
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        // nickname 为空时默认等于 username
        user.setNickname(StrUtil.isBlank(request.getNickname())
                ? request.getUsername() : request.getNickname());
        user.setEmail(request.getEmail());

        // 依赖数据库唯一索引 uk_username 保证并发安全，
        // 捕获 DuplicateKeyException 替代先查后插的 TOCTOU 竞态
        try {
            userMapper.insert(user);
        } catch (org.springframework.dao.DuplicateKeyException e) {
            throw new BusinessException(1001, "用户名已存在");
        }
        log.info("用户注册成功，username: {}", request.getUsername());
    }

    @Override
    public LoginVO login(LoginRequest request) {
        String username = request.getUsername();

        // 检查账户是否被锁定
        if (loginAttemptService.isLocked(username)) {
            throw new BusinessException(1003, "账户已锁定，请15分钟后重试");
        }

        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));

        // 用户名不存在或密码错误统一提示
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            loginAttemptService.loginFailed(username);
            throw new BusinessException(1002, "用户名或密码错误");
        }

        // 登录成功，清除失败记录
        loginAttemptService.loginSucceeded(username);

        String token = jwtUtil.generateToken(user.getId(), user.getUsername());

        UserVO userVO = UserConverter.toVO(user);

        log.info("用户登录成功，username: {}", username);
        return LoginVO.builder()
                .token(token)
                .expiresIn(jwtUtil.getExpiration())
                .user(userVO)
                .build();
    }
}
