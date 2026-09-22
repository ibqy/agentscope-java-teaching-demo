package com.xb.agentscope.controller.demo03;

import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.UserMessage;
import io.agentscope.harness.agent.HarnessAgent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

/**
 * Demo03CallController - Agent call() 调用方式示例
 *
 * 演示 HarnessAgent.call() 的标准用法：构造消息列表、发送、阻塞等待回复。
 * 重点理解消息（Msg）的组装方式和 call() 的返回值处理。
 *
 * @author ibqy
 */
@RestController
@RequestMapping("/api/demo03")
public class Demo03CallController {

    private final HarnessAgent assistantAgent;

    public Demo03CallController(HarnessAgent assistantAgent) {
        this.assistantAgent = assistantAgent;
    }

    /**
     * 向 Agent 发送消息并返回文本回复
     *
     * @param message 用户输入
     * @return Agent 回复文本
     */
    @GetMapping("/call")
    public String call(@RequestParam(defaultValue = "用一句话介绍 AgentScope") String message) {
        List<Msg> messages = List.of(new UserMessage(message));
        Msg reply = assistantAgent.call(messages, RuntimeContext.empty()).block();
        return reply != null ? reply.getTextContent() : "（无回复）";
    }
}