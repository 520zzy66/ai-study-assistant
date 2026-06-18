package com.study.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户信息 VO（不包含密码、deleted 等敏感字段）
 */
@Data
@Builder
public class UserVO {

    private Long id;
    private String username;
    private String nickname;
    private String email;
    private String avatar;
    private LocalDateTime createTime;
}
