package com.study.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一响应结果类
 *
 * @param <T> 数据类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {

    /**
     * 状态码
     */
    private Integer code;

    /**
     * 消息
     */
    private String message;

    /**
     * 数据
     */
    private T data;

    /**
     * 成功响应（无数据）
     *
     * @return Result
     */
    public static <T> Result<T> success() {
        return new Result<>(200, "success", null);
    }

    /**
     * 成功响应（带数据）
     *
     * @param data 数据
     * @return Result
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "success", data);
    }

    /**
     * 成功响应（仅消息，无数据）
     * 注意：使用 successMsg 命名避免与 success(T data) 泛型重载歧义
     *
     * @param message 消息
     * @return Result
     */
    public static <T> Result<T> successMsg(String message) {
        return new Result<>(200, message, null);
    }

    /**
     * 成功响应（带消息和数据）
     *
     * @param message 消息
     * @param data    数据
     * @return Result
     */
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(200, message, data);
    }

    /**
     * 错误响应
     *
     * @param code    错误码
     * @param message 错误信息
     * @return Result
     */
    public static <T> Result<T> error(Integer code, String message) {
        return new Result<>(code, message, null);
    }

    /**
     * 系统错误响应
     *
     * @param message 错误信息
     * @return Result
     */
    public static <T> Result<T> error(String message) {
        return new Result<>(500, message, null);
    }
}
