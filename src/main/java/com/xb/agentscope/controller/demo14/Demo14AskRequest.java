package com.xb.agentscope.controller.demo14;

import jakarta.validation.constraints.NotBlank;

/**
 * Demo14AskRequest - 知识库问答请求体
 *
 * @param question 用户问题，不能为空
 * @author ibqy
 */
public record Demo14AskRequest(
        @NotBlank(message = "question 不能为空")
        String question
) {
}
