package com.xb.agentscope.controller.demo06;

import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.UserMessage;
import io.agentscope.harness.agent.HarnessAgent;
import io.agentscope.harness.agent.subagent.SubagentDeclaration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.nio.file.Paths;
import java.util.List;

/**
 * Demo06SubagentController - 多智能体协作（Subagent）示例控制器
 *
 * 演示如何声明多个子智能体（SubagentDeclaration），由主智能体（director）
 * 根据任务自动协调子智能体完成研究和翻译，展示 Agent 编排的核心模式。
 *
 * @author ibqy
 */
@RestController
@RequestMapping("/api/demo06")
public class Demo06SubagentController {

    private final String apiKey;

    public Demo06SubagentController(@Value("${agentscope.api-key}") String apiKey) {
        this.apiKey = apiKey;
    }

    /**
     * 由 director 协调研究员和翻译官完成主题研究
     *
     * @param topic 研究主题
     * @return director 整合后的最终回复
     */
    @GetMapping("/team")
    public String team(@RequestParam(defaultValue = "AgentScope") String topic) {
        // 声明子智能体：description 是 director 决定是否委派任务给它的依据
        SubagentDeclaration researcher = SubagentDeclaration.builder()
                .name("researcher").description("擅长检索与研究信息，能给出严谨的研究结论")
                .model("dashscope:qwen-plus").build();
        SubagentDeclaration translator = SubagentDeclaration.builder()
                .name("translator").description("擅长把中文内容翻译成地道英文")
                .model("dashscope:qwen-plus").build();
        // director 作为协调者，持有子智能体并决定调用顺序
        HarnessAgent director = HarnessAgent.builder()
                .name("director")
                .sysPrompt("你是一个多智能体的协调者。你可以研究主题，也可以让翻译官把结果翻译成英文。")
                .model("dashscope:qwen-plus")
                .subagent(researcher).subagent(translator).maxIters(5)
                .workspace(Paths.get(".agentscope/workspace-demo06"))
                .build();
        Msg reply = director.call(
                List.<Msg>of(new UserMessage("请研究主题「" + topic + "」并给出要点。")),
                RuntimeContext.empty()).block();
        return reply != null ? reply.getTextContent() : "（无回复）";
    }
}