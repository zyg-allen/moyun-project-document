package com.moyun.ext.ai2.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyun.ext.ai2.entity.AiSceneRegistryConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI统一接入层-场景注册配置 Mapper
 *
 * @author laomao
 * @since 2026-09-09
 */
@Mapper
public interface AiSceneRegistryMapper extends BaseMapper<AiSceneRegistryConfig> {
}
