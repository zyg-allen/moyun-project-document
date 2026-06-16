package com.moyun.portal.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.moyun.portal.domain.entity.PortalInterviewQuestionCompany;

/**
 * 题目-公司关联 Mapper
 *
 * @author moyun
 */
@Mapper
public interface PortalInterviewQuestionCompanyMapper extends BaseMapper<PortalInterviewQuestionCompany>
{
    /**
     * 根据题目ID删除所有关联
     */
    public int deleteByQuestionId(@Param("questionId") Long questionId);

    /**
     * 根据公司ID删除所有关联
     */
    public int deleteByCompanyId(@Param("companyId") Long companyId);

    /**
     * 批量插入关联
     */
    public int batchInsert(@Param("list") List<PortalInterviewQuestionCompany> list);
}
