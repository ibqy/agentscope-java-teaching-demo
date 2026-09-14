package com.xb.agentscope.controller.demo10;

import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import javax.script.ScriptEngineManager;

public class AssistantTools {

    @Tool(name = "calculator", description = "计算一个四则运算表达式的结果，例如 12*3+5")
    public String calculate(
            @ToolParam(name = "expression", description = "需要计算的表达式", required = true) String expression) {
        var engine = new ScriptEngineManager().getEngineByName("JavaScript");
        try {
            Object result = engine.eval(expression);
            return expression + " = " + result;
        } catch (Exception e) {
            return "无法计算表达式：" + expression + "，原因：" + e.getMessage();
        }
    }

    @Tool(name = "save_note", description = "把一条重要的备忘信息保存下来")
    public String saveNote(
            @ToolParam(name = "note", description = "要保存的备忘内容", required = true) String note) {
        return "已保存备忘：" + note;
    }
}