package com.xb.agentscope.controller.demo05;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 天气工具单元测试
 *
 * 心得：工具类本身是纯 POJO，不依赖 Spring 上下文，
 * 测试应该简单直接，验证业务逻辑即可
 */
class WeatherToolsTest {

    @Test
    void testGetWeatherWithDefaultUnit() {
        WeatherTools tools = new WeatherTools();
        String result = tools.getWeather("北京", null);
        
        assertTrue(result.contains("北京"));
        assertTrue(result.contains("摄氏度"));
        assertTrue(result.contains("晴"));
    }

    @Test
    void testGetWeatherWithCustomUnit() {
        WeatherTools tools = new WeatherTools();
        String result = tools.getWeather("上海", "华氏度");
        
        assertTrue(result.contains("上海"));
        assertTrue(result.contains("华氏度"));
    }

    @Test
    void testGetWeatherFormat() {
        WeatherTools tools = new WeatherTools();
        String result = tools.getWeather("广州", "摄氏度");
        
        // 验证返回格式：「城市」今日晴，气温 25单位，适宜出行。
        assertTrue(result.startsWith("「"));
        assertTrue(result.contains("广州"));
        assertTrue(result.endsWith("适宜出行。"));
    }
}
