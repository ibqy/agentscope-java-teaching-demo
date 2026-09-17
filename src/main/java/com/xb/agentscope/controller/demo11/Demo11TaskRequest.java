package com.xb.agentscope.controller.demo11;

import jakarta.validation.constraints.NotBlank;

/**
 * demo11 任务指令请求体
 *
 * <p>作者：ibqy | 日期：2026-09-17</p>
 */
public record Demo11TaskRequest(
        String session,
        @NotBlank(message = "instruction 不能为空")
        String instruction
) {
}
