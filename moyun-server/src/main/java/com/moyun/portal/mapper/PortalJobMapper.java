package com.moyun.portal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.ext.cms.domain.query.JobQuery;
import com.moyun.ext.cms.domain.vo.JobListItemVO;
import com.moyun.ext.cms.domain.vo.JobVO;
import com.moyun.portal.domain.entity.PortalJob;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 职位 Mapper
 *
 * @author moyun
 */
@Mapper
public interface PortalJobMapper extends BaseMapper<PortalJob> {

    /**
     * 公开职位列表分页（仅 open，含公司简要信息）
     */
    Page<JobListItemVO> selectListPage(Page<JobListItemVO> page, @Param("query") JobQuery query);

    /**
     * 职位详情（含公司信息，不含投递状态）
     */
    JobVO selectDetailById(@Param("id") Long id);
}
