package com.moyun.ext.cms.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.ext.cms.domain.query.JobQuery;
import com.moyun.ext.cms.domain.vo.JobListItemVO;
import com.moyun.ext.cms.domain.vo.JobVO;
import com.moyun.ext.cms.service.IPortalJobService;
import com.moyun.portal.mapper.PortalJobMapper;
import com.moyun.util.bean.PageUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 职位 Service 实现（前台）
 *
 * @author moyun
 */
@Service
public class PortalJobServiceImpl implements IPortalJobService {

    @Autowired private PortalJobMapper jobMapper;

    @Override
    public Page<JobListItemVO> listJobs(JobQuery query) {
        Page<JobListItemVO> page = PageUtils.buildPage(query);
        return jobMapper.selectListPage(page, query);
    }

    @Override
    public JobVO getJobDetail(Long id) {
        return jobMapper.selectDetailById(id);
    }
}
