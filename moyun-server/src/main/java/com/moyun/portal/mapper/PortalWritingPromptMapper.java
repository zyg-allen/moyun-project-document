package com.moyun.portal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyun.portal.domain.entity.PortalWritingPrompt;
import org.apache.ibatis.annotations.Mapper;

/**
 * 每日写作 prompt Mapper
 *
 * 简单查询使用 LambdaQueryWrapper，无需自定义 XML 方法。
 *
 * @author moyun
 */
@Mapper
public interface PortalWritingPromptMapper extends BaseMapper<PortalWritingPrompt> {
}
