package com.study.controller;

import com.study.common.Result;
import com.study.dto.request.LoginRequest;
import com.study.dto.request.RegisterRequest;
import com.study.service.AuthService;
import com.study.vo.LoginVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证控制器
 */
@Tag(name = "认证管理", description = "用户注册、登录等认证相关接口")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 用户注册
     *
     * @param request 注册请求
     * @return Result
     */
    @Operation(summary = "用户注册", description = "注册新用户账号")
    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return Result.successMsg("注册成功");
    }

    /**
     * 用户登录
     *
     * @param request 登录请求
     * @return 登录结果（token + 用户信息）
     */
    @Operation(summary = "用户登录", description = "用户登录获取 token")
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginRequest request) {
        LoginVO vo = authService.login(request);
        return Result.success(vo);
    }
}
