package com.study.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改个人信息请求 DTO
 */
@Data
public class UpdateProfileRequest {

    @Size(max = 20, message = "昵称最多20位")
    private String nickname;

    @Email(message = "邮箱格式不正确")
    private String email;

    @Pattern(regexp = "^https://.*", message = "头像必须是有效的 HTTPS URL 地址")
    private String avatar;
}
