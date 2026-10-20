package com.xb.agentscope.controller.demo16;

import io.agentscope.core.event.AgentStartEvent;
import io.agentscope.core.event.ExceedMaxItersEvent;
import io.agentscope.core.event.ModelCallEndEvent;
import io.agentscope.core.event.ModelCallStartEvent;
import io.agentscope.core.event.TextBlockDeltaEvent;
import io.agentscope.core.event.ThinkingBlockDeltaEvent;
import io.agentscope.core.event.ToolCallEndEvent;
import io.agentscope.core.event.ToolCallStartEvent;
import io.agentscope.core.model.ChatUsage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AgentEventTimeline 测试 —— 用真实事件对象验证聚合逻辑
 *
 * 心得：可观测逻辑（分类、累计、渲染）最容易出"看起来对"的 bug——
 * 增量事件该计数的计入表、null usage 该跳过的别 NPE。
 * 这些分支不依赖 LLM，必须离线全覆盖。
 *
 * @author ibqy
 */
@DisplayName("AgentEventTimeline 执行时间线")
class AgentEventTimelineTest {

    /** 构造一次典型执行：启动 → 模型调用 → 工具调用 → 模型调用 → 结束 */
    private AgentEventTimeline typicalRun() {
        AgentEventTimeline timeline = new AgentEventTimeline();
        timeline.record(new AgentStartEvent("reply-1", "assistant", "助手"));
        timeline.record(new ModelCallStartEvent("reply-1"));
        timeline.record(new ToolCallStartEvent("reply-1", "call-1", "get_weather"));
        timeline.record(new ToolCallEndEvent("reply-1", "call-1", "get_weather"));
        timeline.record(new ModelCallEndEvent("reply-1",
                new ChatUsage(120, 35, 155, 0.8)));
        timeline.record(new TextBlockDeltaEvent("reply-1", "block-1", "北京今天多云，"));
        timeline.record(new TextBlockDeltaEvent("reply-1", "block-1", "气温 26 度。"));
        return timeline;
    }

    @Test
    @DisplayName("事件分类计数：模型/工具/启动各自归位")
    void countsEventsByCategory() {
        var stats = typicalRun().stats();

        assertThat(stats.agentRuns()).isEqualTo(1);
        assertThat(stats.modelCalls()).isEqualTo(1);
        assertThat(stats.toolCalls()).isEqualTo(1);
        // 7 条：启动 + 模型开始 + 工具开始 + 工具结束 + 模型结束 + 2 条文本增量
        assertThat(stats.totalEvents()).isEqualTo(7);
    }

    @Test
    @DisplayName("Token 用量来自 ModelCallEndEvent，不是字符估算")
    void accumulatesRealUsage() {
        var stats = typicalRun().stats();

        assertThat(stats.inputTokens()).isEqualTo(120);
        assertThat(stats.outputTokens()).isEqualTo(35);
        assertThat(stats.totalTokens()).isEqualTo(155);
        assertThat(stats.modelTimeSeconds()).isEqualTo(0.8);
    }

    @Test
    @DisplayName("文本增量只计数不入表（时间线不被 token 级事件淹没）")
    void textDeltasAreCountedNotListed() {
        var timeline = typicalRun();
        var stats = timeline.stats();

        assertThat(stats.textChars()).isEqualTo("北京今天多云，气温 26 度。".length());
        // 时间线条目里不应出现 TEXT_BLOCK_DELTA，只在统计里体现字符数
        assertThat(timeline.render()).doesNotContain("TEXT_BLOCK_DELTA");
        assertThat(timeline.render()).contains("文本增量 15 字符");
    }

    @Test
    @DisplayName("工具名进入时间线（可观测的核心维度）")
    void toolNamesAppearInTimeline() {
        String render = typicalRun().render();

        assertThat(render).contains("TOOL_CALL_START");
        assertThat(render).contains("get_weather");
        assertThat(render).contains("TOOL_CALL_END");
    }

    @Test
    @DisplayName("多轮模型调用累计（一次请求可能含多次模型往返）")
    void multipleModelCallsAccumulate() {
        AgentEventTimeline timeline = new AgentEventTimeline();
        timeline.record(new ModelCallStartEvent("r1"));
        timeline.record(new ModelCallEndEvent("r1", new ChatUsage(100, 10, 110, 0.5)));
        timeline.record(new ModelCallStartEvent("r1"));
        timeline.record(new ModelCallEndEvent("r1", new ChatUsage(200, 20, 220, 0.7)));

        var stats = timeline.stats();
        assertThat(stats.modelCalls()).isEqualTo(2);
        assertThat(stats.inputTokens()).isEqualTo(300);
        assertThat(stats.totalTokens()).isEqualTo(330);
        assertThat(stats.modelTimeSeconds()).isCloseTo(1.2, org.assertj.core.api.Assertions.within(0.001));
    }

    @Test
    @DisplayName("无用量信息的 MODEL_CALL_END 不 NPE，按 0 计")
    void nullUsageIsSafe() {
        AgentEventTimeline timeline = new AgentEventTimeline();
        timeline.record(new ModelCallStartEvent("r1"));
        timeline.record(new ModelCallEndEvent("r1", null));

        var stats = timeline.stats();
        assertThat(stats.modelCalls()).isEqualTo(1);
        assertThat(stats.totalTokens()).isZero();
        assertThat(timeline.render()).contains("无用量信息");
    }

    @Test
    @DisplayName("超迭代事件被标记（死循环熔断的可观测信号）")
    void exceedMaxItersIsFlagged() {
        AgentEventTimeline timeline = new AgentEventTimeline();
        timeline.record(new ExceedMaxItersEvent("r1", 10, 10));

        assertThat(timeline.stats().exceededMaxIters()).isTrue();
        assertThat(timeline.render()).contains("EXCEED_MAX_ITERS");
        assertThat(timeline.render()).contains("最大迭代 10");
    }

    @Test
    @DisplayName("null 事件容错（流里不应有 null，但不该因此崩）")
    void nullEventIsSafe() {
        AgentEventTimeline timeline = new AgentEventTimeline();
        timeline.record(null);

        assertThat(timeline.stats().totalEvents()).isEqualTo(1);
        assertThat(timeline.render()).contains("UNKNOWN");
    }

    @Test
    @DisplayName("思考增量同样只计数")
    void thinkingDeltasAreCounted() {
        AgentEventTimeline timeline = new AgentEventTimeline();
        timeline.record(new ThinkingBlockDeltaEvent("r1", "tb-1", "先查天气再判断"));

        assertThat(timeline.stats().thinkingChars()).isEqualTo("先查天气再判断".length());
        assertThat(timeline.render()).doesNotContain("THINKING_BLOCK_DELTA");
    }
}
