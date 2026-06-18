package com.study.controller;

import com.study.common.Result;
import com.study.common.UserContext;
import com.study.dto.request.ChangePasswordRequest;
import com.study.dto.request.UpdateProfileRequest;
import com.study.service.UserService;
import com.study.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 用户控制器
 */
@Tag(name = "用户管理", description = "用户个人信息管理接口")
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 获取个人信息
     *
     * @return 用户信息
     */
    @Operation(summary = "获取个人信息", description = "获取当前登录用户的个人信息")
    @GetMapping("/profile")
    public Result<UserVO> getProfile() {
        Long userId = UserContext.getCurrentUserId();
        UserVO vo = userService.getProfile(userId);
        return Result.success(vo);
    }

    /**
     * 修改个人信息
     *
     * @param request 修改请求
     * @return Result
     */
    @Operation(summary = "修改个人信息", description = "修改当前登录用户的昵称、邮箱、头像")
    @PutMapping("/profile")
    public Result<Void> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        Long userId = UserContext.getCurrentUserId();
        userService.updateProfile(userId, request);
        return Result.successMsg("更新成功");
    }

    /**
     * 修改密码
     *
     * @param request 修改密码请求
     * @return Result
     */
    @Operation(summary = "修改密码", description = "修改当前登录用户的密码")
    @PutMapping("/password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        Long userId = UserContext.getCurrentUserId();
        userService.changePassword(userId, request);
        return Result.successMsg("密码修改成功");
    }
}
