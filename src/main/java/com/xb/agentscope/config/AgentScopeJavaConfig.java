package com.xb.agentscope.config;

import com.xb.agentscope.controller.demo05.WeatherTools;
import com.xb.agentscope.controller.demo10.AssistantTools;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.extensions.model.dashscope.DashScopeChatModel;
import io.agentscope.extensions.model.dashscope.formatter.DashScopeChatFormatter;
import io.agentscope.harness.agent.HarnessAgent;
import io.agentscope.harness.agent.memory.MemoryConfig;
import io.agentscope.harness.agent.memory.compaction.CompactionConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.nio.file.Paths;

@Configuration
public class AgentScopeJavaConfig {

    @Value("${agentscope.api-key}")
    private String apiKey;

    @Value("${agentscope.model:}")
    private String model;

    @Value("${agentscope.dashscope-model:qwen-plus}")
    private String dashscopeModel;

    @Bean
    public DashScopeChatModel dashScopeChatModel() {
        return DashScopeChatModel.builder()
                .apiKey(apiKey)
                .modelName(dashscopeModel)
                .stream(true)
                .formatter(new DashScopeChatFormatter())
                .build();
    }

    @Bean
    public Toolkit sharedToolkit() {
        Toolkit toolkit = new Toolkit();
        toolkit.registerTool(new WeatherTools());
        toolkit.registerTool(new AssistantTools());
        return toolkit;
    }

    @Bean
    public HarnessAgent assistantAgent(Toolkit toolkit) {
        MemoryConfig memoryConfig = MemoryConfig.builder().model(model).build();
        CompactionConfig compactionConfig = CompactionConfig.builder()
                .triggerMessages(10).triggerTokens(2048).keepMessages(5).build();
        return HarnessAgent.builder()
                .name("assistant")
                .sysPrompt("你是一位耐心、专业、乐于助人的中文助手。请用简洁清晰的语言回答用户问题。")
                .model(model)
                .toolkit(toolkit)
                .workspace(Paths.get(".agentscope/workspace"))
                .memory(memoryConfig)
                .compaction(compactionConfig)
                .build();
    }
}