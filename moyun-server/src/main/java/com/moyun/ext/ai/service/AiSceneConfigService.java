package com.moyun.ext.ai.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.moyun.ext.ai.entity.AiSceneConfig;

import java.util.List;

/**
 * AI场景配置服务接口
 *
 * @author moyun
 */
public interface AiSceneConfigService extends IService<AiSceneConfig> {

    /**
     * 获取所有场景配置（按创建时间倒序）
     */
    List<AiSceneConfig> listOrderByCreateTimeDesc();

    /**
     * 按场景代码获取启用中的配置（供灰度解析）
     */
    List<AiSceneConfig> listEnabledBySceneCode(String sceneCode);
}