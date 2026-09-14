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

@RestController
@RequestMapping("/api/demo05")
public class Demo05ToolCallingController {

    private final String apiKey;

    public Demo05ToolCallingController(@Value("${agentscope.api-key}") String apiKey) {
        this.apiKey = apiKey;
    }

    @GetMapping("/weather")
    public String weather(
            @RequestParam(defaultValue = "北京") String city,
            @RequestParam(defaultValue = "请查一下这个城市的天气") String prompt) {
        Toolkit toolkit = new Toolkit();
        toolkit.registerTool(new WeatherTools());
        HarnessAgent agent = HarnessAgent.builder()
                .name("weather-agent")
                .sysPrompt("你可以调用 get_weather 工具查询天气，并把结果整理给用户。")
                .model("dashscope:qwen-plus")
                .toolkit(toolkit).maxIters(3)
                .workspace(Paths.get(".agentscope/workspace-demo05"))
                .build();
        String question = prompt + "：" + city;
        Msg reply = agent.call(List.<Msg>of(new UserMessage(question)), RuntimeContext.empty()).block();
        return reply != null ? reply.getTextContent() : "（无回复）";
    }
}