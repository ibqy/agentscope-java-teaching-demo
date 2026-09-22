package com.xb.agentscope.controller.demo15;

import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.UserMessage;
import io.agentscope.core.permission.PermissionBehavior;
import io.agentscope.core.permission.PermissionContextState;
import io.agentscope.core.permission.PermissionMode;
import io.agentscope.core.permission.PermissionRule;
import io.agentscope.core.state.AgentState;
import io.agentscope.core.state.JsonFileAgentStateStore;
import io.agentscope.harness.agent.HarnessAgent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Paths;
import java.util.List;

/**
 * Demo15Controller - Plan Mode 与权限系统示例控制器
 *
 * 演示 AgentScope Java 2.0 的三大核心架构特性：
 * 1. Plan Mode：Agent 先规划再执行，适合复杂多步骤任务
 * 2. Permission System：细粒度控制工具执行权限（允许/拒绝/询问）
 * 3. RuntimeContext + AgentState：多用户会话隔离与状态持久化
 *
 * 这些特性让 AgentScope Java 从"能用的 Agent 框架"升级为"可生产部署的 Agent 框架"。
 *
 * @author ibqy
 */
@RestController
@RequestMapping("/api/demo15")
public class Demo15Controller {

    private final String apiKey;

    public Demo15Controller(@Value("${agentscope.api-key}") String apiKey) {
        this.apiKey = apiKey;
    }

    /**
     * Plan Mode 演示：Agent 先写计划再执行
     *
     * Plan Mode 的核心流程：
     * 1. 构建 Agent 时调用 enablePlanMode()
     * 2. 运行时调用 enterPlanMode() 进入规划模式
     * 3. Agent 会先输出执行计划（不执行工具）
     * 4. 确认计划后调用 exitPlanMode() 进入执行模式
     * 5. Agent 按计划逐步执行工具并返回结果
     *
     * @param task 要完成的任务
     * @return Agent 的回复（包含计划或执行结果）
     */
    @GetMapping("/plan")
    public String planMode(@RequestParam(defaultValue = "帮我查询北京今天的天气，然后计算 100 乘以 25") String task) {
        // Step 1: 构建支持 Plan Mode 的 Agent
        HarnessAgent planner = HarnessAgent.builder()
                .name("planner")
                .sysPrompt("你是一个谨慎的助手。在执行任务前，先列出你的执行计划步骤。")
                .model("dashscope:qwen-plus")
                .enablePlanMode()
                .planFileDirectory(".agentscope/plans")
                .workspace(Paths.get(".agentscope/workspace-demo15"))
                .build();

        // Step 2: 创建运行时上下文（标识会话和用户）
        RuntimeContext ctx = RuntimeContext.builder()
                .sessionId("plan-session-001")
                .userId("user-ibqy")
                .build();

        // Step 3: 进入 Plan Mode —— Agent 会先输出计划，不执行工具
        planner.enterPlanMode(ctx);

        Msg planReply = planner.call(task, ctx).block();
        String planText = planReply != null ? planReply.getTextContent() : "（无计划输出）";

        // Step 4: 退出 Plan Mode —— Agent 开始执行工具
        planner.exitPlanMode(ctx);

        Msg execReply = planner.call("请按照刚才的计划执行", ctx).block();
        String execText = execReply != null ? execReply.getTextContent() : "（无执行结果）";

        return "【规划阶段】\n" + planText + "\n\n【执行阶段】\n" + execText;
    }

    /**
     * Permission System 演示：配置工具执行权限规则
     *
     * PermissionContextState 允许你为每个工具配置权限规则：
     * - addAllowRule：自动允许执行（不询问用户）
     * - addDenyRule：自动拒绝执行
     * - addAskRule：每次执行前询问用户确认
     *
     * PermissionMode 控制整体行为：
     * - DEFAULT：按规则逐条匹配
     * - BYPASS：跳过所有权限检查（危险操作）
     * - ACCEPT_EDITS：自动允许所有编辑类操作
     * - DONT_ASK：自动拒绝所有需要询问的操作
     *
     * @param toolName 工具名称（如 "get_weather"）
     * @param behavior 权限行为（ALLOW / DENY / ASK）
     * @param message  发给 Agent 的消息
     * @return Agent 的回复
     */
    @GetMapping("/permission")
    public String permission(
            @RequestParam(defaultValue = "get_weather") String toolName,
            @RequestParam(defaultValue = "ALLOW") String behavior,
            @RequestParam(defaultValue = "查询北京天气") String message) {

        // Step 1: 解析权限行为
        PermissionBehavior permBehavior = PermissionBehavior.valueOf(behavior.toUpperCase());

        // Step 2: 构建权限规则
        PermissionRule rule = new PermissionRule(
                toolName,
                "允许执行天气查询工具",
                permBehavior,
                "demo15"
        );

        // Step 3: 构建权限上下文
        PermissionContextState permCtx = PermissionContextState.builder()
                .mode(PermissionMode.DEFAULT)
                .addAllowRule(toolName, rule)
                .build();

        // Step 4: 构建带权限配置的 Agent
        HarnessAgent guardedAgent = HarnessAgent.builder()
                .name("guarded-assistant")
                .sysPrompt("你是一个助手，可以查询天气。")
                .model("dashscope:qwen-plus")
                .permissionContext(permCtx)
                .workspace(Paths.get(".agentscope/workspace-demo15-perm"))
                .build();

        RuntimeContext ctx = RuntimeContext.builder()
                .sessionId("perm-session-001")
                .userId("user-ibqy")
                .build();

        Msg reply = guardedAgent.call(message, ctx).block();
        return "权限规则：工具=" + toolName + "，行为=" + permBehavior + "\n\n" +
               (reply != null ? reply.getTextContent() : "（无回复）");
    }

