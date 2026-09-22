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
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Demo11Controller - 综合任务助手（Full Task Assistant）示例控制器
 *
 * 融合工具调用、记忆、流式输出等所有能力，构建一个完整的任务型工作助手。
 * 教学要点：多工具注册 + 记忆隔离 + 流式输出的综合集成模式。
 *
 * @author ibqy
 */
@RestController
@RequestMapping("/api/demo11")
public class Demo11Controller {

    // 按 sessionId 缓存 Agent 实例，同一会话复用同一个 Agent 保持记忆连续
    private final Map<String, HarnessAgent> agentCache = new ConcurrentHashMap<>();

    /**
     * 获取或创建指定会话的 Agent 实例
     *
     * @param sessionId 会话 ID
     * @return 对应会话的 HarnessAgent
     */
    private HarnessAgent assistantFor(String sessionId) {
        return agentCache.computeIfAbsent(sessionId, this::buildAssistant);
    }

    /**
     * 构建一个集成了任务管理、天气查询、计算和备忘工具的记忆 Agent
     *
     * @param sessionId 会话 ID，用于隔离工作空间路径
     * @return 配置完成的 HarnessAgent
     */
    private HarnessAgent buildAssistant(String sessionId) {
        Toolkit toolkit = new Toolkit();
        // 注册三类工具：任务管理、天气查询、计算备忘
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

    /**
     * 通过表单参数发送任务指令给 Agent
     *
     * @param session     会话 ID
     * @param instruction 用户指令
     * @return Agent 的文本回复
     */
    @PostMapping("/task")
    public String task(@RequestParam(defaultValue = "demo") String session,
                       @RequestParam String instruction) {
        HarnessAgent assistant = assistantFor(session);
        Msg reply = assistant.call(List.of(new UserMessage(instruction)), RuntimeContext.empty()).block();
        return reply != null ? reply.getTextContent() : "（无回复）";
    }

    /**
     * 通过 JSON 请求体发送任务指令（支持 @Valid 校验）
     *
     * @param req 包含 session 和 instruction 的请求体
     * @return Agent 的文本回复
     */
    @PostMapping("/execute")
    public String execute(@Valid @RequestBody Demo11TaskRequest req) {
        String sessionId = (req.session() == null || req.session().isBlank()) ? "default" : req.session();
        HarnessAgent assistant = assistantFor(sessionId);
        Msg reply = assistant.call(List.of(new UserMessage(req.instruction())), RuntimeContext.empty()).block();
        return reply != null ? reply.getTextContent() : "（无回复）";
    }

    /**
     * 以流式方式接收 Agent 的回复并拼接为完整文本
     *
     * @param session 会话 ID
     * @param message 用户输入
     * @return 包含事件条数和拼接结果的字符串
     */
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