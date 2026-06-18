package com.study.common;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusiness(BusinessException e) {
        log.warn("业务异常: code={}, message={}", e.getCode(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理参数校验异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("参数校验失败: {}", msg);
        return Result.error(400, msg);
    }

    /**
     * 处理文件大小超限异常
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<Void> handleFileSize(MaxUploadSizeExceededException e) {
        log.warn("文件大小超过限制", e);
        return Result.error(400, "文件大小超过限制（50MB）");
    }

    /**
     * 处理 @RequestParam / @PathVariable 校验异常
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Void> handleConstraintViolation(ConstraintViolationException e) {
        String msg = e.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining("; "));
        log.warn("参数校验失败: {}", msg);
        return Result.error(400, msg);
    }

    /**
     * 处理表单绑定校验异常
     */
    @ExceptionHandler(BindException.class)
    public Result<Void> handleBind(BindException e) {
        String msg = e.getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("表单绑定校验失败: {}", msg);
        return Result.error(400, msg);
    }

    /**
     * 处理请求体解析失败（JSON 格式错误等）
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleMessageNotReadable(HttpMessageNotReadableException e) {
        log.warn("请求体解析失败: {}", e.getMessage());
        return Result.error(400, "请求格式错误，请检查请求体");
    }

    /**
     * 处理 HTTP 请求方法不支持
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public Result<Void> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        log.warn("不支持的请求方法: {}", e.getMethod());
        return Result.error(405, "不支持的请求方法: " + e.getMethod());
    }

    /**
     * 处理系统异常
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleSystem(Exception e) {
        log.error("系统异常", e);
        return Result.error(500, "系统繁忙，请稍后再试");
    }
}
