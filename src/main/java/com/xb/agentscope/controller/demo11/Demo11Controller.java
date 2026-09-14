package com.xb.agentscope.controller.demo11;

import com.xb.agentscope.controller.demo05.WeatherTools;
import com.xb.agentscope.controller.demo10.AssistantTools;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.AgentEvent;
import io.agentscope.core.event.TextBlockDeltaEvent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.UserMessage;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.harness.agent.HarnessAgent;
import io.agentscope.harness.agent.memory.MemoryConfig;
import io.agentscope.harness.agent.memory.compaction.CompactionConfig;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/demo11")
public class Demo11Controller {

    private final Map<String, HarnessAgent> agentCache = new ConcurrentHashMap<>();

    private HarnessAgent assistantFor(String sessionId) {
        return agentCache.computeIfAbsent(sessionId, this::buildAssistant);
    }

    private HarnessAgent buildAssistant(String sessionId) {
        Toolkit toolkit = new Toolkit();
        toolkit.registerTool(new TaskTools());
        toolkit.registerTool(new WeatherTools());
        toolkit.registerTool(new AssistantTools());
        MemoryConfig memoryConfig = MemoryConfig.builder().model("dashscope:qwen-plus").build();
        CompactionConfig compactionConfig = CompactionConfig.builder()
                .triggerMessages(8).triggerTokens(1600).keepMessages(4).build();
        return HarnessAgent.builder()
                .name("task-assistant")
                .sysPrompt("你是一位'长期记忆的任务式工作助手'。你可以帮助用户："
                        + "①创建/查看/完成结构化任务(create_task/list_tasks/complete_task)；"
                        + "②查询天气、做四则运算；③记住用户的偏好并在后续对话中回忆。"
                        + "请高效使用工具完成用户指令，并以简洁清晰的中文作答。")
                .model("dashscope:qwen-plus").toolkit(toolkit).maxIters(6)
                .workspace(Paths.get(".agentscope/workspace-demo11", sessionId))
                .memory(memoryConfig).compaction(compactionConfig).build();
    }

    @PostMapping("/task")
    public String task(@RequestParam(defaultValue = "demo") String session,
                       @RequestParam String instruction) {
        HarnessAgent assistant = assistantFor(session);
        Msg reply = assistant.call(List.of(new UserMessage(instruction)), RuntimeContext.empty()).block();
        return reply != null ? reply.getTextContent() : "（无回复）";
    }

    @GetMapping("/stream")
    public String stream(@RequestParam(defaultValue = "demo") String session,
                         @RequestParam(defaultValue = "请帮我列出当前所有任务") String message) {
        HarnessAgent assistant = assistantFor(session);
        StringBuilder collected = new StringBuilder();
        List<AgentEvent> events = assistant.streamEvents(new UserMessage(message), RuntimeContext.empty())
                .doOnNext(event -> {
                    if (event instanceof TextBlockDeltaEvent tde) collected.append(tde.getDelta());
                })
                .collectList().block();
        int eventCount = events == null ? 0 : events.size();
        return "流式事件条数 = " + eventCount + System.lineSeparator()
                + "逐块增量拼接结果 >>>" + System.lineSeparator() + collected;
    }
}