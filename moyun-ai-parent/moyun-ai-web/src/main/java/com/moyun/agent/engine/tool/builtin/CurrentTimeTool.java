package com.moyun.agent.engine.tool.builtin;

import com.moyun.agent.engine.tool.ToolContext;
import com.moyun.agent.engine.tool.ToolExecutor;
import com.moyun.agent.engine.tool.ToolResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * 当前时间工具
 *
 * <p>获取当前日期和时间，支持不同时区</p>
 *
 * @author laomao
 */
@Slf4j
@Component
public class CurrentTimeTool implements ToolExecutor {

    @Override
    public String getName() {
        return "current_time";
    }

    @Override
    public String getDescription() {
        return "获取当前的日期和时间，可指定时区和格式";
    }

    @Override
    public String getParametersSchema() {
        return """
            {
                "type": "object",
                "properties": {
                    "timezone": {
                        "type": "string",
                        "description": "时区，如Asia/Shanghai，默认北京时间",
                        "default": "Asia/Shanghai"
                    },
                    "format": {
                        "type": "string",
                        "description": "时间格式，默认yyyy-MM-dd HH:mm:ss",
                        "default": "yyyy-MM-dd HH:mm:ss"
                    }
                },
                "required": []
            }
            """;
    }

    @Override
    public ToolResult execute(ToolContext context, Map<String, Object> params) {
        try {
            String timezone = (String) params.getOrDefault("timezone", "Asia/Shanghai");
            String format = (String) params.getOrDefault("format", "yyyy-MM-dd HH:mm:ss");

            ZonedDateTime now = ZonedDateTime.now(ZoneId.of(timezone));
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
            String timeStr = now.format(formatter);

            // 获取星期几
            String dayOfWeek = now.getDayOfWeek().toString();
            String dayOfWeekCn = switch (dayOfWeek) {
                case "MONDAY" -> "星期一";
                case "TUESDAY" -> "星期二";
                case "WEDNESDAY" -> "星期三";
                case "THURSDAY" -> "星期四";
                case "FRIDAY" -> "星期五";
                case "SATURDAY" -> "星期六";
                case "SUNDAY" -> "星期日";
                default -> dayOfWeek;
            };

            String result = String.format("当前时间是：%s %s（时区：%s）", timeStr, dayOfWeekCn, timezone);

            log.info("🕐 时间查询: {}", result);
            return ToolResult.success(result);

        } catch (Exception e) {
            log.error("时间查询失败", e);
            return ToolResult.fail("获取时间失败: " + e.getMessage());
        }
    }
}
