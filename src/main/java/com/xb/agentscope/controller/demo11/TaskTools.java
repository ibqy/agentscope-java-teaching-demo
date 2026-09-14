package com.xb.agentscope.controller.demo11;

import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class TaskTools {

    private final Map<Long, String> tasks = new ConcurrentHashMap<>();
    private final AtomicLong idCounter = new AtomicLong(1);

    @Tool(name = "create_task", description = "创建一个新任务并记录到任务表中")
    public String createTask(
            @ToolParam(name = "title", description = "任务标题，必须简短明确", required = true) String title,
            @ToolParam(name = "detail", description = "任务详情描述，可为空") String detail) {
        long id = idCounter.getAndIncrement();
        String detailText = detail == null || detail.isBlank() ? "（无）" : detail;
        tasks.put(id, "#" + id + " [待办] " + title + " | 详情：" + detailText);
        return "已创建任务 #" + id + "：" + title;
    }

    @Tool(name = "list_tasks", description = "列出当前已有的全部任务及其状态")
    public String listTasks() {
        if (tasks.isEmpty()) return "当前没有任务。";
        StringBuilder sb = new StringBuilder("当前任务列表：");
        tasks.values().forEach(t -> sb.append(System.lineSeparator()).append(" - ").append(t));
        return sb.toString();
    }

    @Tool(name = "complete_task", description = "把指定 id 的任务标记为已完成")
    public String completeTask(
            @ToolParam(name = "id", description = "要完成的任务 id", required = true) Long id) {
        String old = tasks.get(id);
        if (old == null) return "找不到任务 #" + id + "，请先创建或检查 id 是否正确。";
        tasks.put(id, old.replace("[待办]", "[已完成]"));
        return "任务 #" + id + " 已标记为已完成。";
    }
}