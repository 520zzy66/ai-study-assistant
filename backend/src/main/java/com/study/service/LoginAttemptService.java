package com.study.service;

/**
 * 登录尝试服务
 * 管理登录失败次数和账户锁定
 */
public interface LoginAttemptService {

    /**
     * 登录成功后清除失败记录
     *
     * @param username 用户名
     */
    void loginSucceeded(String username);

    /**
     * 原子化检查锁定状态并递增失败计数
     * 解决检查与递增之间的 TOCTOU 竞态
     *
     * @param username 用户名
     * @return true 如果账户仍可登录，false 如果已被锁定
     */
    boolean checkAndIncrement(String username);
}
