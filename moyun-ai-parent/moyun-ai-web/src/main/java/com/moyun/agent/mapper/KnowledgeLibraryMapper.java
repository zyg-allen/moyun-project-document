package com.moyun.agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyun.agent.entity.KnowledgeLibrary;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

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
    @Update("UPDATE knowledge_library SET " +
            "document_count = (SELECT COUNT(*) FROM knowledge_base WHERE library_id = #{libraryId} AND status = 2), " +
            "total_segments = (SELECT COALESCE(SUM(segment_count), 0) FROM knowledge_base WHERE library_id = #{libraryId} AND status = 2), " +
            "total_size = (SELECT COALESCE(SUM(file_size), 0) FROM knowledge_base WHERE library_id = #{libraryId}), " +
            "updated_at = NOW() " +
            "WHERE id = #{libraryId}")
    int updateStatistics(@Param("libraryId") Long libraryId);
    
    /**
     * 增加使用次数
     */
    @Update("UPDATE knowledge_library SET usage_count = usage_count + 1, last_used_time = NOW() WHERE id = #{libraryId}")
    int incrementUsageCount(@Param("libraryId") Long libraryId);
    
    /**
     * 增加命中次数
     */
    @Update("UPDATE knowledge_library SET hit_count = hit_count + 1 WHERE id = #{libraryId}")
    int incrementHitCount(@Param("libraryId") Long libraryId);
    
    /**
     * 按分类查询知识库
     */
    @Select("SELECT * FROM knowledge_library WHERE category = #{category} AND status = 'active' ORDER BY usage_count DESC")
    List<KnowledgeLibrary> selectByCategory(@Param("category") String category);
    
    /**
     * 查询所有活跃的知识库（用于下拉选择）
     */
    @Select("SELECT id, name, description, icon, category, document_count, status FROM knowledge_library WHERE status = 'active' ORDER BY usage_count DESC, created_at DESC")
    List<KnowledgeLibrary> selectActiveLibraries();
}
