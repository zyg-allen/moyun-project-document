package com.moyun.portal.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import com.moyun.portal.domain.entity.PortalInterviewQuestion;

/**
 * 门户面试题目表 数据层
 *
 * @author moyun
 */
@Mapper
public interface PortalInterviewQuestionMapper extends BaseMapper<PortalInterviewQuestion>
{
    /**
     * 根据条件分页查询面试题目列表
     *
     * @param portalInterviewQuestion 面试题目信息
     * @return 面试题目信息集合信息
     */
    public List<PortalInterviewQuestion> selectPortalInterviewQuestionList(PortalInterviewQuestion portalInterviewQuestion);

    /**
     * 通过面试题目ID查询面试题目
     *
     * @param id 面试题目ID
     * @return 面试题目对象信息
     */
    public PortalInterviewQuestion selectPortalInterviewQuestionById(Long id);

    /**
     * 新增面试题目信息
     *
     * @param portalInterviewQuestion 面试题目信息
     * @return 结果
     */
    public int insertPortalInterviewQuestion(PortalInterviewQuestion portalInterviewQuestion);

    /**
     * 修改面试题目信息
     *
     * @param portalInterviewQuestion 面试题目信息
     * @return 结果
     */
    public int updatePortalInterviewQuestion(PortalInterviewQuestion portalInterviewQuestion);

    /**
     * 通过面试题目ID删除面试题目
     *
     * @param id 面试题目ID
     * @return 结果
     */
    public int deletePortalInterviewQuestionById(Long id);

    /**
     * 批量删除面试题目信息
     *
     * @param ids 需要删除的面试题目ID
     * @return 结果
     */
    public int deletePortalInterviewQuestionByIds(Long[] ids);

    /**
     * 原子增加点赞数（避免并发丢失更新，参考 PortalArticleMapper.incrementLikes 模式）
     *
     * @param id    题目ID
     * @param delta 增量（正数增加，负数减少）
     * @return 受影响行数
     */
    @Update("UPDATE portal_interview_question SET like_count = like_count + #{delta} WHERE id = #{id} AND like_count + #{delta} >= 0")
    int incrementLikes(@Param("id") Long id, @Param("delta") long delta);
}