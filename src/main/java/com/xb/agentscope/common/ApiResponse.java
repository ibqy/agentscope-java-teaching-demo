package com.xb.agentscope.common;

/**
 * 统一 API 响应包装 —— 所有接口返回统一 JSON 结构
 *
 * <p>作者：ibqy | 日期：2026-09-17</p>
 *
 * <p><b>实战价值</b>：前端只需一种解析方式。成功 code=0，失败 code 为业务错误码。</p>
 *
 * @author ibqy
 */
public record ApiResponse<T>(
        int code,
        String message,
        T data
) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(0, "ok", data);
    }

    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }
}
