package com.xb.agentscope.controller.demo10;

import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import javax.script.ScriptEngineManager;

/**
 * AssistantTools - 计算与备忘工具类
 *
 * 提供四则运算计算器和备忘保存两个工具，演示 Agent 多工具协作场景。
 * 教学要点：多个 @Tool 方法如何在同一个类中共存，Agent 按需选择调用。
 *
 * @author ibqy
 */
public class AssistantTools {

    /**
     * 使用 JavaScript 引擎计算四则运算表达式
     *
     * @param expression 四则运算表达式（如 "12*3+5"）
     * @return 计算结果字符串，或错误提示
     */
    @Tool(name = "calculator", description = "计算一个四则运算表达式的结果，例如 12*3+5")
    public String calculate(
            @ToolParam(name = "expression", description = "需要计算的表达式", required = true) String expression) {
        // 依赖 JavaScript 引擎，JDK 15+ 已移除 Nashorn，需注意兼容性
        var engine = new ScriptEngineManager().getEngineByName("JavaScript");
        try {
            Object result = engine.eval(expression);
            return expression + " = " + result;
        } catch (Exception e) {
            return "无法计算表达式：" + expression + "，原因：" + e.getMessage();
        }
    }

    /**
     * 保存一条备忘信息
     *
     * @param note 备忘内容
     * @return 确认保存的提示文本
     */
    @Tool(name = "save_note", description = "把一条重要的备忘信息保存下来")
    public String saveNote(
            @ToolParam(name = "note", description = "要保存的备忘内容", required = true) String note) {
        return "已保存备忘：" + note;
    }
}