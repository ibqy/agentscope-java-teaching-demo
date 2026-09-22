package com.xb.agentscope.controller.demo11;

import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * TaskTools - 任务管理工具类（CRUD 演示）
 *
 * 提供创建任务、列出任务、完成任务三个工具方法，
 * 演示 Agent 在对话中进行结构化数据管理的场景。
 * 教学要点：ConcurrentHashMap + AtomicLong 保证工具在并发调用下的数据一致性。
 *
 * @author ibqy
 */
public class TaskTools {

    // ConcurrentHashMap 保证多线程下任务数据的安全读写
    private final Map<Long, String> tasks = new ConcurrentHashMap<>();
    // AtomicLong 提供线程安全的自增 ID 生成，避免 id 冲突
    private final AtomicLong idCounter = new AtomicLong(1);

    /**
     * 创建一个新任务并存入任务表
     *
     * @param title  任务标题
     * @param detail 任务详情（可选）
     * @return 创建成功的提示信息
     */
    @Tool(name = "create_task", description = "创建一个新任务并记录到任务表中")
    public String createTask(
            @ToolParam(name = "title", description = "任务标题，必须简短明确", required = true) String title,
            @ToolParam(name = "detail", description = "任务详情描述，可为空") String detail) {
        long id = idCounter.getAndIncrement();
        String detailText = detail == null || detail.isBlank() ? "（无）" : detail;
        tasks.put(id, "#" + id + " [待办] " + title + " | 详情：" + detailText);
        return "已创建任务 #" + id + "：" + title;
    }

    /**
     * 列出当前所有任务及其状态
     *
     * @return 格式化的任务列表文本
     */
    @Tool(name = "list_tasks", description = "列出当前已有的全部任务及其状态")
    public String listTasks() {
        if (tasks.isEmpty()) return "当前没有任务。";
        StringBuilder sb = new StringBuilder("当前任务列表：");
        tasks.values().forEach(t -> sb.append(System.lineSeparator()).append(" - ").append(t));
        return sb.toString();
    }

    /**
     * 将指定 ID 的任务标记为已完成
     *
     * @param id 要完成的任务 ID
     * @return 操作结果提示
     */
    @Tool(name = "complete_task", description = "把指定 id 的任务标记为已完成")
    public String completeTask(
            @ToolParam(name = "id", description = "要完成的任务 id", required = true) Long id) {
        String old = tasks.get(id);
        if (old == null) return "找不到任务 #" + id + "，请先创建或检查 id 是否正确。";
        tasks.put(id, old.replace("[待办]", "[已完成]"));
        return "任务 #" + id + " 已标记为已完成。";
    }
}