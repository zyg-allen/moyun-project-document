package com.moyun.core.config;

import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.core.io.support.PropertySourceFactory;

import java.io.IOException;
import java.util.Objects;
import java.util.Properties;

/**
 * 让 {@code @PropertySource} 支持 YAML 的工厂
 *
 * <p><b>存在意义</b>：Spring 的 {@code @PropertySource} 默认使用
 * {@link org.springframework.core.io.support.DefaultPropertySourceFactory}，
 * 它以 <b>{@code .properties} 语义</b>读取文件（{@code java.util.Properties}）。
 * 用一个 YAML 文件配合默认工厂会产生"看似能跑"的假象：
 * {@code Properties} 会去掉行首空白，于是</p>
 *
 * <pre>
 * gen:
 *   author: moyun      →  被平铺解析为顶层键 author=moyun
 * </pre>
 *
 * <p>单层缩进下恰好也是合法 properties，因此值能读到；一旦出现<b>二级嵌套</b>
 * （如 {@code gen.template.dir}），平铺解析会产生错误键名并<b>静默失效</b>——
 * 这正是本项目 {@code GenConfig} 的历史缺陷（配合 static 字段 +
 * {@code @Value("${author}")} 取顶层键，能跑纯属巧合）。</p>
 *
 * <p>本工厂用 {@link YamlPropertiesFactoryBean} 正确解析 YAML，保留层级结构，
 * 使 {@code gen.author} 这类带前缀的键名按预期可用。</p>
 *
 * <p><b>用法</b></p>
 * <pre>{@code
 * @PropertySource(value = "classpath:generator.yml", factory = YamlPropertySourceFactory.class)
 * }</pre>
 *
 * <p><b>注意</b>：{@code @PropertySource} 加入的源优先级低于 {@code application*.yaml}。
 * 这是期望行为——环境 profile 可覆盖该配置文件。</p>
 *
 * @author moyun
 * @since 2026-09-26
 */
public class YamlPropertySourceFactory implements PropertySourceFactory {

    @Override
    public PropertySource<?> createPropertySource(String name, EncodedResource resource) throws IOException {
        String sourceName = name != null ? name : resource.getResource().getFilename();
        if (sourceName == null) {
            // 无名且无文件名：给一个可识别的兜底名，避免 NPE
            sourceName = "yamlPropertySource";
        }
        YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
        // 保留编码信息（resource.getResource() 的流式读取），避免中文/特殊字符乱码
        factory.setResources(resource.getResource());
        Properties properties = Objects.requireNonNull(
                factory.getObject(), "YAML 解析结果为空：" + sourceName);
        return new PropertiesPropertySource(sourceName, properties);
    }
}
