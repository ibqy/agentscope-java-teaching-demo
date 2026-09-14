package com.xb.agentscope.controller.demo09;

import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.SystemMessage;
import io.agentscope.core.message.UserMessage;
import io.agentscope.harness.agent.HarnessAgent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/api/demo09")
public class Demo09PromptController {

    @GetMapping("/persona")
    public String persona(
            @RequestParam(defaultValue = "teacher") String persona,
            @RequestParam(defaultValue = "解释什么是大模型") String message) {
        String sysPrompt = switch (persona) {
            case "tour_guide" -> "你是一位热情的旅游导游，回答要生动有趣，并给出实用的小贴士。";
            case "code_reviewer" -> "你是一位资深代码评审专家，回答严谨，专注于可读性、规范与潜在缺陷。";
            default -> "你是一位耐心的好老师，回答循序渐进、通俗易懂，并善于举例。";
        };
        HarnessAgent agent = HarnessAgent.builder()
                .name("persona-agent").sysPrompt(sysPrompt)
                .model("dashscope:qwen-plus")
                .workspace(Paths.get(".agentscope/workspace-demo09"))
                .build();
        List<Msg> messages = List.of(
                new SystemMessage("请严格保持当前角色身份，不要偏离设定的语气。"),
                new UserMessage(message));
        Msg reply = agent.call(messages, RuntimeContext.empty()).block();
        return reply != null ? reply.getTextContent() : "（无回复）";
    }
}