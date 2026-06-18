package com.study.common;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 用户上下文
 * 使用 ThreadLocal 存储当前请求的用户信息
 */
public class UserContext {

    private static final ThreadLocal<UserInfo> HOLDER = new ThreadLocal<>();

    /**
     * 设置当前用户
     *
     * @param user 用户信息
     */
    public static void setCurrentUser(UserInfo user) {
        HOLDER.set(user);
    }

    /**
     * 获取当前用户
     *
     * @return 用户信息
     * @throws BusinessException 未登录时抛出异常
     */
    public static UserInfo getCurrentUser() {
        UserInfo user = HOLDER.get();
        if (user == null) {
            throw new BusinessException(401, "未登录");
        }
        return user;
    }

    /**
     * 获取当前用户 ID
     *
     * @return 用户 ID
     */
    public static Long getCurrentUserId() {
        return getCurrentUser().getUserId();
    }

    /**
     * 清除用户上下文
     */
    public static void clear() {
        HOLDER.remove();
    }

    /**
     * 用户信息
     */
    @Data
    @AllArgsConstructor
    public static class UserInfo {
        private Long userId;
        private String username;
    }
}
