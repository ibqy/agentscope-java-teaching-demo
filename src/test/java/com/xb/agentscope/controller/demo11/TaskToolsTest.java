package com.xb.agentscope.controller.demo11;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 任务管理工具单元测试
 *
 * 心得：任务管理涉及状态变更，测试必须覆盖：
 * 1. 创建任务（ID 自增）
 * 2. 列出任务（空/非空）
 * 3. 完成任务（状态转换）
 * 4. 错误处理（不存在的任务）
 */
class TaskToolsTest {

    private TaskTools tools;

    @BeforeEach
    void setUp() {
        tools = new TaskTools();
    }

    @Test
    void testCreateTaskWithDetail() {
        String result = tools.createTask("完成报告", "Q3 季度财务报告");
        
        assertTrue(result.contains("已创建任务"));
        assertTrue(result.contains("#1"));
        assertTrue(result.contains("完成报告"));
    }

    @Test
    void testCreateTaskWithoutDetail() {
        String result = tools.createTask("简单任务", null);
        
        assertTrue(result.contains("已创建任务"));
        assertTrue(result.contains("#1"));
    }

    @Test
    void testCreateMultipleTasksAutoIncrementId() {
        String result1 = tools.createTask("任务1", "详情1");
        String result2 = tools.createTask("任务2", "详情2");
        String result3 = tools.createTask("任务3", "详情3");
        
        assertTrue(result1.contains("#1"));
        assertTrue(result2.contains("#2"));
        assertTrue(result3.contains("#3"));
    }

    @Test
    void testListTasksWhenEmpty() {
        String result = tools.listTasks();
        assertEquals("当前没有任务。", result);
    }

    @Test
    void testListTasksWithMultipleTasks() {
        tools.createTask("任务1", "详情1");
        tools.createTask("任务2", "详情2");
        
        String result = tools.listTasks();
        
        assertTrue(result.contains("当前任务列表"));
        assertTrue(result.contains("任务1"));
        assertTrue(result.contains("任务2"));
        assertTrue(result.contains("#1"));
        assertTrue(result.contains("#2"));
    }

    @Test
    void testCompleteTaskSuccess() {
        tools.createTask("待完成任务", "详情");
        
        String result = tools.completeTask(1L);
        
        assertTrue(result.contains("任务 #1"));
        assertTrue(result.contains("已完成"));
        
        // 验证状态已变更
        String list = tools.listTasks();
        assertTrue(list.contains("[已完成]"));
        assertFalse(list.contains("[待办]"));
    }

    @Test
    void testCompleteTaskNotFound() {
        String result = tools.completeTask(999L);
        
        assertTrue(result.contains("找不到任务"));
        assertTrue(result.contains("#999"));
    }

    @Test
    void testTaskStateTransition() {
        // 创建任务
        tools.createTask("测试任务", "测试详情");
        String list1 = tools.listTasks();
        assertTrue(list1.contains("[待办]"));
        
        // 完成任务
        tools.completeTask(1L);
        String list2 = tools.listTasks();
        assertTrue(list2.contains("[已完成]"));
        assertFalse(list2.contains("[待办]"));
    }

    @Test
    void testTaskDetailPlaceholder() {
        // detail 为 null 或空时显示（无）
        tools.createTask("无详情任务", null);
        String list = tools.listTasks();
        assertTrue(list.contains("详情：（无）"));
        
        tools.createTask("空详情任务", "   ");
        list = tools.listTasks();
        // 第二个任务也应该是（无）
        assertTrue(list.contains("详情：（无）"));
    }
}
