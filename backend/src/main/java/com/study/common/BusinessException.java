package com.study.common;

import lombok.Getter;

/**
 * 业务异常类
 * 用于封装业务逻辑中的异常情况
 */
@Getter
public class BusinessException extends RuntimeException {

    /**
     * 错误码
     */
    private final Integer code;

    /**
     * 构造函数
     *
     * @param code    错误码
     * @param message 错误信息
     */
    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * 构造函数
     *
     * @param code    错误码
     * @param message 错误信息
     * @param cause   原因
     */
    public BusinessException(Integer code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }
}
