package com.xb.agentscope.controller.demo13;

import io.agentscope.core.a2a.agent.A2aAgent;
import io.agentscope.core.a2a.agent.card.WellKnownAgentCardResolver;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.UserMessage;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.Map;

/**
 * Demo13Controller - A2A（Agent-to-Agent）跨智能体通信示例控制器
 *
 * 演示 A2A 协议的客户端用法：通过 A2aAgent 调用远程 A2A 服务。
 * A2A 是 AgentScope 对跨智能体通信标准协议的支持，让不同框架、不同语言的 Agent
 * 能够互相发现、互相调用，实现真正的多智能体 interoperability。
 *
 * 本示例展示两种远程 AgentCard 发现方式：
 * 1. WellKnown 路径发现：从目标服务器的 /.well-known/agent-card.json 自动获取
 * 2. 直接指定 AgentCard：手动构建 AgentCard 对象（适合已知目标信息的场景）
 *
 * 服务端配置见 application.yml 中 agentscope.a2a.server 部分，
 * 生产环境中 A2A 服务端通常部署为独立的微服务。
 *
 * @author ibqy
 */
@RestController
@RequestMapping("/api/demo13")
public class Demo13Controller {

    /**
     * 通过 Well-Known 路径自动发现并调用远程 A2A Agent
     *
     * WellKnownAgentCardResolver 会自动访问目标服务器的
     * /.well-known/agent-card.json 端点，获取该服务器的 AgentCard
     * （包含名称、描述、技能列表、通信端点等元信息），然后 A2aAgent
     * 根据 AgentCard 中的信息构建 A2A 协议请求并发送。
     *
     * @param remoteUrl 远程 A2A 服务的基础 URL（如 http://localhost:9090）
     * @param message   发给远程 Agent 的消息
     * @return 远程 Agent 的回复文本
     */
    @GetMapping("/call")
    public String callRemote(
            @RequestParam(defaultValue = "http://localhost:9090") String remoteUrl,
            @RequestParam(defaultValue = "你好，请做个自我介绍") String message) {
        // Step 1: 创建 WellKnown 解析器，自动发现远程 Agent 的 AgentCard
        WellKnownAgentCardResolver resolver = WellKnownAgentCardResolver.builder()
                .baseUrl(remoteUrl)
                .relativeCardPath("/.well-known/agent-card.json")
                .build();

        // Step 2: 构建 A2A Agent —— 远程 A2A 服务的本地代理
        A2aAgent agent = A2aAgent.builder()
                .name("remote-caller")
                .agentCardResolver(resolver)
                .build();

        // Step 3: 像调用本地 Agent 一样调用远程 Agent
        try {
            Msg response = agent.call(List.of(new UserMessage(message))).block();
            return response != null ? response.getTextContent() : "（无回复）";
        } catch (Exception e) {
            return "A2A 调用失败（请确认远程 A2A 服务已启动）：" + e.getMessage();
        }
    }

    /**
     * 演示自定义请求头调用远程 A2A Agent（如携带认证信息）
     *
     * 生产环境中，A2A 服务通常需要认证。通过 WellKnownAgentCardResolver
     * 的 headers 参数可以传递 Authorization 等自定义请求头。
     *
     * @param remoteUrl 远程 A2A 服务的基础 URL
     * @param token     认证 Token（Bearer Token 方式）
     * @param message   发给远程 Agent 的消息
     * @return 远程 Agent 的回复文本
     */
    @GetMapping("/auth")
    public String callWithAuth(
            @RequestParam(defaultValue = "http://localhost:9090") String remoteUrl,
            @RequestParam(defaultValue = "demo-token") String token,
            @RequestParam(defaultValue = "你好") String message) {
        // 携带认证请求头的 WellKnown 解析器
        WellKnownAgentCardResolver resolver = WellKnownAgentCardResolver.builder()
                .baseUrl(remoteUrl)
                .relativeCardPath("/.well-known/agent-card.json")
                .authHeaders(Map.of("Authorization", "Bearer " + token))
                .build();

        A2aAgent agent = A2aAgent.builder()
                .name("auth-caller")
                .agentCardResolver(resolver)
                .build();

        try {
            Msg response = agent.call(List.of(new UserMessage(message))).block();
            return response != null ? response.getTextContent() : "（无回复）";
        } catch (Exception e) {
            return "A2A 认证调用失败：" + e.getMessage();
        }
    }
}
