package com.xb.agentscope.controller.demo07;

import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.SystemMessage;
import io.agentscope.core.message.UserMessage;
import io.agentscope.harness.agent.HarnessAgent;
import io.agentscope.harness.agent.memory.MemoryConfig;
import io.agentscope.harness.agent.memory.compaction.CompactionConfig;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Demo07MemoryWorkspaceController - 记忆与工作空间持久化示例
 *
 * 演示 Agent 的记忆机制：通过 MemoryConfig 和 CompactionConfig 配置
 * 对话记忆的存储与压缩策略，不同会话名称的 Agent 各自保持独立记忆。
 *
 * @author ibqy
 */
@RestController
@RequestMapping("/api/demo07")
public class Demo07MemoryWorkspaceController {

    // 按用户名称缓存 Agent 实例，同一 name 的对话共享历史记忆
    private final Map<String, HarnessAgent> agentCache = new ConcurrentHashMap<>();

    /**
     * 与具有记忆能力的 Agent 进行对话
     *
     * @param name 会话名称，相同名称共享记忆
     * @param text 用户输入内容
     * @return Agent 的回复
     */
    @GetMapping("/memory")
    public String memory(@RequestParam String name, @RequestParam String text) {
        // computeIfAbsent：首次请求时创建 Agent，后续复用同一实例保持记忆连续
        HarnessAgent agent = agentCache.computeIfAbsent(name, k -> buildMemoryAgent(k));
        List<Msg> messages = new ArrayList<>();
        messages.add(new SystemMessage("你拥有记忆能力，请记住用户在本会话中提到过的关键信息，并在被问及时回忆出来。"));
        messages.add(new UserMessage(text));
        Msg reply = agent.call(messages, RuntimeContext.empty()).block();
        return reply != null ? reply.getTextContent() : "（无回复）";
    }

    /**
     * 构建具有记忆功能的 Agent
     *
     * @param name 用于生成独立工作空间路径，实现持久化隔离
     * @return 配置完成的记忆 Agent
     */
    private HarnessAgent buildMemoryAgent(String name) {
        MemoryConfig memoryConfig = MemoryConfig.builder().model("dashscope:qwen-plus").build();
        CompactionConfig compactionConfig = CompactionConfig.builder()
                .triggerMessages(6).triggerTokens(1200).keepMessages(3).build();
        return HarnessAgent.builder()
                .name("memory-agent")
                .sysPrompt("你是一个有长期记忆的助手。")
                .model("dashscope:qwen-plus")
                .workspace(Paths.get(".agentscope/workspace-" + name))
                .memory(memoryConfig).compaction(compactionConfig)
                .build();
    }
}