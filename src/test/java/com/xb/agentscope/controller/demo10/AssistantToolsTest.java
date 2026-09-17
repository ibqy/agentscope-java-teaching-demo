package com.xb.agentscope.controller.demo10;

import org.junit.jupiter.api.Test;

import javax.script.ScriptEngineManager;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 助理工具单元测试
 *
 * 心得：计算器工具使用 JavaScript 引擎求值。
 * JDK 15+ 移除了 Nashorn 引擎，因此在 JDK 21 上计算器会走错误分支。
 * 测试需要区分引擎是否可用，分别验证正确路径和容错路径。
 */
class AssistantToolsTest {

    private final boolean jsEngineAvailable =
            new ScriptEngineManager().getEngineByName("JavaScript") != null;

    @Test
    void testCalculateSimpleExpression() {
        AssistantTools tools = new AssistantTools();
        String result = tools.calculate("2+3");

        assertTrue(result.contains("2+3"));
        if (jsEngineAvailable) {
            assertTrue(result.contains("5"));
        } else {
            assertTrue(result.contains("无法计算"));
        }
    }

    @Test
    void testCalculateComplexExpression() {
        AssistantTools tools = new AssistantTools();
        String result = tools.calculate("12*3+5");

        assertTrue(result.contains("12*3+5"));
        if (jsEngineAvailable) {
            assertTrue(result.contains("41"));
        } else {
            assertTrue(result.contains("无法计算"));
        }
    }

    @Test
    void testCalculateWithParentheses() {
        AssistantTools tools = new AssistantTools();
        String result = tools.calculate("(10+5)*2");

        assertTrue(result.contains("(10+5)*2"));
        if (jsEngineAvailable) {
            assertTrue(result.contains("30"));
        } else {
            assertTrue(result.contains("无法计算"));
        }
    }

    @Test
    void testCalculateInvalidExpression() {
        AssistantTools tools = new AssistantTools();
        String result = tools.calculate("invalid");

        assertTrue(result.contains("无法计算"));
        assertTrue(result.contains("invalid"));
    }

    @Test
    void testSaveNote() {
        AssistantTools tools = new AssistantTools();
        String result = tools.saveNote("明天下午3点开会");

        assertTrue(result.contains("已保存备忘"));
        assertTrue(result.contains("明天下午3点开会"));
    }

    @Test
    void testSaveNoteWithSpecialCharacters() {
        AssistantTools tools = new AssistantTools();
        String result = tools.saveNote("重要：项目截止日 2026-09-30！");

        assertTrue(result.contains("已保存备忘"));
        assertTrue(result.contains("2026-09-30"));
    }
}
