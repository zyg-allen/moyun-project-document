package com.moyun.portal.mapper;

import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.moyun.portal.domain.entity.PortalEntityTag;

@Mapper
public interface PortalEntityTagMapper extends BaseMapper<PortalEntityTag> {

    int insertBatch(@Param("list") List<PortalEntityTag> entityTags);

    int deleteByEntity(@Param("entityType") String entityType, @Param("entityId") Long entityId);

    List<PortalEntityTag> selectByEntity(@Param("entityType") String entityType, @Param("entityId") Long entityId);

    List<Long> selectTagIdsByEntity(@Param("entityType") String entityType, @Param("entityId") Long entityId);

    List<PortalEntityTag> selectByTagId(@Param("tagId") Long tagId);

    // ==================== 学习统计（阶段三 3.5 知识图谱） ====================

    /**
     * 知识图谱节点：绑定到面试题的标签，含每个标签关联的题目数（3.5）
     * <p>
     * 返回每行 Map：{ tag_id, tag_name, question_count }
     *
     * @param limit 取前 N 个标签（按题目数降序）
     */
    @Select("SELECT t.id AS tag_id, t.name AS tag_name, " +
            "COUNT(DISTINCT et.entity_id) AS question_count " +
            "FROM portal_entity_tag et " +
            "JOIN portal_tag t ON t.id = et.tag_id " +
            "WHERE et.entity_type = 'interview_question' " +
            "GROUP BY t.id, t.name " +
            "ORDER BY question_count DESC, t.id ASC " +
            "LIMIT #{limit}")
    List<Map<String, Object>> selectKnowledgeNodes(@Param("limit") int limit);

    /**
     * 知识图谱节点掌握度：某用户在每个标签下的题目总数与已通过数（3.5）
     * <p>
     * 返回每行 Map：{ tag_id, total, solved }
     * 仅返回该用户至少有一次提交的标签维度数据，未提交的标签 solved=0 由 Service 兜底。
     *
     * @param userId 门户用户ID
     */
    @Select("SELECT t.id AS tag_id, " +
            "COUNT(DISTINCT et.entity_id) AS total, " +
            "COUNT(DISTINCT CASE WHEN s.is_success = 1 THEN et.entity_id END) AS solved " +
            "FROM portal_entity_tag et " +
            "JOIN portal_tag t ON t.id = et.tag_id " +
            "LEFT JOIN portal_interview_submission s ON s.question_id = et.entity_id AND s.user_id = #{userId} " +
            "WHERE et.entity_type = 'interview_question' " +
            "GROUP BY t.id")
    List<Map<String, Object>> selectKnowledgeMastery(@Param("userId") Long userId);

    /**
     * 知识图谱边：标签在题目上的共现关系（3.5）
     * <p>
     * 返回每行 Map：{ source, target, weight }（weight = 共同出现的题目数）
     *
     * @param limit 取前 N 条边（按共现次数降序）
     */
    @Select("SELECT et1.tag_id AS source, et2.tag_id AS target, " +
            "COUNT(DISTINCT et1.entity_id) AS weight " +
            "FROM portal_entity_tag et1 " +
            "JOIN portal_entity_tag et2 ON et1.entity_id = et2.entity_id AND et1.tag_id < et2.tag_id " +
            "WHERE et1.entity_type = 'interview_question' AND et2.entity_type = 'interview_question' " +
            "GROUP BY et1.tag_id, et2.tag_id " +
            "ORDER BY weight DESC " +
            "LIMIT #{limit}")
    List<Map<String, Object>> selectKnowledgeEdges(@Param("limit") int limit);
}
