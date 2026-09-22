package com.xb.agentscope.controller.demo05;

import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.UserMessage;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.harness.agent.HarnessAgent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.nio.file.Paths;
import java.util.List;

/**
 * Demo05ToolCallingController - 工具调用（Tool Calling）示例控制器
 *
 * 演示如何让 Agent 自主调用外部工具（天气查询）来完成任务。
 * 核心概念：工具注册、系统提示词引导、Agent 自动决策调用工具并整合结果。
 *
 * @author ibqy
 */
@RestController
@RequestMapping("/api/demo05")
public class Demo05ToolCallingController {

    private final String apiKey;

    public Demo05ToolCallingController(@Value("${agentscope.api-key}") String apiKey) {
        this.apiKey = apiKey;
    }

    /**
     * 让 Agent 调用天气工具查询指定城市天气
     *
     * @param city 目标城市名称
     * @param prompt 附加提示语，引导 Agent 如何使用工具
     * @return Agent 整合工具结果后的回复文本
     */
    @GetMapping("/weather")
    public String weather(
            @RequestParam(defaultValue = "北京") String city,
            @RequestParam(defaultValue = "请查一下这个城市的天气") String prompt) {
        // 每次请求独立创建工具包和 Agent，避免全局状态干扰
        Toolkit toolkit = new Toolkit();
        toolkit.registerTool(new WeatherTools());
        HarnessAgent agent = HarnessAgent.builder()
                .name("weather-agent")
                .sysPrompt("你可以调用 get_weather 工具查询天气，并把结果整理给用户。")
                .model("dashscope:qwen-plus")
                // maxIters 限制 Agent 自主循环调用工具的最大次数，防止死循环
                .toolkit(toolkit).maxIters(3)
                .workspace(Paths.get(".agentscope/workspace-demo05"))
                .build();
        String question = prompt + "：" + city;
        Msg reply = agent.call(List.<Msg>of(new UserMessage(question)), RuntimeContext.empty()).block();
        return reply != null ? reply.getTextContent() : "（无回复）";
    }
}