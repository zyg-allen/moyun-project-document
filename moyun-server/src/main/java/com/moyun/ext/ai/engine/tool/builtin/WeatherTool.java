package com.moyun.ext.ai.engine.tool.builtin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moyun.ext.ai.engine.tool.ToolContext;
import com.moyun.ext.ai.engine.tool.ToolExecutor;
import com.moyun.ext.ai.engine.tool.ToolResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 天气查询工具
 *
 * <p>查询指定城市的实时天气和未来天气预报（真实数据）。</p>
 *
 * <p>数据通道（按优先级）：</p>
 * <ol>
 *     <li>心知天气（Seniverse）：配置 tool.weather.api-key 时启用</li>
 *     <li>Open-Meteo（默认）：完全免费、无需 API Key。
 *         先通过 Geocoding API 将中文城市名解析为经纬度，再查询预报。</li>
 * </ol>
 *
 * @author laomao
 */
@Slf4j
@Component
public class WeatherTool implements ToolExecutor {

    @Value("${tool.weather.api-key:}")
    private String apiKey;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final RestTemplate restTemplate;

    public WeatherTool() {
        // 设置超时，避免第三方接口无响应时阻塞工具线程
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(10000);
        this.restTemplate = new RestTemplate(factory);
    }

    @Override
    public String getName() {
        return "weather_query";
    }

    @Override
    public String getDescription() {
        return "查询指定城市的实时天气和未来天气预报（真实数据），包括温度、湿度、风向、天气状况等，预报最多7天";
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
        if (days < 1) days = 1;
        if (days > 7) days = 7;
        city = city.trim();

        // 已配置心知天气 Key 时优先使用，否则走免费的 Open-Meteo
        if (apiKey != null && !apiKey.isEmpty()) {
            try {
                String encodedCity = URLEncoder.encode(city, StandardCharsets.UTF_8);
                String url = String.format(
                        "https://api.seniverse.com/v3/weather/daily.json?key=%s&location=%s&language=zh-Hans&unit=c&start=0&days=%d",
                        apiKey, encodedCity, days
                );
                String response = restTemplate.getForObject(url, String.class);
                return parseSeniverseResponse(response, city);
            } catch (Exception e) {
                log.warn("心知天气查询失败，降级到 Open-Meteo: {}", e.getMessage());
            }
        }

        return queryByOpenMeteo(city, days);
    }

    /**
     * Open-Meteo 免费通道：地理编码（中文城市名 → 经纬度）+ 天气预报
     */
    private ToolResult queryByOpenMeteo(String city, int days) {
        try {
            // 1. 地理编码：城市名 → 经纬度
            String geoUrl = String.format(
                    "https://geocoding-api.open-meteo.com/v1/search?name=%s&count=1&language=zh&format=json",
                    URLEncoder.encode(city, StandardCharsets.UTF_8)
            );
            // 注意：必须传 URI 对象。传 String 会被当作 URI 模板二次编码（% → %25），导致中文城市名乱码
            JsonNode geoRoot = objectMapper.readTree(restTemplate.getForObject(URI.create(geoUrl), String.class));
            JsonNode geoResult = geoRoot.path("results").path(0);
            if (geoResult.isMissingNode()) {
                return ToolResult.fail("未找到城市: " + city + "，请确认城市名称是否正确");
            }

            double latitude = geoResult.path("latitude").asDouble();
            double longitude = geoResult.path("longitude").asDouble();
            // 优先用中文名，其次拼音名
            String resolvedName = geoResult.path("name").asText(city);
            String region = geoResult.path("admin1").asText("");
            String country = geoResult.path("country").asText("");

            // 2. 查询天气预报
            String forecastUrl = String.format(
                    "https://api.open-meteo.com/v1/forecast?latitude=%f&longitude=%f" +
                            "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max,wind_speed_10m_max,wind_direction_10m_dominant" +
                            "&current=temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m" +
                            "&timezone=auto&forecast_days=%d",
                    latitude, longitude, days
            );
            JsonNode forecast = objectMapper.readTree(restTemplate.getForObject(URI.create(forecastUrl), String.class));

            return parseOpenMeteoResponse(forecast, resolvedName, region, country);

        } catch (Exception e) {
            log.error("Open-Meteo 天气查询失败: {}", city, e);
            return ToolResult.fail("天气查询失败: " + e.getMessage());
        }
    }

