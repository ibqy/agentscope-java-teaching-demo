package com.xb.agentscope.controller.demo02;

import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.UserMessage;
import io.agentscope.extensions.model.dashscope.DashScopeChatModel;
import io.agentscope.extensions.model.dashscope.formatter.DashScopeChatFormatter;
import io.agentscope.harness.agent.HarnessAgent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.nio.file.Paths;
import java.util.List;

/**
 * Demo02DashScopeModelController - 显式创建 DashScopeChatModel 示例
 *
 * 与 Demo01 不同，本示例在 Controller 中手动构建模型和 Agent，
 * 演示模型配置的完整参数，帮助学习者理解底层装配过程。
 *
 * @author ibqy
 */
@RestController
@RequestMapping("/api/demo02")
public class Demo02DashScopeModelController {

    private final String apiKey;

    public Demo02DashScopeModelController(@Value("${agentscope.api-key}") String apiKey) {
        this.apiKey = apiKey;
    }

    /**
     * 使用显式构建的 DashScopeChatModel 与 Agent 对话
     *
     * 每次请求都会新建模型和 Agent 实例，演示完整的构建流程
     *
     * @param message 用户输入的消息
     * @return Agent 的文本回复
     */
    @GetMapping("/chat")
    public String chat(@RequestParam(defaultValue = "介绍一下你自己") String message) {
        // 显式构建模型：需要手动指定 apiKey、模型名、格式化器
        DashScopeChatModel dashScopeModel = DashScopeChatModel.builder()
                .apiKey(apiKey).modelName("qwen-plus").stream(true)
                .formatter(new DashScopeChatFormatter()).build();
        // 用构建好的模型实例创建 Agent，与 Demo01 注入 Bean 的方式对比
        HarnessAgent agent = HarnessAgent.builder()
                .name("dashscope-explicit-agent")
                .sysPrompt("你是一个由显式 DashScopeChatModel 驱动的智能体。")
                .model(dashScopeModel)
                .workspace(Paths.get(".agentscope/workspace-demo02"))
                .build();
        Msg reply = agent.call(List.<Msg>of(new UserMessage(message)), RuntimeContext.empty()).block();
        return reply != null ? reply.getTextContent() : "（无回复）";
    }
}