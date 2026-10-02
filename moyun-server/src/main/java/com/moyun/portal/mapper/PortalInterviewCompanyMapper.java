package com.moyun.portal.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.moyun.portal.domain.entity.PortalInterviewCompany;

/**
 * 面试公司标签 Mapper
 *
 * @author moyun
 */
@Mapper
public interface PortalInterviewCompanyMapper extends BaseMapper<PortalInterviewCompany>
{
    /**
     * 查询热门公司（按关联题目数量排序）
     */
    public List<PortalInterviewCompany> selectHotCompanies(@Param("limit") int limit);

    /**
     * 根据题目ID查询关联的公司标签
     */
    public List<PortalInterviewCompany> selectCompaniesByQuestionId(@Param("questionId") Long questionId);

    /**
     * 按公司ID查询关联的题目ID集合。
     *
     * <p>公司页「公司题目」筛选使用：返回 id 集合供上层做 IN，**不用 JOIN** ——
     * 分页查询用 JOIN 会放大行数、影响 count 语义。</p>
     */
    public List<Long> selectQuestionIdsByCompanyId(@Param("companyId") Long companyId);
}