    /**
     * 多用户隔离演示：不同用户的会话互不干扰
     *
     * RuntimeContext 通过 sessionId + userId 实现会话隔离：
     * - 每个用户有独立的对话历史
     * - 每个会话有独立的工具执行上下文
     * - Agent 实例可以复用，状态按 sessionId 隔离
     *
     * @param userA 用户 A 的消息
     * @param userB 用户 B 的消息
     * @return 两个用户的回复（展示隔离效果）
     */
    @GetMapping("/multi-user")
    public String multiUser(
            @RequestParam(defaultValue = "我叫张三，请记住我的名字") String userA,
            @RequestParam(defaultValue = "我叫李四，请记住我的名字") String userB) {

        // 共享同一个 Agent 实例
        HarnessAgent sharedAgent = HarnessAgent.builder()
                .name("shared-assistant")
                .sysPrompt("你是一个助手，请记住用户的名字并在回复中称呼他们。")
                .model("dashscope:qwen-plus")
                .workspace(Paths.get(".agentscope/workspace-demo15-multi"))
                .build();

        // 用户 A 的上下文
        RuntimeContext ctxA = RuntimeContext.builder()
                .sessionId("session-A")
                .userId("user-A")
                .build();

        // 用户 B 的上下文
        RuntimeContext ctxB = RuntimeContext.builder()
                .sessionId("session-B")
                .userId("user-B")
                .build();

        // 两个用户同时与同一个 Agent 对话
        Msg replyA = sharedAgent.call(userA, ctxA).block();
        Msg replyB = sharedAgent.call(userB, ctxB).block();

        return "【用户 A（session-A）】\n" +
               (replyA != null ? replyA.getTextContent() : "（无回复）") +
               "\n\n【用户 B（session-B）】\n" +
               (replyB != null ? replyB.getTextContent() : "（无回复）") +
               "\n\n（两个会话完全隔离，Agent 不会混淆用户身份）";
    }

    /**
     * 状态持久化演示：保存和恢复 Agent 会话状态
     *
     * AgentState 封装了 Agent 的完整运行状态：
     * - sessionId / userId：会话标识
     * - summary：对话摘要
     * - context：消息历史
     * - curIter：当前迭代次数
     * - planModeContext / permissionContext：模式和权限状态
     *
     * AgentStateStore（如 JsonFileAgentStateStore）负责持久化：
     * - save(sessionId, userId, stateType, state)：保存状态
     * - get(sessionId, userId, stateType, clazz)：恢复状态
     * - exists(sessionId, userId)：检查是否存在
     * - listSessionIds(userId)：列出用户的所有会话
     *
     * @param userId 用户 ID
     * @param message 发给 Agent 的消息
     * @return Agent 的回复 + 状态保存结果
     */
    @GetMapping("/state")
    public String statePersistence(
            @RequestParam(defaultValue = "user-demo") String userId,
            @RequestParam(defaultValue = "你好，请做个自我介绍") String message) {

        // Step 1: 创建状态存储（JSON 文件持久化）
        JsonFileAgentStateStore stateStore = new JsonFileAgentStateStore(
                Paths.get(".agentscope/state-store")
        );

        // Step 2: 构建带状态存储的 Agent
        HarnessAgent statefulAgent = HarnessAgent.builder()
                .name("stateful-assistant")
                .sysPrompt("你是一个助手，请记住与用户的对话内容。")
                .model("dashscope:qwen-plus")
                .stateStore(stateStore)
                .workspace(Paths.get(".agentscope/workspace-demo15-state"))
                .build();

        // Step 3: 构建运行时上下文
        String sessionId = "state-session-" + userId;
        RuntimeContext ctx = RuntimeContext.builder()
                .sessionId(sessionId)
                .userId(userId)
                .build();

        // Step 4: 调用 Agent（状态会自动保存）
        Msg reply = statefulAgent.call(message, ctx).block();

        // Step 5: 检查状态是否已保存
        boolean stateExists = stateStore.exists(sessionId, userId);

        return "用户=" + userId + "，会话=" + sessionId + "\n" +
               "状态已持久化：" + stateExists + "\n\n" +
               (reply != null ? reply.getTextContent() : "（无回复）");
    }
}
