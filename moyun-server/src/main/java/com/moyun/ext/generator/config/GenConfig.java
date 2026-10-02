package com.moyun.ext.generator.config;

import com.moyun.core.config.YamlPropertySourceFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

/**
 * 读取代码生成相关配置（来源 {@code classpath:generator.yml}）
 *
 * <p><b>实现要点与易错点</b>：</p>
 * <ol>
 *   <li>原类上标了 {@code @ConfigurationProperties(prefix = "gen")}，但字段全是
 *       {@code static} —— Spring 的 {@code @ConfigurationProperties} <b>不绑定静态字段</b>，
 *       该注解实际是空操作（本类不使用它，以免产生"配置类已绑定"的误导）。</li>
 *   <li>原 {@code @Value("${author}")} 取的是<b>顶层键</b>，而配置源里只有
 *       {@code gen.author}。能读到值的唯一原因是第 3 点。<b>已改为 {@code ${gen.xxx}}</b>。</li>
 *   <li>原 {@code @PropertySource("classpath:generator.yml")} 用默认工厂，以
 *       {@code .properties} 语义平铺解析 YAML —— 恰好把缩进的 {@code author: moyun}
 *       变成顶层键。一旦有人加二级嵌套就会静默失效。<b>已改用
 *       {@link YamlPropertySourceFactory}</b> 正确解析层级。</li>
 * </ol>
 *
 * <p><b>为何保留 static 字段</b>：消费方 {@code GenUtils} 是无 Spring 注解的纯静态工具类
 * （{@code GenUtils.java:23,24,27,156,157} 直接调 {@code GenConfig.getXxx()}），
 * {@code GenController:249} 亦然。改为实例字段需连带把这两个类的调用链改成注入形式，
 * 改动面与收益不成比例。此处以"setter 注入到 static 字段"保持既有访问方式，
 * 并由 {@code ConfigWiringValidator} 在启动期断言 {@code gen.author} 确实可读，
 * 避免再次出现"配置读不到却无人察觉"。</p>
 *
 * <p>新增配置项时请同步：① 本类加 static 字段 + getter + {@code @Value("${gen.xxx}")}
 * setter；② {@code generator.yml} 在 {@code gen:} 下加同名键；③ 如需断言则在
 * {@code ConfigWiringValidator} 登记。</p>
 *
 * @author allen-zyg
 */
@Component
@PropertySource(value = "classpath:generator.yml", factory = YamlPropertySourceFactory.class)
public class GenConfig {

    /**
     * 作者
     */
    private static String author;

    /**
     * 生成包路径
     */
    private static String packageName;

    /**
     * 自动去除表前缀
     */
    private static boolean autoRemovePre;

    /**
     * 表前缀
     */
    private static String tablePrefix;

    /**
     * 是否允许生成文件覆盖到本地（自定义路径）
     */
    private static boolean allowOverwrite;

    public static String getAuthor() {
        return author;
    }

    @Value("${gen.author}")
    public void setAuthor(String author) {
        GenConfig.author = author;
    }

    public static String getPackageName() {
        return packageName;
    }

    @Value("${gen.packageName}")
    public void setPackageName(String packageName) {
        GenConfig.packageName = packageName;
    }

    public static boolean getAutoRemovePre() {
        return autoRemovePre;
    }

    @Value("${gen.autoRemovePre}")
    public void setAutoRemovePre(boolean autoRemovePre) {
        GenConfig.autoRemovePre = autoRemovePre;
    }

    public static String getTablePrefix() {
        return tablePrefix;
    }

    @Value("${gen.tablePrefix}")
    public void setTablePrefix(String tablePrefix) {
        GenConfig.tablePrefix = tablePrefix;
    }

    public static boolean isAllowOverwrite() {
        return allowOverwrite;
    }

    @Value("${gen.allowOverwrite}")
    public void setAllowOverwrite(boolean allowOverwrite) {
        GenConfig.allowOverwrite = allowOverwrite;
    }
}
