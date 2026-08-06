package com.moyun.ext.ai.engine.tool.builtin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moyun.ext.ai.engine.tool.ToolContext;
import com.moyun.ext.ai.engine.tool.ToolExecutor;
import com.moyun.ext.ai.engine.tool.ToolResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 天气查询工具
 *
 * <p>查询指定城市的实时天气和天气预报</p>
 *
 * @author laomao
 */
@Slf4j
@Component
public class WeatherTool implements ToolExecutor {

    @Value("${tool.weather.api-key:}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getName() {
        return "weather_query";
    }

    @Override
    public String getDescription() {
        return "查询指定城市的实时天气和未来天气预报，包括温度、湿度、风向、天气状况等";
    }

    @Override
    public String getParametersSchema() {
        return """
            {
                "type": "object",
                "properties": {
                    "city": {
                        "type": "string",
                        "description": "城市名称，如北京、上海、广州"
                    },
                    "days": {
                        "type": "integer",
                        "description": "预报天数1-7，默认1天",
                        "default": 1
                    }
                },
                "required": ["city"]
            }
            """;
    }

    @Override
    public ToolResult execute(ToolContext context, Map<String, Object> params) {
        String city = asString(params, "city");
        int days = asInt(params, "days", 1);

        if (city == null || city.trim().isEmpty()) {
            return ToolResult.fail("城市名称不能为空");
        }

        // 如果没有配置API Key，返回模拟数据
        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("天气API未配置，返回模拟数据");
            return getMockWeather(city, days);
        }

        try {
            String encodedCity = URLEncoder.encode(city, StandardCharsets.UTF_8);
            String url = String.format(
                    "https://api.seniverse.com/v3/weather/daily.json?key=%s&location=%s&language=zh-Hans&unit=c&start=0&days=%d",
                    apiKey, encodedCity, days
            );

            String response = restTemplate.getForObject(url, String.class);
            return parseWeatherResponse(response, city);

        } catch (Exception e) {
            log.error("天气查询失败: {}", city, e);
            // 失败时返回模拟数据
            return getMockWeather(city, days);
        }
    }

    /**
     * 解析天气API响应
     */
    private ToolResult parseWeatherResponse(String response, String city) {
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode results = root.path("results").get(0);
            JsonNode daily = results.path("daily");

            StringBuilder sb = new StringBuilder();
            sb.append(String.format("📍 %s 天气预报：\n\n", city));

            for (int i = 0; i < daily.size(); i++) {
                JsonNode day = daily.get(i);
                String date = day.path("date").asText();
                String textDay = day.path("text_day").asText();
                String textNight = day.path("text_night").asText();
                String high = day.path("high").asText();
                String low = day.path("low").asText();
                String windDirection = day.path("wind_direction").asText();
                String windScale = day.path("wind_scale").asText();

                sb.append(String.format("📅 %s\n", date));
                sb.append(String.format("   白天：%s，夜间：%s\n", textDay, textNight));
                sb.append(String.format("   温度：%s℃ ~ %s℃\n", low, high));
                sb.append(String.format("   风向：%s，风力：%s级\n\n", windDirection, windScale));
            }

            return ToolResult.success(sb.toString().trim());

        } catch (Exception e) {
            log.error("解析天气数据失败", e);
            return ToolResult.fail("解析天气数据失败");
        }
    }

    /**
     * 返回模拟天气数据（API未配置时使用）
     */
    private ToolResult getMockWeather(String city, int days) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("📍 %s 天气预报（模拟数据）：\n\n", city));
        sb.append("📅 今天\n");
        sb.append("   白天：晴，夜间：多云\n");
        sb.append("   温度：5℃ ~ 15℃\n");
        sb.append("   风向：北风，风力：3级\n\n");

        if (days > 1) {
            sb.append("📅 明天\n");
            sb.append("   白天：多云，夜间：阴\n");
            sb.append("   温度：3℃ ~ 12℃\n");
            sb.append("   风向：东北风，风力：2级\n\n");
        }

        sb.append("⚠️ 注意：当前为模拟数据，请配置天气API获取真实数据");

        return ToolResult.success(sb.toString().trim());
    }
}
