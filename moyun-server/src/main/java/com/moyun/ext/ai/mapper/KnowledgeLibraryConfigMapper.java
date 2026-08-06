package com.moyun.ext.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyun.ext.ai.entity.KnowledgeLibraryConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

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
    @Select("SELECT * FROM knowledge_library_config WHERE library_id = #{libraryId}")
    KnowledgeLibraryConfig selectByLibraryId(@Param("libraryId") Long libraryId);
}
