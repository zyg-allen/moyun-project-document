package com.moyun.core.config;

import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import java.time.ZoneId;
import java.util.TimeZone;

@Configuration
@EnableAspectJAutoProxy(exposeProxy = true)
@MapperScan("com.moyun.**.mapper")
public class ApplicationConfig {
    /**
     * 时区配置
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonObjectMapperCustomization() {
        return jacksonObjectMapperBuilder -> {
            // 设置时区为中国时区
            jacksonObjectMapperBuilder.timeZone(TimeZone.getTimeZone(ZoneId.of("Asia/Shanghai")));
            // 启用 Java8 日期时间模块支持 LocalDateTime、LocalDate、LocalTime 等类型
            jacksonObjectMapperBuilder.modules(new JavaTimeModule());
            // 配置日期时间格式化
            jacksonObjectMapperBuilder.simpleDateFormat("yyyy-MM-dd HH:mm:ss");
            // Long 类型转 String 避免前端精度丢失
            jacksonObjectMapperBuilder.serializerByType(Long.class, ToStringSerializer.instance);
        };
    }
}
