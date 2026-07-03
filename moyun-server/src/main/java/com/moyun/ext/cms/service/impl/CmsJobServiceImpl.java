package com.moyun.ext.cms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.common.exception.system.ServiceException;
import com.moyun.ext.cms.service.ICmsJobService;
import com.moyun.portal.domain.entity.PortalJob;
import com.moyun.portal.mapper.PortalJobMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;

/**
 * CMS 职位管理 Service 实现
 *
 * @author moyun
 */
@Service
public class CmsJobServiceImpl implements ICmsJobService {

    @Autowired private PortalJobMapper jobMapper;

    @Override
    public Page<PortalJob> selectJobPage(Page<PortalJob> page, PortalJob job) {
        LambdaQueryWrapper<PortalJob> wrapper = new LambdaQueryWrapper<>();
        if (job != null) {
            if (job.getTitle() != null && !job.getTitle().isEmpty()) {
                wrapper.like(PortalJob::getTitle, job.getTitle());
            }
            if (job.getCompanyId() != null) {
                wrapper.eq(PortalJob::getCompanyId, job.getCompanyId());
            }
            if (job.getCity() != null && !job.getCity().isEmpty()) {
                wrapper.eq(PortalJob::getCity, job.getCity());
            }
            if (job.getStatus() != null && !job.getStatus().isEmpty()) {
                wrapper.eq(PortalJob::getStatus, job.getStatus());
            }
        }
        wrapper.orderByDesc(PortalJob::getCreatedTime);
        return jobMapper.selectPage(page, wrapper);
    }

    @Override
    public PortalJob selectJobById(Long id) {
        return jobMapper.selectById(id);
    }

    @Override
    public int insertJob(PortalJob job) {
        if (job.getStatus() == null || job.getStatus().isEmpty()) {
            job.setStatus("open");
        }
        job.setCreatedTime(LocalDateTime.now());
        job.setUpdatedTime(LocalDateTime.now());
        return jobMapper.insert(job);
    }

    @Override
    public int updateJob(PortalJob job) {
        PortalJob existing = jobMapper.selectById(job.getId());
        if (existing == null) {
            throw new ServiceException("职位不存在");
        }
        job.setUpdatedTime(LocalDateTime.now());
        return jobMapper.updateById(job);
    }

    @Override
    public int deleteJobByIds(Long[] ids) {
        return jobMapper.deleteBatchIds(Arrays.asList(ids));
    }
}
