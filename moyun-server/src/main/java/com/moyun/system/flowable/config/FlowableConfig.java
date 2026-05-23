package com.moyun.system.flowable.config;

import org.flowable.spring.SpringProcessEngineConfiguration;
import org.flowable.spring.boot.EngineConfigurationConfigurer;
import org.springframework.context.annotation.Configuration;

/**
 * Flowable配置类 - 适配 Flowable 7.1.0
 *
 * @author Tony
 * @date 2021-04-03
 */
@Configuration
public class FlowableConfig implements EngineConfigurationConfigurer<SpringProcessEngineConfiguration> {

    @Override
    public void configure(SpringProcessEngineConfiguration engineConfiguration) {
        // 设置流程图字体（支持中文）
        engineConfiguration.setActivityFontName("宋体");
        engineConfiguration.setLabelFontName("宋体");
        engineConfiguration.setAnnotationFontName("宋体");
        
        // Flowable 7.x 额外配置
        // 启用事件记录器
        engineConfiguration.setEnableEventDispatcher(true);
        
        // 设置历史级别（已在 application.yaml 中配置，这里作为补充）
        // engineConfiguration.setHistoryLevel(org.flowable.common.engine.impl.history.HistoryLevel.FULL);
    }
}
