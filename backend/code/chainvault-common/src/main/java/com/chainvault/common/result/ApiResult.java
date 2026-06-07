package com.chainvault.common.result;

import lombok.Data;

/**
 * 统一 API 响应封装
 *
 * @param <T> 业务数据类型
 * @author chainvault
 * @date 2026-06-05
 */
@Data
public class ApiResult<T> {

    /** 状态码，0 表示成功 */
    private int code;

    /** 提示信息 */
    private String message;

    /** 业务数据 */
    private T data;

    /**
     * 成功响应（无数据）
     *
     * @param <T> 数据类型
     * @return 成功结果
     */
    public static <T> ApiResult<T> ok() {
        return ok(null);
    }

    /**
     * 成功响应（带数据）
     *
     * @param data 业务数据
     * @param <T>  数据类型
     * @return 成功结果
     */
    public static <T> ApiResult<T> ok(T data) {
        ApiResult<T> result = new ApiResult<>();
        result.setCode(0);
        result.setMessage("success");
        result.setData(data);
        return result;
    }

    /**
     * 失败响应
     *
     * @param code    错误码
     * @param message 错误信息
     * @param <T>     数据类型
     * @return 失败结果
     */
    public static <T> ApiResult<T> fail(int code, String message) {
        ApiResult<T> result = new ApiResult<>();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }
}
