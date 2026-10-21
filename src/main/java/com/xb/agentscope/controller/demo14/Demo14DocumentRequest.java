package com.xb.agentscope.controller.demo14;

import jakarta.validation.constraints.NotBlank;

/**
 * Demo14DocumentRequest - 文档入库请求体
 *
 * 用于 /api/demo14/documents 接口，演示 @NotBlank 校验与 record 的结合。
 *
 * @author ibqy
 */
public record Demo14DocumentRequest(
        @NotBlank(message = "title 不能为空")
        String title,
        @NotBlank(message = "content 不能为空")
        String content
) {
}
