package com.moyun.ext.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyun.ext.ai.entity.AiProvider;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI 提供商注册表 Mapper
 *
 * @author moyun
 */
@Mapper
public interface AiProviderMapper extends BaseMapper<AiProvider> {

    /**
     * 物理删除（绕过 @TableLogic 软删除）
     *
     * <p>注册表带 uk_code 唯一键：软删后行仍占用编码，重新注册同 code 提供商会报
     * Duplicate entry。注册表数据无审计价值，删除即物理删除。</p>
     */
    @Delete("DELETE FROM ai_provider WHERE id = #{id}")
    int hardDeleteById(Long id);
}
