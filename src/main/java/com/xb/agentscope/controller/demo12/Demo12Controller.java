package com.xb.agentscope.controller.demo12;

import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.UserMessage;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.core.tool.mcp.McpClientBuilder;
import io.agentscope.core.tool.mcp.McpClientWrapper;
import io.agentscope.harness.agent.HarnessAgent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.nio.file.Paths;
import java.util.List;

/**
 * Demo12Controller - MCP（Model Context Protocol）工具集成示例控制器
 *
 * 演示如何通过 MCP 协议连接外部工具服务器，将 MCP 工具注册到 AgentScope 的 Toolkit 中，
 * 让 Agent 透明使用 MCP 生态中的海量工具。MCP 是连接 AI 应用与外部数据源/工具的标准协议。
 *
 * 本示例使用 StdIO 传输方式连接 @modelcontextprotocol/server-filesystem MCP 服务器，
 * Agent 可通过该工具操作指定目录下的文件。
 *
 * 三种传输方式：
 * - StdIO：本地子进程通信，适合本地工具（需安装 Node.js + npx）
 * - SSE：HTTP Server-Sent Events，适合远程工具服务器
 * - HTTP：Streamable HTTP，无状态请求/响应模式
 *
 * @author ibqy
 */
@RestController
@RequestMapping("/api/demo12")
public class Demo12Controller {

    /**
     * 通过 MCP 协议连接文件系统工具服务器，让 Agent 操作文件
     *
     * 工作流程：
     * 1. McpClientBuilder 创建 MCP 客户端，通过 StdIO 启动本地 MCP 服务器子进程
     * 2. 客户端与服务器握手，自动发现服务器提供的工具（如 read_file、write_file、list_directory）
     * 3. toolkit.registerMcpClient() 将发现的所有 MCP 工具注册到 Toolkit
     * 4. HarnessAgent 获得 MCP 工具能力，可自主决定调用哪个工具完成用户请求
     *
     * @param directory 允许 Agent 操作的目录路径（默认系统临时目录）
     * @param message   发给 Agent 的指令
     * @return Agent 整合 MCP 工具调用结果后的回复
     */
    @GetMapping("/filesystem")
    public String filesystem(
            @RequestParam(defaultValue = "/tmp") String directory,
            @RequestParam(defaultValue = "请列出当前目录下的所有文件") String message) {
        // Step 1: 通过 StdIO 传输启动 MCP 文件系统服务器子进程
        // npx -y 自动下载并运行 @modelcontextprotocol/server-filesystem
        // 需要本地安装 Node.js（>= 18）
        McpClientWrapper mcpClient = McpClientBuilder.create("fs-mcp")
                .stdioTransport("npx", "-y",
                        "@modelcontextprotocol/server-filesystem", directory)
                .buildAsync()
                .block();

        // Step 2: 将 MCP 客户端发现的所有工具注册到 Toolkit
        // registerMcpClient 会自动调用 MCP 的 tools/list 方法发现可用工具
        Toolkit toolkit = new Toolkit();
        toolkit.registerMcpClient(mcpClient).block();

        // Step 3: 构建拥有 MCP 工具能力的 Agent
        HarnessAgent agent = HarnessAgent.builder()
                .name("mcp-fs-agent")
                .sysPrompt("你是一位文件管理助手。你可以通过 MCP 工具操作文件系统中的文件，"
                        + "包括读取文件内容、写入文件、列出目录等。请根据用户需求合理使用工具。")
                .model("dashscope:qwen-plus")
                .toolkit(toolkit)
                .maxIters(5)
                .workspace(Paths.get(".agentscope/workspace-demo12"))
                .build();

        // Step 4: Agent 自主决定调用哪些 MCP 工具来完成任务
        Msg reply = agent.call(
                List.of(new UserMessage(message)),
                RuntimeContext.empty()).block();
        return reply != null ? reply.getTextContent() : "（无回复）";
    }

    /**
     * 演示 SSE 传输方式连接远程 MCP 工具服务器
     *
     * SSE 适合连接部署在远程的 MCP 工具服务器，通过 HTTP Server-Sent Events 流式通信。
     * 支持自定义请求头（如认证 Token）和查询参数。
     *
     * @param endpoint MCP 服务器的 SSE 端点 URL
     * @param message  发给 Agent 的指令
     * @return Agent 的回复文本
     */
    @GetMapping("/sse")
    public String sse(
            @RequestParam(defaultValue = "http://localhost:3001/sse") String endpoint,
            @RequestParam(defaultValue = "请列出你可以使用的所有工具") String message) {
        // SSE 传输方式：连接远程 MCP 服务器
        McpClientWrapper mcpClient = McpClientBuilder.create("remote-mcp")
                .sseTransport(endpoint)
                .buildAsync()
                .block();

        Toolkit toolkit = new Toolkit();
        toolkit.registerMcpClient(mcpClient).block();

        HarnessAgent agent = HarnessAgent.builder()
                .name("mcp-sse-agent")
                .sysPrompt("你是一位可以使用远程工具的助手。请根据用户需求合理调用工具。")
                .model("dashscope:qwen-plus")
                .toolkit(toolkit)
                .maxIters(3)
                .workspace(Paths.get(".agentscope/workspace-demo12-sse"))
                .build();

        Msg reply = agent.call(
                List.of(new UserMessage(message)),
                RuntimeContext.empty()).block();
        return reply != null ? reply.getTextContent() : "（无回复）";
    }
}