    /**
     * 解析 Open-Meteo 响应，输出中文天气报告
     */
    private ToolResult parseOpenMeteoResponse(JsonNode forecast, String city, String region, String country) {
        try {
            StringBuilder sb = new StringBuilder();

            // 当前实况
            JsonNode current = forecast.path("current");
            if (!current.isMissingNode()) {
                sb.append(String.format("📍 %s%s%s 当前天气：\n",
                        city, region.isEmpty() ? "" : "（" + region + "）", country.isEmpty() ? "" : "，" + country));
                sb.append(String.format("   天气：%s，气温：%.1f℃，湿度：%d%%，风速：%.1f km/h\n\n",
                        wmoCodeToText(current.path("weather_code").asInt()),
                        current.path("temperature_2m").asDouble(),
                        current.path("relative_humidity_2m").asInt(),
                        current.path("wind_speed_10m").asDouble()));
            }

            // 逐日预报
            JsonNode daily = forecast.path("daily");
            int dayCount = daily.path("time").size();
            if (dayCount > 0) {
                sb.append("📅 未来预报：\n");
                for (int i = 0; i < dayCount; i++) {
                    String date = daily.path("time").get(i).asText();
                    String text = wmoCodeToText(daily.path("weather_code").get(i).asInt());
                    double high = daily.path("temperature_2m_max").get(i).asDouble();
                    double low = daily.path("temperature_2m_min").get(i).asDouble();
                    int rainProb = daily.path("precipitation_probability_max").get(i).asInt(-1);
                    double windSpeed = daily.path("wind_speed_10m_max").get(i).asDouble();
                    int windDir = daily.path("wind_direction_10m_dominant").get(i).asInt();

                    sb.append(String.format("   %s：%s，%s℃ ~ %s℃，%s风 %.0f km/h",
                            date, text, fmt(low), fmt(high), degToDirection(windDir), windSpeed));
                    if (rainProb >= 0) {
                        sb.append(String.format("，降水概率 %d%%", rainProb));
                    }
                    sb.append("\n");
                }
            }

            sb.append("\n（数据来源：Open-Meteo）");
            return ToolResult.success(sb.toString().trim());

        } catch (Exception e) {
            log.error("解析 Open-Meteo 数据失败", e);
            return ToolResult.fail("解析天气数据失败");
        }
    }

    /**
     * 解析心知天气（Seniverse）响应
     */
    private ToolResult parseSeniverseResponse(String response, String city) {
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode results = root.path("results").get(0);
            JsonNode daily = results.path("daily");

            StringBuilder sb = new StringBuilder();
            sb.append(String.format("📍 %s 天气预报：\n\n", city));

            for (int i = 0; i < daily.size(); i++) {
                JsonNode day = daily.get(i);
                sb.append(String.format("📅 %s\n", day.path("date").asText()));
                sb.append(String.format("   白天：%s，夜间：%s\n",
                        day.path("text_day").asText(), day.path("text_night").asText()));
                sb.append(String.format("   温度：%s℃ ~ %s℃\n",
                        day.path("low").asText(), day.path("high").asText()));
                sb.append(String.format("   风向：%s，风力：%s级\n\n",
                        day.path("wind_direction").asText(), day.path("wind_scale").asText()));
            }

            sb.append("（数据来源：心知天气）");
            return ToolResult.success(sb.toString().trim());

        } catch (Exception e) {
            log.error("解析心知天气数据失败", e);
            return ToolResult.fail("解析天气数据失败");
        }
    }

    /** WMO 天气代码 → 中文描述 */
    private String wmoCodeToText(int code) {
        return switch (code) {
            case 0 -> "晴";
            case 1 -> "大部晴";
            case 2 -> "局部多云";
            case 3 -> "阴";
            case 45, 48 -> "雾";
            case 51, 53, 55 -> "毛毛雨";
            case 56, 57 -> "冻毛毛雨";
            case 61 -> "小雨";
            case 63 -> "中雨";
            case 65 -> "大雨";
            case 66, 67 -> "冻雨";
            case 71 -> "小雪";
            case 73 -> "中雪";
            case 75 -> "大雪";
            case 77 -> "雪粒";
            case 80 -> "小阵雨";
            case 81 -> "阵雨";
            case 82 -> "强阵雨";
            case 85, 86 -> "阵雪";
            case 95 -> "雷暴";
            case 96, 99 -> "雷暴伴冰雹";
            default -> "未知天气（代码" + code + "）";
        };
    }

    /** 风向角度 → 中文方位 */
    private String degToDirection(int deg) {
        String[] dirs = {"北", "东北", "东", "东南", "南", "西南", "西", "西北"};
        return dirs[(int) Math.round(((deg % 360) / 45.0)) % 8] + "风";
    }

    /** 温度格式化（去多余小数位） */
    private String fmt(double value) {
        return value == Math.floor(value) ? String.valueOf((long) value) : String.format("%.1f", value);
    }

}
