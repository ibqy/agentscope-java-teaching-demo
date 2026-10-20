package com.xb.agentscope.controller.demo16;

import io.agentscope.core.event.AgentEvent;
import io.agentscope.core.event.AgentEventType;
import io.agentscope.core.event.AgentStartEvent;
import io.agentscope.core.event.ExceedMaxItersEvent;
import io.agentscope.core.event.ModelCallEndEvent;
import io.agentscope.core.event.TextBlockDeltaEvent;
import io.agentscope.core.event.ThinkingBlockDeltaEvent;
import io.agentscope.core.event.ToolCallEndEvent;
import io.agentscope.core.event.ToolCallStartEvent;
import io.agentscope.core.model.ChatUsage;

import java.util.ArrayList;
import java.util.List;

/**
 * Agent 执行时间线 —— 把流式事件流整理成"可读的执行过程"
 *
 * <p>心得：AgentScope 的 {@code streamEvents()} 吐出的是细粒度事件流
 * （模型调用开始/结束、工具调用开始/结束、思考块、文本块、超迭代……），
 * 直接 `doOnNext(print)` 会得到一屏碎片。<b>事件系统的正确用法是分类聚合</b>：</p>
 *
 * <ul>
 *   <li>文本/思考增量：只计数不逐条入表——否则时间线被 token 级事件淹没</li>
 *   <li>模型调用：累计 Token 用量与耗时（{@link ModelCallEndEvent} 自带 {@link ChatUsage}）</li>
 *   <li>工具调用：记录工具名（可观测的核心维度：Agent 到底调了什么）</li>
 *   <li>兜底：未知事件类型走 default 分支，框架升级加新事件不至于崩</li>
 * </ul>
 *
 * <p>这个类不依赖 Spring 也不依赖 LLM——把事件对象喂进来就能跑，
 * 所以可以用真实事件类在离线单测里验证全部聚合逻辑
 * （参见 {@code AgentEventTimelineTest}）。</p>
 *
 * @author ibqy
 */
public class AgentEventTimeline {

    /** 时间线条目 */
    public record Entry(long seq, String type, String detail) {}

    /** 聚合统计 */
    public record Stats(int agentRuns, int modelCalls, int toolCalls,
                        long inputTokens, long outputTokens, long totalTokens,
                        double modelTimeSeconds, int textChars, int thinkingChars,
                        boolean exceededMaxIters, int totalEvents) {}

    private final List<Entry> entries = new ArrayList<>();

    private int agentRuns;
    private int modelCalls;
    private int toolCalls;
    private long inputTokens;
    private long outputTokens;
    private long totalTokens;
    private double modelTimeSeconds;
    private int textChars;
    private int thinkingChars;
    private boolean exceededMaxIters;
    private long seq;

