package com.moyun.ext.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyun.ext.ai.entity.KnowledgeLibrary;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 知识库Mapper
 *
 * @author laomao
 */
@Mapper
public interface KnowledgeLibraryMapper extends BaseMapper<KnowledgeLibrary> {
    
    /**
     * 更新知识库统计信息
     */

    int updateStatistics(@Param("libraryId") Long libraryId);

    /**
     * 增加使用次数
     */

    int incrementUsageCount(@Param("libraryId") Long libraryId);

    /**
     * 增加命中次数
     */

    int incrementHitCount(@Param("libraryId") Long libraryId);

    /**
     * 按分类查询知识库
     */

    List<KnowledgeLibrary> selectByCategory(@Param("category") String category);

    /**
     * 查询所有活跃的知识库（用于下拉选择）
     */

    List<KnowledgeLibrary> selectActiveLibraries();
}
