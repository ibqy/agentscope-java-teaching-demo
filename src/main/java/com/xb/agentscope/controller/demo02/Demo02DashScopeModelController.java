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

@RestController
@RequestMapping("/api/demo02")
public class Demo02DashScopeModelController {

    private final String apiKey;

    public Demo02DashScopeModelController(@Value("${agentscope.api-key}") String apiKey) {
        this.apiKey = apiKey;
    }

    @GetMapping("/chat")
    public String chat(@RequestParam(defaultValue = "介绍一下你自己") String message) {
        DashScopeChatModel dashScopeModel = DashScopeChatModel.builder()
                .apiKey(apiKey).modelName("qwen-plus").stream(true)
                .formatter(new DashScopeChatFormatter()).build();
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