package com.xb.agentscope.controller.demo01;

import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.UserMessage;
import io.agentscope.harness.agent.HarnessAgent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

/**
 * Demo01HelloAgentController - 第一个 Hello Agent 示例控制器
 *
 * 演示最基本的智能体调用方式：通过注入全局 HarnessAgent，
 * 将用户消息发送给 Agent 并获取回复。重点理解 call() 方法的使用模式。
 *
 * @author ibqy
 */
@RestController
@RequestMapping("/api/demo01")
public class Demo01HelloAgentController {

    private final HarnessAgent assistantAgent;

    public Demo01HelloAgentController(HarnessAgent assistantAgent) {
        this.assistantAgent = assistantAgent;
    }

    /**
     * 发送消息给 Agent 并获取回复
     *
     * @param message 用户输入的消息内容
     * @return Agent 的文本回复
     */
    @GetMapping("/hello")
    public String hello(@RequestParam(defaultValue = "你好，请做个自我介绍") String message) {
        UserMessage userMessage = new UserMessage(message);
        // call() 返回 Flux，block() 阻塞等待最终结果；生产环境应改用异步方式
        Msg reply = assistantAgent.call(List.<Msg>of(userMessage), RuntimeContext.empty()).block();
        return reply != null ? reply.getTextContent() : "（无回复）";
    }
}