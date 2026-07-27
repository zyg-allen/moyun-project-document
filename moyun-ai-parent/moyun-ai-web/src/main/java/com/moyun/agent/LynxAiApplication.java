package com.moyun.agent;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 应用程序入口类
 *
 * <p>Cat AI Agent 智能体管理系统启动类</p>
 *
 * @author laomao
 */
@SpringBootApplication
@MapperScan("com.moyun.agent.mapper")
@EnableAsync
@EnableScheduling
@EnableConfigurationProperties
public class LynxAiApplication {

    /**
     * 应用程序入口方法
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(LynxAiApplication.class, args);
    }
}
