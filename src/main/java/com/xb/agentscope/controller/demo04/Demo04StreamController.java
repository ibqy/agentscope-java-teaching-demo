package com.xb.agentscope.controller.demo04;

import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.AgentEvent;
import io.agentscope.core.event.TextBlockDeltaEvent;
import io.agentscope.core.message.UserMessage;
import io.agentscope.harness.agent.HarnessAgent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/demo04")
public class Demo04StreamController {

    private final HarnessAgent assistantAgent;

    public Demo04StreamController(HarnessAgent assistantAgent) {
        this.assistantAgent = assistantAgent;
    }

    @GetMapping("/stream")
    public String stream(@RequestParam(defaultValue = "请用三句话介绍 DashScope") String message) {
        StringBuilder collected = new StringBuilder();
        List<AgentEvent> events = assistantAgent.streamEvents(
                        new UserMessage(message), RuntimeContext.empty())
                .doOnNext(event -> {
                    if (event instanceof TextBlockDeltaEvent tde) {
                        collected.append(tde.getDelta());
                    }
                })
                .collectList().block();
        int eventCount = events == null ? 0 : events.size();
        return "流式事件条数 = " + eventCount + System.lineSeparator()
                + "拼接文本 >>>" + System.lineSeparator() + collected;
    }
}