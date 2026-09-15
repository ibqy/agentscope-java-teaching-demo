# Demo06：多智能体协作

## 知识点

- `SubagentDeclaration` 声明子智能体
- Director 协调模式
- 子智能体的调度机制

## 核心概念

AgentScope 支持**多智能体协作**——一个导演 Agent（Director）可以调度多个子智能体（SubAgent）协同完成任务。每个子智能体只需声明其名称和能力描述，Director 会根据任务自动将工作分派给合适的子智能体。

## 声明子智能体

使用 `SubagentDeclaration.builder()` 声明子智能体，只需指定 name、description 和 model：

```java
SubagentDeclaration researcher = SubagentDeclaration.builder()
    .name("researcher")
    .description("擅长检索与研究信息，能给出严谨的研究结论")
    .model("dashscope:qwen-plus")
    .build();

SubagentDeclaration translator = SubagentDeclaration.builder()
    .name("translator")
    .description("擅长把中文内容翻译成地道英文")
    .model("dashscope:qwen-plus")
    .build();
```

> `description` 字段是关键——Director 靠它判断哪个子智能体适合当前任务。

## 创建 Director

将子智能体注册到 Director，并配置其系统提示和迭代次数：

```java
HarnessAgent director = HarnessAgent.builder()
    .name("director")
    .sysPrompt("你是一个多智能体的协调者。你可以研究主题，也可以让翻译官把结果翻译成英文。")
    .model("dashscope:qwen-plus")
    .subagent(researcher)
    .subagent(translator)
    .maxIters(5)   // 最多迭代 5 轮
    .workspace(Paths.get(".agentscope/workspace-demo06"))
    .build();
```

## 执行协作任务

调用方式与普通 Agent 相同，但 Director 内部会自动调度子智能体：

```java
Msg reply = director.call(
    List.<Msg>of(new UserMessage("请研究主题「" + topic + "」并给出要点。")),
    RuntimeContext.empty()).block();
```

## 调度机制

Director 的调度遵循以下原则：

1. **理解任务** — 分析用户请求，拆解子任务
2. **匹配子智能体** — 根据 description 找到最合适的子智能体
3. **委派执行** — 将子任务发送给对应子智能体
4. **汇总结果** — 整合各子智能体输出，生成最终回复

```
用户请求 → Director 分析 → 匹配 SubAgent → 委派任务 → 收集结果 → 汇总回复
                │
         ┌──────┴──────┐
      researcher    translator
```

## 小结

- `SubagentDeclaration` 只需 name + description + model
- Director 靠 description 自动匹配合适的子智能体
- `.subagent(...)` 可链式注册多个子智能体
- `maxIters` 控制最大迭代轮次，防止无限循环
