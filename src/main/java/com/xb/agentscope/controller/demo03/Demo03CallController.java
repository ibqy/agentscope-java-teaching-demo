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

@RestController
@RequestMapping("/api/demo03")
public class Demo03CallController {

    private final HarnessAgent assistantAgent;

    public Demo03CallController(HarnessAgent assistantAgent) {
        this.assistantAgent = assistantAgent;
    }

    @GetMapping("/call")
    public String call(@RequestParam(defaultValue = "用一句话介绍 AgentScope") String message) {
        List<Msg> messages = List.of(new UserMessage(message));
        Msg reply = assistantAgent.call(messages, RuntimeContext.empty()).block();
        return reply != null ? reply.getTextContent() : "（无回复）";
    }
}