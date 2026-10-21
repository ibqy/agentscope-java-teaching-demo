# Demo10：全能助理 — 多工具聚合与 maxIters

> **导航**：[← 上一篇：Demo09 Prompt 模板](./09-prompt-template.md) · [返回首页](./index.md) · [下一篇：Demo11 综合实战 →](./11-comprehensive-demo.md)
>
> **本章源码**：[Demo10AssistantController](../src/main/java/com/xb/agentscope/controller/demo10/Demo10AssistantController.java) · [AssistantTools](../src/main/java/com/xb/agentscope/controller/demo10/AssistantTools.java)

## 知识点

- 多工具注册：一个 Agent 拥有多个 `@Tool` 方法
- Agent 自主选择工具（计算器 / 备忘）
- `maxIters` 控制多轮工具调用循环
- 工具类的组织方式：同类工具放在一个 Tools 类中

## 多工具助理

Demo10 在 Demo05 的基础上更进一步：不再只有单一工具，而是一个集**聊天、计算、备忘**于一体的助理：

```java
@GetMapping("/assistant")
public String assistant(@RequestParam(defaultValue = "帮我把 12*3+5 算出来") String message) {
    Toolkit toolkit = new Toolkit();
    toolkit.registerTool(new AssistantTools());
    HarnessAgent assistant = HarnessAgent.builder()
            .name("all-in-one-assistant")
            .sysPrompt("你是一个集聊天、计算、备忘于一体的智能助理，善用工具辅助回答。")
            .model("dashscope:qwen-plus").toolkit(toolkit).maxIters(5)
            .workspace(Paths.get(".agentscope/workspace-demo10"))
            .build();
    Msg reply = assistant.call(List.<Msg>of(new UserMessage(message)), RuntimeContext.empty()).block();
    return reply != null ? reply.getTextContent() : "（无回复）";
}
```

## AssistantTools — 两个工具

```java
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
```

| 工具名 | 功能 | 关键参数 |
|--------|------|---------|
| `calculator` | 计算四则运算表达式 | `expression`（必填） |
| `save_note` | 保存一条备忘信息 | `note`（必填） |

## Agent 如何选择工具

```
用户："帮我算一下 128*7"
   │
   ├─ 分析意图 → 需要计算 → 匹配 calculator
   ├─ 填写参数 → expression="128*7"
   └─ 执行工具 → 拿到结果 → 组织自然语言回复
```

- 工具选择依赖 `description`：计算器写"四则运算表达式"，备忘写"保存重要信息"，模型据此匹配；
- 同一个问题可能触发多轮工具调用（先计算、再保存备忘），`maxIters(5)` 保证有足够轮次且不死循环。

> 教学简化：`calculator` 使用 `ScriptEngineManager` 的 JavaScript 引擎（JDK 15+ 已移除 Nashorn，仅作演示），生产环境应使用安全的表达式求值库。

## 接口调用

```bash
# 触发计算器
curl "http://localhost:8080/api/demo10/assistant?message=帮我把 12*3+5 算出来"

# 触发备忘
curl "http://localhost:8080/api/demo10/assistant?message=帮我记住：周五下午三点开评审会"

# 组合任务：先算再记
curl "http://localhost:8080/api/demo10/assistant?message=计算 88*9 并把结果记到备忘里"
```

## 单工具 vs 多工具

| 维度 | Demo05（单工具） | Demo10（多工具） |
|------|------------------|------------------|
| 工具箱 | 只有天气 | 计算器 + 备忘 |
| 选择难度 | 低（只有一个选择） | 中（需按意图匹配） |
| maxIters | 3 | 5（预留多轮调用） |
| 关键设计 | 工具描述清晰 | 工具描述**可区分** |

## 小结

- 一个 `Toolkit` 可以注册多个工具类，Agent 按意图自主选择
- 工具描述必须彼此可区分，否则模型容易选错
- `maxIters` 要覆盖可能的工具调用轮数，同时防止死循环
- 工具类可按领域组织（AssistantTools 收纳计算与备忘）
