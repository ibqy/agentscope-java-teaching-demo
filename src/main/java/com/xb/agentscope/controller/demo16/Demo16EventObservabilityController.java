package com.xb.agentscope.controller.demo16;

import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.AgentEvent;
import io.agentscope.core.message.UserMessage;
import io.agentscope.harness.agent.HarnessAgent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Demo16EventObservabilityController - 事件驱动可观测示例控制器
 *
 * 演示 AgentScope Java 2.0 的事件系统（streamEvents 全事件订阅）：
 * 把细粒度事件流聚合成"执行时间线"与"Token 用量统计"。
 *
 * 与 demo04 的分工：
 * - demo04 只关心 TEXT_BLOCK_DELTA（把文本拼出来给用户看）——面向 C 端体验
 * - demo16 关心全部事件类型（模型调用/工具调用/迭代上限/HITL 确认点）——面向可观测与计量
 *
 * @author ibqy
 */
@RestController
@RequestMapping("/api/demo16")
public class Demo16EventObservabilityController {

    private final HarnessAgent assistantAgent;

    public Demo16EventObservabilityController(HarnessAgent assistantAgent) {
        this.assistantAgent = assistantAgent;
    }

    /**
     * 执行时间线：跑一次任务，把全过程事件整理成可读时间线
     *
     * 观察要点（对比 demo04 的"只拼文本"）：
     * 1. AGENT_START/END —— 任务边界（一次请求可能包含多轮模型调用）
     * 2. MODEL_CALL_START/END —— 模型往返次数与每次的 Token 用量/耗时
     * 3. TOOL_CALL_START/END —— Agent 实际调用了哪些工具（幻觉调用一目了然）
     * 4. EXCEED_MAX_ITERS —— 迭代上限熔断（防死循环的最后防线）
     * 5. TEXT/THINKING 增量只计数不入表——否则时间线被 token 级事件淹没
     *
     * @param message 用户输入的消息（默认问题会触发天气工具，便于观察工具事件）
     * @return 执行时间线文本
     */
    @GetMapping("/timeline")
    public String timeline(@RequestParam(defaultValue = "北京今天天气怎么样？需要带伞吗？") String message) {
        AgentEventTimeline timeline = new AgentEventTimeline();
        runAndCollect(message, timeline);
        return timeline.render();
    }

    /**
     * Token 用量统计：从 ModelCallEndEvent 累计真实用量
     *
     * 为什么不用字符数估算：ModelCallEndEvent 自带 ChatUsage
     * （输入/输出/合计/缓存/推理 token + 耗时），是模型返回的真实计量，
     * 按字符除 2 估算在中文场景误差能到 50% 以上。
     *
     * @param message 用户输入的消息
     * @return 用量统计文本
     */
    @GetMapping("/usage")
    public String usage(@RequestParam(defaultValue = "北京今天天气怎么样？") String message) {
        AgentEventTimeline timeline = new AgentEventTimeline();
        runAndCollect(message, timeline);

        AgentEventTimeline.Stats s = timeline.stats();
        return """
                === Token 用量统计（来自 ModelCallEndEvent）===
                模型调用次数: %d
                输入 tokens : %d
                输出 tokens : %d
                合计 tokens : %d
                模型累计耗时: %.2f 秒
                工具调用次数: %d（调了哪些工具看 /api/demo16/timeline）
                文本输出字符: %d / 思考字符: %d
                超迭代终止: %s
                """.formatted(s.modelCalls(), s.inputTokens(), s.outputTokens(), s.totalTokens(),
                s.modelTimeSeconds(), s.toolCalls(), s.textChars(), s.thinkingChars(),
                s.exceededMaxIters() ? "是" : "否");
    }

    /**
     * 跑一次任务并把事件喂进时间线
     *
     * <p>streamEvents 是"冷"流：collectList().block() 才会真正订阅执行。
     * doOnNext 里只做轻量聚合，不打断事件流。</p>
     */
    private void runAndCollect(String message, AgentEventTimeline timeline) {
        List<AgentEvent> events = assistantAgent.streamEvents(
                        new UserMessage(message), RuntimeContext.empty())
                .doOnNext(timeline::record)
                .collectList()
                .block();
        if (events == null || events.isEmpty()) {
            timeline.record(null);  // 触发一条 UNKNOWN，避免返回空时间线让人误解
        }
    }
}
