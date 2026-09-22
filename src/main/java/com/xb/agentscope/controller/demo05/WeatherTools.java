package com.xb.agentscope.controller.demo05;

import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;

/**
 * WeatherTools - 天气查询工具
 *
 * 演示如何用 @Tool / @ToolParam 注解将普通 Java 方法注册为 Agent 可调用的工具。
 * Agent 会根据用户意图自动决定是否调用此工具，并将参数映射到对应方法参数。
 *
 * @author ibqy
 */
public class WeatherTools {

    /**
     * 查询指定城市的模拟天气信息
     *
     * @param city 城市名称
     * @param unit 温度单位（可选），为 null 时默认摄氏度
     * @return 格式化的天气描述字符串
     */
    @Tool(name = "get_weather", description = "查询指定城市当前的天气情况")
    public String getWeather(
            @ToolParam(name = "city", description = "需要查询天气的城市名称", required = true) String city,
            @ToolParam(name = "unit", description = "温度单位：摄氏度或华氏度") String unit) {
        return "「" + city + "」今日晴，气温 25" + (unit == null ? "摄氏度" : unit) + "，适宜出行。";
    }
}