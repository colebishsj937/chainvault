package com.chainvault.common.exception;

import lombok.Getter;

/**
 * 业务异常，携带错误码与提示信息
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Getter
public class BusinessException extends RuntimeException {

    /** 业务错误码 */
    private final int code;

    /**
     * 构造业务异常
     *
     * @param code    错误码
     * @param message 错误信息
     */
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * 构造业务异常（默认错误码 400）
     *
     * @param message 错误信息
     */
    public BusinessException(String message) {
        this(400, message);
    }
}
