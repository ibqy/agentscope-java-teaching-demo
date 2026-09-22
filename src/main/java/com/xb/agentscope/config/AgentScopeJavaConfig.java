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

/**
 * AgentScopeJavaConfig - AgentScope 核心 Bean 配置中心
 *
 * 集中声明模型、工具包、智能体等 Spring Bean，是教学演示的"装配车间"。
 * 关键要点：理解 Bean 如何串联 API Key、模型、工具、记忆等组件，
 * 以及全局共享 Bean 与 Controller 中局部构建的区别。
 *
 * @author ibqy
 */
@Configuration
public class AgentScopeJavaConfig {

    @Value("${agentscope.api-key}")
    private String apiKey;

    @Value("${agentscope.model:}")
    private String model;

    @Value("${agentscope.dashscope-model:qwen-plus}")
    private String dashscopeModel;

    /**
     * 创建 DashScope 大语言模型实例
     *
     * 启用流式输出，使用 DashScope 专用的消息格式化器
     *
     * @return 配置好 API Key 和模型名的 DashScopeChatModel
     */
    @Bean
    public DashScopeChatModel dashScopeChatModel() {
        return DashScopeChatModel.builder()
                .apiKey(apiKey)
                .modelName(dashscopeModel)
                .stream(true)
                .formatter(new DashScopeChatFormatter())
                .build();
    }

    /**
     * 创建全局共享工具包
     *
     * 注册天气查询和计算/备忘工具，供多个 Demo 的 Agent 复用
     *
     * @return 包含所有已注册工具的 Toolkit 实例
     */
    @Bean
    public Toolkit sharedToolkit() {
        Toolkit toolkit = new Toolkit();
        toolkit.registerTool(new WeatherTools());
        toolkit.registerTool(new AssistantTools());
        return toolkit;
    }

    /**
     * 创建全局共享的助手智能体
     *
     * 配置系统提示词、工具包、记忆压缩策略和工作空间路径，
     * 被 Demo01/Demo03/Demo04 等多个控制器注入使用
     *
     * @param toolkit 由 Spring 注入的共享工具包
     * @return 配置完成的 HarnessAgent 实例
     */
    @Bean
    public HarnessAgent assistantAgent(Toolkit toolkit) {
        MemoryConfig memoryConfig = MemoryConfig.builder().model(model).build();
        // 当消息数或 token 数超过阈值时，自动压缩历史对话以节省上下文窗口
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