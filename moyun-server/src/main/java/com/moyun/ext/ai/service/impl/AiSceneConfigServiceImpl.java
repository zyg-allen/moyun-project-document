package com.moyun.ext.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.moyun.ext.ai.entity.AiSceneConfig;
import com.moyun.ext.ai.mapper.AiSceneConfigMapper;
import com.moyun.ext.ai.service.AiSceneConfigService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * AI场景配置服务实现
 *
 * @author moyun
 */
@Service
public class AiSceneConfigServiceImpl extends ServiceImpl<AiSceneConfigMapper, AiSceneConfig>
        implements AiSceneConfigService {

    @Override
    public List<AiSceneConfig> listOrderByCreateTimeDesc() {
        return list(new LambdaQueryWrapper<AiSceneConfig>()
                .orderByDesc(AiSceneConfig::getCreateTime));
    }

    @Override
    public List<AiSceneConfig> listEnabledBySceneCode(String sceneCode) {
        return list(new LambdaQueryWrapper<AiSceneConfig>()
                .eq(AiSceneConfig::getSceneCode, sceneCode)
                .eq(AiSceneConfig::getEnabled, true)
                .orderByDesc(AiSceneConfig::getPriority));
    }
}