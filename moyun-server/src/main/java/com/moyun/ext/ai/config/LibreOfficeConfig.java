package com.moyun.ext.ai.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jodconverter.core.office.OfficeManager;
import org.jodconverter.local.LocalConverter;
import org.jodconverter.local.office.LocalOfficeManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PreDestroy;
import java.io.File;

/**
 * LibreOffice 配置类
 *
 * <p>用于配置本地 LibreOffice 进程管理</p>
 *
 * <h3>配置方式：</h3>
 * <pre>
 * jodconverter.local.office-home=/usr/lib/libreoffice
 * jodconverter.local.timeout=120
 * jodconverter.local.max-tasks=5
 * </pre>
 *
 * @author laomao
 * @since 2024/11/28
 */
@Slf4j
@Data
@Configuration
@ConditionalOnProperty(name = "jodconverter.enabled", havingValue = "true")
@ConfigurationProperties(prefix = "jodconverter.local")
public class LibreOfficeConfig {

    /**
     * LibreOffice 安装目录
     */
    private String officeHome = "/usr/lib/libreoffice";

    /**
     * 转换超时时间（秒）
     */
    private int timeout = 120;

    /**
     * 最大并发转换任务数
     */
    private int maxTasks = 5;

    private OfficeManager officeManager;

    /**
     * 创建 OfficeManager Bean
     */
    @Bean
    public OfficeManager officeManager() {
        try {
            log.info("========== LibreOffice 本地配置 ==========");
            log.info("安装路径: {}", officeHome);
            log.info("转换超时: {}秒", timeout);
            log.info("最大任务数: {}", maxTasks);

            // 检查 LibreOffice 是否安装
            File officeFile = new File(officeHome);
            if (!officeFile.exists()) {
                log.warn("⚠️ LibreOffice未安装在: {}", officeHome);
                log.warn("⚠️ 文档转换将使用备用方案（格式可能丢失）");
                log.info("💡 请安装LibreOffice或配置正确的安装路径");
                return null;
            }

            // 构建 OfficeManager
            this.officeManager = LocalOfficeManager.builder()
                    .officeHome(officeFile)
                    .maxTasksPerProcess(maxTasks)
                    .taskExecutionTimeout(timeout * 1000L)
                    .build();

            // 启动 OfficeManager
            this.officeManager.start();

            log.info("✅ LibreOffice 本地服务已启动");
            log.info("==========================================");

            return this.officeManager;

        } catch (Exception e) {
            log.error("❌ LibreOffice 初始化失败: {}", e.getMessage(), e);
            log.warn("⚠️ 文档转换将使用备用方案（格式可能丢失）");
            return null;
        }
    }

    /**
     * 应用关闭时停止 OfficeManager
     */
    @PreDestroy
    public void destroy() {
        if (officeManager != null) {
            try {
                log.info("正在停止 LibreOffice 服务...");
                officeManager.stop();
                log.info("✅ LibreOffice 服务已停止");
            } catch (Exception e) {
                log.error("❌ 停止 LibreOffice 服务失败: {}", e.getMessage());
            }
        }
    }
}
