package com.moyun.framework.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI (Swagger) 配置类
 * <p>
 * 配置 API 文档信息和 JWT Token 认证机制，提供以下功能：
 * <ul>
 *     <li>定义 API 文档的基本信息（标题、描述、版本）</li>
 *     <li>配置 Bearer Token 认证方案（JWT 格式）</li>
 *     <li>设置全局安全要求，所有接口默认需要认证</li>
 * </ul>
 * </p>
 * 
 * @author zyg
 * @version 1.0.0
 * @since 2026-03-15
 */
@Configuration
public class OpenApiConfig {

    @Value("${spring.application.name:BootV3 API}")
    private String applicationName;

    @Value("${app.api.version:1.0.0}")
    private String apiVersion;

    /**
     * 配置 OpenAPI 文档实例
     * <p>
     * 创建并返回一个完整的 OpenAPI 配置对象，包括：
     * </p>
     * <ul>
     *     <li><strong>Info 信息</strong>：定义 API 文档的标题、描述、版本和联系人</li>
     *     <li><strong>SecurityRequirement</strong>：添加全局安全要求，标记为 "Bearer Authentication"</li>
     *     <li><strong>Components.SecurityScheme</strong>：
     *         <ul>
     *             <li>类型：HTTP Bearer 认证</li>
     *             <li>格式：JWT Token</li>
     *             <li>描述：指导用户输入正确的 Token 格式</li>
     *         </ul>
     *     </li>
     * </ul>
     * 
     * @return OpenAPI 配置对象，包含完整的文档元数据和安全配置
     * @see io.swagger.v3.oas.models.OpenAPI
     * @see io.swagger.v3.oas.models.info.Info
     */
    @Bean
    public OpenAPI customOpenAPI() {
        // 配置安全方案（JWT Token）
        SecurityScheme securityScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("请输入 JWT Token 值（不需要加 Bearer 前缀，系统会自动添加）");
        
        // 配置全局安全要求
        SecurityRequirement securityRequirement = new SecurityRequirement()
                .addList("Authorization");
        
        return new OpenAPI()
                .info(new Info()
                        .title(applicationName + " API 文档")
                        .description("基于 Spring Boot 3 + Spring Security 6 的 RESTful API 文档")
                        .version(apiVersion)
                        .contact(new Contact()
                                .name("zyg")
                                .email("zyg@yourcompany.com")))
                // 配置安全组件
                .components(new Components()
                        .addSecuritySchemes("Authorization", securityScheme))
                // 添加全局安全要求（需要 JWT 认证）
                .addSecurityItem(securityRequirement);
    }
}
