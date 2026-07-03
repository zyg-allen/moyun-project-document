package com.moyun.ext.cms.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.portal.domain.entity.PortalJob;

/**
 * CMS 职位管理 Service 接口
 *
 * 提供职位 CRUD。
 *
 * @author moyun
 */
public interface ICmsJobService {

    Page<PortalJob> selectJobPage(Page<PortalJob> page, PortalJob job);

    PortalJob selectJobById(Long id);

    int insertJob(PortalJob job);

    int updateJob(PortalJob job);

    int deleteJobByIds(Long[] ids);
}
