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

@RestController
@RequestMapping("/api/demo07")
public class Demo07MemoryWorkspaceController {

    private final Map<String, HarnessAgent> agentCache = new ConcurrentHashMap<>();

    @GetMapping("/memory")
    public String memory(@RequestParam String name, @RequestParam String text) {
        HarnessAgent agent = agentCache.computeIfAbsent(name, k -> buildMemoryAgent(k));
        List<Msg> messages = new ArrayList<>();
        messages.add(new SystemMessage("你拥有记忆能力，请记住用户在本会话中提到过的关键信息，并在被问及时回忆出来。"));
        messages.add(new UserMessage(text));
        Msg reply = agent.call(messages, RuntimeContext.empty()).block();
        return reply != null ? reply.getTextContent() : "（无回复）";
    }

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