    /**
     * 记录一个事件（时间线 + 统计一次性完成）
     *
     * @param event streamEvents() 吐出的事件；null 会记为一条 UNKNOWN
     *              （调用方用它做"流为空"的兜底信号，静默跳过会让人以为没执行）
     */
    public void record(AgentEvent event) {
        seq++;
        if (event == null) {
            add("UNKNOWN", "空事件（事件流异常或未产生任何事件）");
            return;
        }
        AgentEventType type = event.getType();
        if (type == null) {
            add("UNKNOWN", "事件无类型");
            return;
        }

        switch (type) {
            case AGENT_START -> {
                agentRuns++;
                String name = event instanceof AgentStartEvent s ? s.getName() : null;
                add("AGENT_START", name == null ? "Agent 启动" : "Agent 启动: " + name);
            }
            case MODEL_CALL_START -> {
                modelCalls++;
                add("MODEL_CALL_START", "第 " + modelCalls + " 次模型调用");
            }
            case MODEL_CALL_END -> {
                // 模型结束事件自带用量与耗时——这是"真实计量"的数据源，
                // 比事后按字符估算准得多（还能拿到缓存/推理 token）
                if (event instanceof ModelCallEndEvent end && end.getUsage() != null) {
                    ChatUsage usage = end.getUsage();
                    inputTokens += usage.getInputTokens();
                    outputTokens += usage.getOutputTokens();
                    totalTokens += usage.getTotalTokens();
                    modelTimeSeconds += usage.getTime();
                    add("MODEL_CALL_END", String.format(
                            "第 %d 次调用结束: 输入 %d / 输出 %d / 合计 %d tokens, 耗时 %.2fs",
                            modelCalls, usage.getInputTokens(), usage.getOutputTokens(),
                            usage.getTotalTokens(), usage.getTime()));
                } else {
                    add("MODEL_CALL_END", "第 " + modelCalls + " 次调用结束（无用量信息）");
                }
            }
            case TOOL_CALL_START -> {
                toolCalls++;
                String tool = event instanceof ToolCallStartEvent s ? s.getToolCallName() : null;
                add("TOOL_CALL_START", "调用工具: " + (tool == null ? "未知" : tool));
            }
            case TOOL_CALL_END -> {
                String tool = event instanceof ToolCallEndEvent e ? e.getToolCallName() : null;
                add("TOOL_CALL_END", "工具返回: " + (tool == null ? "未知" : tool));
            }
            case EXCEED_MAX_ITERS -> {
                exceededMaxIters = true;
                String detail = event instanceof ExceedMaxItersEvent e
                        ? "已达最大迭代 " + e.getMaxIters() + "（当前第 " + e.getCurrentIter() + " 轮）"
                        : "已达最大迭代次数";
                add("EXCEED_MAX_ITERS", detail);
            }
            case TEXT_BLOCK_DELTA ->
                // 增量文本只计数：逐条入表会把时间线淹没在 token 级事件里
                textChars += event instanceof TextBlockDeltaEvent d && d.getDelta() != null
                        ? d.getDelta().length() : 0;
            case THINKING_BLOCK_DELTA ->
                thinkingChars += event instanceof ThinkingBlockDeltaEvent d && d.getDelta() != null
                        ? d.getDelta().length() : 0;
            case AGENT_END -> add("AGENT_END", "Agent 结束");
            case AGENT_RESULT -> add("AGENT_RESULT", "产出最终结果");
            case REQUIRE_USER_CONFIRM -> add("REQUIRE_USER_CONFIRM", "等待用户确认（HITL 审批点）");
            default -> add(type.name(), "（未分类事件）");
        }
    }

    /** 当前统计快照 */
    public Stats stats() {
        return new Stats(agentRuns, modelCalls, toolCalls, inputTokens, outputTokens,
                totalTokens, modelTimeSeconds, textChars, thinkingChars, exceededMaxIters, (int) seq);
    }

    /** 渲染为可读文本（时间线 + 统计） */
    public String render() {
        StringBuilder sb = new StringBuilder("=== Agent 执行时间线 ===\n");
        for (Entry entry : entries) {
            sb.append(String.format("%3d. %-18s %s%n", entry.seq(), entry.type(), entry.detail()));
        }
        Stats s = stats();
        sb.append(String.format("""
                        ---
                        事件总数: %d（文本增量 %d 字符、思考增量 %d 字符未逐条列出）
                        Agent 启动: %d 次 | 模型调用: %d 次 | 工具调用: %d 次
                        Token: 输入 %d / 输出 %d / 合计 %d，模型累计耗时 %.2fs
                        超迭代终止: %s
                        """,
                s.totalEvents(), s.textChars(), s.thinkingChars(),
                s.agentRuns(), s.modelCalls(), s.toolCalls(),
                s.inputTokens(), s.outputTokens(), s.totalTokens(), s.modelTimeSeconds(),
                s.exceededMaxIters() ? "是" : "否"));
        return sb.toString();
    }

    private void add(String type, String detail) {
        entries.add(new Entry(seq, type, detail));
    }
}
