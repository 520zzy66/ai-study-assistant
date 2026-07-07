package com.study.controller;

import com.study.common.Result;
import com.study.common.UserContext;
import com.study.dto.request.UpdateUserProfileRequest;
import com.study.service.UserProfileService;
import com.study.vo.UserProfileVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 用户画像控制器
 *
 * <p>提供用户画像的查询和更新接口，前端设置页使用。
 */
@Tag(name = "用户画像", description = "用户学习画像管理接口")
@RestController
@RequestMapping("/user/learning-profile")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;

    /**
     * 获取当前用户画像
     *
     * @return 用户画像信息
     */
    @Operation(summary = "获取用户画像", description = "获取当前登录用户的学习画像信息")
    @GetMapping
    public Result<UserProfileVO> getProfile() {
        Long userId = UserContext.getCurrentUserId();
        UserProfileVO vo = userProfileService.getProfile(userId);
        return Result.success(vo);
    }

    /**
     * 更新当前用户画像
     *
     * @param request 更新请求（空字段不覆盖已有值）
     * @return Result
     */
    @Operation(summary = "更新用户画像", description = "更新当前用户的学习偏好、薄弱知识点等画像信息")
    @PutMapping
    public Result<Void> updateProfile(@Valid @RequestBody UpdateUserProfileRequest request) {
        Long userId = UserContext.getCurrentUserId();
        userProfileService.updateProfile(userId, request);
        return Result.successMsg("保存成功");
    }
}
