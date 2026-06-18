package com.study.common;

import com.study.entity.User;
import com.study.vo.UserVO;

/**
 * 用户对象转换器
 * 提供 User 实体到 UserVO 的转换，避免重复构建逻辑
 */
public class UserConverter {

    private UserConverter() {
        // 工具类，禁止实例化
    }

    /**
     * 将 User 实体转换为 UserVO
     *
     * @param user 用户实体
     * @return 用户 VO
     */
    public static UserVO toVO(User user) {
        if (user == null) {
            return null;
        }
        return UserVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .avatar(user.getAvatar())
                .createTime(user.getCreateTime())
                .build();
    }
}
