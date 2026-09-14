package com.xb.agentscope.controller.demo05;

import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;

public class WeatherTools {

    @Tool(name = "get_weather", description = "查询指定城市当前的天气情况")
    public String getWeather(
            @ToolParam(name = "city", description = "需要查询天气的城市名称", required = true) String city,
            @ToolParam(name = "unit", description = "温度单位：摄氏度或华氏度") String unit) {
        return "「" + city + "」今日晴，气温 25" + (unit == null ? "摄氏度" : unit) + "，适宜出行。";
    }
}