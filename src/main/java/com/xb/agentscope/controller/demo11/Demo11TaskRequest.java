package com.xb.agentscope.controller.demo11;

import jakarta.validation.constraints.NotBlank;

/**
 * Demo11TaskRequest - 任务指令请求体（JSON 格式）
 *
 * 用于 /api/demo11/execute 接口，演示 @NotBlank 参数校验与 record 类的结合使用。
 *
 * @author ibqy
 */
public record Demo11TaskRequest(
        String session,
        @NotBlank(message = "instruction 不能为空")
        String instruction
) {
}
