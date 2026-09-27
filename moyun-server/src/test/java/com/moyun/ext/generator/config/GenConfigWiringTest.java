package com.moyun.ext.generator.config;

import com.moyun.core.config.YamlPropertySourceFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * {@link GenConfig} 与 {@link YamlPropertySourceFactory} 的接线验证
 *
 * <p><b>为什么需要这组测试</b>：{@code GenConfig} 此前存在三重叠加缺陷
 * （static 字段 + {@code @Value} 取顶层键 + {@code @PropertySource} 以 .properties
 * 语义平铺解析 YAML），结果是"能跑纯属巧合"——单层缩进下平铺解析恰好产生
 * 顶层键 {@code author}，所以 {@code ${author}} 能取到值。<b>这种"坏但看起来对"的
 * 状态无法靠编译发现，也无法靠全量集成测试发现</b>（集成测试需真实 MySQL/Redis，
 * 而本问题与 DB 无关）。</p>
 *
 * <p>故分两步锁定，且两步都**不手动注入属性源**，以真实验证注解链路：</p>
 * <ol>
 *   <li><b>工厂层</b>：{@link YamlPropertySourceFactory} 必须产出**带层级的**
 *       {@code gen.author}，且**不产生**平铺的 {@code author} —— 证明第 3 个缺陷已修；</li>
 *   <li><b>注解+绑定层</b>：只注册 {@code GenConfig}，让 {@code @PropertySource}
 *       自己加载 YAML，断言五个静态字段被写入 —— 证明 {@code ${gen.xxx}} 路径正确。</li>
 * </ol>
 *
 * @author moyun
 */
class GenConfigWiringTest {

    /**
     * 步骤 1：YAML 工厂必须保留 {@code gen.} 前缀（证明不再是 .properties 平铺解析）
     */
    @Test
    @DisplayName("YamlPropertySourceFactory：保留层级键 gen.author，且不产生平铺的 author")
    void yamlFactoryPreservesHierarchy() throws Exception {
        YamlPropertySourceFactory factory = new YamlPropertySourceFactory();
        var resource = new org.springframework.core.io.support.EncodedResource(
                new org.springframework.core.io.ClassPathResource("generator.yml"));

        var propertySource = factory.createPropertySource("genTest", resource);

        // 关键断言 1：层级键存在（修复后才成立）
        assertEquals("moyun", propertySource.getProperty("gen.author"),
                "YAML 工厂必须产出 gen.author；若为 null 说明又退回平铺解析");
        assertEquals("com.moyun.system", propertySource.getProperty("gen.packageName"));
        assertEquals("sys_", propertySource.getProperty("gen.tablePrefix"));

        // 关键断言 2：不应存在顶层 author（存在即说明 YAML 被当成 properties 平铺解析了）
        assertNull(propertySource.getProperty("author"),
                "存在顶层键 author 说明 YAML 被平铺解析——这正是历史缺陷的根因");
    }

    /**
     * 步骤 2：仅注册 {@code GenConfig}，由 {@code @PropertySource} 自行加载 generator.yml，
     * 验证 {@code @Value("${gen.xxx}")} → static 字段的完整链路。
     *
     * <p>用最小上下文（不启动全应用）避免依赖 DB/Redis，同时真实触发
     * {@code ConfigurationClassPostProcessor} 对 {@code @PropertySource} 的处理。</p>
     */
    @Test
    @DisplayName("GenConfig：@PropertySource 生效，五个静态字段均正确注入")
    void genConfigFieldsArePopulatedViaAnnotation() {
        try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext()) {
            ctx.register(GenConfig.class);
            ctx.refresh();

            GenConfig config = ctx.getBean(GenConfig.class);
            assertNotNull(config, "GenConfig 必须是 Spring Bean");

            // 先证明 @PropertySource 确实把 YAML 加进了 Environment（且是带前缀的键）
            assertEquals("moyun", ctx.getEnvironment().getProperty("gen.author"),
                    "@PropertySource + YamlPropertySourceFactory 未把 gen.author 加入 Environment");

            // 核心：静态字段必须被写入。
            // 若 @Value 路径被改回顶层的 ${author}，这里会因为占位符无法解析而启动失败，
            // 或（若恰好又存在顶层键）读到错误来源——两种都会被下列断言暴露。
            assertEquals("moyun", GenConfig.getAuthor(),
                    "gen.author 未注入——@Value 路径可能被改回顶层键 ${author}");
            assertEquals("com.moyun.system", GenConfig.getPackageName());
            assertEquals("sys_", GenConfig.getTablePrefix());
            assertFalse(GenConfig.getAutoRemovePre(), "generator.yml 中 autoRemovePre=false");
            assertFalse(GenConfig.isAllowOverwrite(), "generator.yml 中 allowOverwrite=false");

            // 顺带锁定：环境里不应存在平铺的顶层 author（回归守卫）
            assertNull(ctx.getEnvironment().getProperty("author"),
                    "Environment 中出现了顶层 author，说明 YAML 又被平铺解析了");
        }
    }
}
