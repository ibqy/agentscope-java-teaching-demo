# Demo05：工具调用 — @Tool 与 Toolkit

> **导航**：[← 上一篇：Demo04 流式输出](./04-streaming.md) · [返回首页](./index.md) · [下一篇：Demo06 多智能体协作 →](./06-multi-agent.md)
>
> **本章源码**：[Demo05ToolCallingController](../src/main/java/com/xb/agentscope/controller/demo05/Demo05ToolCallingController.java) · [WeatherTools](../src/main/java/com/xb/agentscope/controller/demo05/WeatherTools.java)

## 知识点

- `@Tool` / `@ToolParam` 注解定义工具函数
- `Toolkit` 工具集注册
- 工具调用生命周期
- `maxIters` 限制工具调用轮次

## 定义工具函数

使用 `@Tool` 和 `@ToolParam` 注解将普通 Java 方法声明为 Agent 可调用的工具：

```java
public class WeatherTools {

    @Tool(name = "get_weather", description = "查询指定城市当前的天气情况")
    public String getWeather(
            @ToolParam(name = "city", description = "需要查询天气的城市名称", required = true) String city,
            @ToolParam(name = "unit", description = "温度单位：摄氏度或华氏度") String unit) {
        return "「" + city + "」今日晴，气温 25" + (unit == null ? "摄氏度" : unit) + "，适宜出行。";
    }
}
```

注解说明：

| 注解 | 作用 | 关键属性 |
|------|------|---------|
| `@Tool` | 标记方法为可调用工具 | `name`：工具名（LLM 看到的函数名）<br>`description`：功能描述 |
| `@ToolParam` | 标记参数 | `name`：参数名<br>`description`：参数说明<br>`required`：是否必填 |

> `description` 不是注释，而是模型选择工具的依据——写得越清楚，Agent 选得越准。

## 注册工具

创建 `Toolkit` 实例，将工具对象注册进去：

```java
Toolkit toolkit = new Toolkit();
toolkit.registerTool(new WeatherTools());

HarnessAgent agent = HarnessAgent.builder()
    .name("weather-agent")
    .sysPrompt("你可以调用 get_weather 工具查询天气，并把结果整理给用户。")
    .model("dashscope:qwen-plus")
    .toolkit(toolkit)
    .maxIters(3)    // 工具调用最多迭代 3 轮
    .workspace(Paths.get(".agentscope/workspace-demo05"))
    .build();
```

> **重要**：系统提示中要明确告知 Agent 可以调用哪些工具，否则 Agent 不会主动使用。

## 工具调用流程

```
用户提问 → Agent 分析 → 决定调用工具 → 执行工具方法 → 获取结果 → 整合回复
         │                  │
         └─ maxIters 限制 ─┘  （超过则停止）
```

`maxIters` 限制 Agent 在单次对话中调用工具的最大轮次，防止死循环。

## 全局共享 Toolkit

在 `AgentScopeJavaConfig` 中，工具也被注册为全局 Bean：

```java
@Bean
public Toolkit sharedToolkit() {
    Toolkit toolkit = new Toolkit();
    toolkit.registerTool(new WeatherTools());
    toolkit.registerTool(new AssistantTools());
    return toolkit;
}
```

全局 Toolkit 适合跨 Demo 复用的工具；局部 Toolkit 适合只服务单个场景的工具，二者按需选择。

## 接口调用

```bash
curl "http://localhost:8080/api/demo05/weather?city=北京&prompt=请查一下这个城市的天气"
```

## 小结

- `@Tool` + `@ToolParam` 将 Java 方法暴露给 LLM
- `Toolkit.registerTool()` 注册工具实例
- 系统提示中要描述可用工具，引导 Agent 使用
- `maxIters` 防止工具调用死循环
- 全局 Bean 方式适合跨 Demo 共享工具
