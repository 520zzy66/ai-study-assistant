package com.study.service;

import com.study.dto.request.LoginRequest;
import com.study.dto.request.RegisterRequest;
import com.study.vo.LoginVO;

/**
 * 认证服务接口
 */
public interface AuthService {

    /**
     * 用户注册
     *
     * @param request 注册请求
     */
    void register(RegisterRequest request);

    /**
     * 用户登录
     *
     * @param request 登录请求
     * @return 登录结果（token + 用户信息）
     */
    LoginVO login(LoginRequest request);
}
