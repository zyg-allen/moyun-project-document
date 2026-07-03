package com.moyun.ext.cms.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.ext.cms.domain.query.JobQuery;
import com.moyun.ext.cms.domain.vo.JobListItemVO;
import com.moyun.ext.cms.domain.vo.JobVO;

/**
 * 职位 Service 接口（前台）
 *
 * @author moyun
 */
public interface IPortalJobService {

    /**
     * 公开职位列表（分页，仅 open）
     */
    Page<JobListItemVO> listJobs(JobQuery query);

    /**
     * 职位详情（含公司信息）
     *
     * @param id 职位ID
     */
    JobVO getJobDetail(Long id);
}
