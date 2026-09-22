package com.xb.agentscope.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.UUID;

/**
 * 全局异常处理器 —— 统一拦截 Controller 层异常，返回结构化错误响应
 *
 * <p>作者：ibqy | 日期：2026-09-17</p>
 *
 * <p><b>分类处理</b>：参数缺失 → 400、校验失败 → 400、业务异常 → 400、未知异常 → 500（带 requestId）</p>
 *
 * @author ibqy
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理请求缺少必要参数时抛出的异常
     *
     * @param ex 参数缺失异常
     * @return 包含缺失参数名的错误响应
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleMissingParam(MissingServletRequestParameterException ex) {
        return ApiResponse.error(400, "缺少必要参数: " + ex.getParameterName());
    }

    /**
     * 处理 @Valid 校验失败时抛出的异常，提取首个出错字段信息
     *
     * @param ex 校验异常
     * @return 包含字段和错误信息的响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleValidation(MethodArgumentNotValidException ex) {
        String field = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .orElse("参数校验失败");
        return ApiResponse.error(400, field);
    }

    /**
     * 处理业务逻辑中的非法参数异常
     *
     * @param ex 非法参数异常
     * @return 包含异常消息的错误响应
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleIllegalArgument(IllegalArgumentException ex) {
        return ApiResponse.error(400, ex.getMessage());
    }

    /**
     * 兜底处理所有未被上层捕获的未知异常，生成 requestId 便于排查
     *
     * @param ex 未处理的异常
     * @return 包含 requestId 的内部错误响应
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleUnknown(Exception ex) {
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        log.error("[{}] 未处理异常", requestId, ex);
        return ApiResponse.error(500, "服务内部错误，requestId=" + requestId);
    }
}
