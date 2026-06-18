package com.study.service;

/**
 * 登录尝试服务
 * 管理登录失败次数和账户锁定
 */
public interface LoginAttemptService {

    /**
     * 记录登录失败
     *
     * @param username 用户名
     */
    void loginFailed(String username);

    /**
     * 登录成功后清除失败记录
     *
     * @param username 用户名
     */
    void loginSucceeded(String username);

    /**
     * 判断账户是否被锁定
     *
     * @param username 用户名
     * @return true 表示已锁定
     */
    boolean isLocked(String username);
}
