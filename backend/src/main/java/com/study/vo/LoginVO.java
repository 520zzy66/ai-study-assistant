package com.study.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 登录响应 VO
 */
@Data
@Builder
public class LoginVO {

    /** JWT token */
    private String token;

    /** 过期时间（毫秒） */
    private Long expiresIn;

    /** 用户信息 */
    private UserVO user;
}
