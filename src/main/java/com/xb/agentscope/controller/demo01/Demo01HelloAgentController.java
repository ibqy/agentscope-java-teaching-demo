package com.xb.agentscope.controller.demo01;

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
@RequestMapping("/api/demo01")
public class Demo01HelloAgentController {

    private final HarnessAgent assistantAgent;

    public Demo01HelloAgentController(HarnessAgent assistantAgent) {
        this.assistantAgent = assistantAgent;
    }

    @GetMapping("/hello")
    public String hello(@RequestParam(defaultValue = "你好，请做个自我介绍") String message) {
        UserMessage userMessage = new UserMessage(message);
        Msg reply = assistantAgent.call(List.<Msg>of(userMessage), RuntimeContext.empty()).block();
        return reply != null ? reply.getTextContent() : "（无回复）";
    }
}