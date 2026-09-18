package com.moyun.ext.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyun.ext.ai.entity.KnowledgeLibraryConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 知识库配置Mapper
 *
 * @author laomao
 */
@Mapper
public interface KnowledgeLibraryConfigMapper extends BaseMapper<KnowledgeLibraryConfig> {
    
    /**
     * 根据知识库ID查询配置
     */

    KnowledgeLibraryConfig selectByLibraryId(@Param("libraryId") Long libraryId);
}
