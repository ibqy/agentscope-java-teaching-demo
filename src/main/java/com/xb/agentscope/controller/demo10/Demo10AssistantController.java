package com.xb.agentscope.controller.demo10;

import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.UserMessage;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.harness.agent.HarnessAgent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.nio.file.Paths;
import java.util.List;

/**
 * Demo10AssistantController - 多功能助理（Multi-Tool Assistant）示例控制器
 *
 * 演示如何给一个 Agent 注册多个工具，让它在对话中按需自主选择调用，
 * 教学要点：Toolkit 多工具注册、maxIters 控制多轮工具调用循环。
 *
 * @author ibqy
 */
@RestController
@RequestMapping("/api/demo10")
public class Demo10AssistantController {

    private final String apiKey;

    public Demo10AssistantController(@Value("${agentscope.api-key}") String apiKey) {
        this.apiKey = apiKey;
    }

    /**
     * 向多功能助理发送请求，Agent 会自主决定是否使用计算或备忘工具
     *
     * @param message 用户的指令内容
     * @return Agent 的回复文本
     */
    @GetMapping("/assistant")
    public String assistant(@RequestParam(defaultValue = "帮我把 12*3+5 算出来") String message) {
        Toolkit toolkit = new Toolkit();
        toolkit.registerTool(new AssistantTools());
        HarnessAgent assistant = HarnessAgent.builder()
                .name("all-in-one-assistant")
                .sysPrompt("你是一个集聊天、计算、备忘于一体的智能助理，善用工具辅助回答。")
                .model("dashscope:qwen-plus").toolkit(toolkit).maxIters(5)
                .workspace(Paths.get(".agentscope/workspace-demo10"))
                .build();
        Msg reply = assistant.call(List.<Msg>of(new UserMessage(message)), RuntimeContext.empty()).block();
        return reply != null ? reply.getTextContent() : "（无回复）";
    }
}