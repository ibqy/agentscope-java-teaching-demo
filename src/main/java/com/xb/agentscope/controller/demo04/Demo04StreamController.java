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

/**
 * Demo04StreamController - 流式输出（Streaming）示例控制器
 *
 * 演示 Agent 的流式事件订阅机制，通过 streamEvents() 逐块接收
 * 模型生成的文本增量（TextBlockDeltaEvent），模拟"打字机效果"。
 *
 * @author ibqy
 */
@RestController
@RequestMapping("/api/demo04")
public class Demo04StreamController {

    private final HarnessAgent assistantAgent;

    public Demo04StreamController(HarnessAgent assistantAgent) {
        this.assistantAgent = assistantAgent;
    }

    /**
     * 流式接收 Agent 的事件增量并拼接为完整文本
     *
     * @param message 用户输入的消息
     * @return 包含事件条数和拼接后完整文本的结果
     */
    @GetMapping("/stream")
    public String stream(@RequestParam(defaultValue = "请用三句话介绍 DashScope") String message) {
        StringBuilder collected = new StringBuilder();
        // streamEvents 返回响应式事件流，doOnNext 逐条处理增量文本事件
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