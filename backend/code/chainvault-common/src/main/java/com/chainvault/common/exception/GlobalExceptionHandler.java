package com.chainvault.common.exception;

import com.chainvault.common.result.ApiResult;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 *
 * @author chainvault
 * @date 2026-06-05
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 处理业务异常
     *
     * @param e 业务异常
     * @return 统一错误响应
     */
    @ExceptionHandler(BusinessException.class)
    public ApiResult<Void> handleBusiness(BusinessException e) {
        return ApiResult.fail(e.getCode(), e.getMessage());
    }

    /**
     * 处理参数校验失败
     *
     * @param e 校验异常
     * @return 统一错误响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResult<Void> handleValidation(MethodArgumentNotValidException e) {
        // 1. 拼接所有字段错误信息
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return ApiResult.fail(400, msg);
    }

    /**
     * 处理未预期异常
     *
     * @param e   异常
     * @param req 请求
     * @return 统一错误响应
     */
    @ExceptionHandler(Exception.class)
    public ApiResult<Void> handleUnexpected(Exception e, HttpServletRequest req) {
        log.error("未处理异常 uri={}", req.getRequestURI(), e);
        return ApiResult.fail(500, "服务内部错误，请联系管理员");
    }
}
