package com.moyun.ext.cms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.common.exception.system.ServiceException;
import com.moyun.ext.cms.service.ICmsBookClubService;
import com.moyun.portal.domain.entity.PortalBookClubActivity;
import com.moyun.portal.mapper.PortalBookClubActivityMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;

/**
 * CMS 共读活动管理 Service 实现
 *
 * <p>复用 {@link PortalBookClubActivityMapper}，使用 LambdaQueryWrapper 模式。</p>
 *
 * @author moyun
 */
@Service
public class CmsBookClubServiceImpl implements ICmsBookClubService {

    @Autowired
    private PortalBookClubActivityMapper activityMapper;

    @Override
    public Page<PortalBookClubActivity> selectActivityPage(Page<PortalBookClubActivity> page, PortalBookClubActivity query) {
        LambdaQueryWrapper<PortalBookClubActivity> wrapper = new LambdaQueryWrapper<>();
        if (query != null) {
            if (query.getTitle() != null && !query.getTitle().isEmpty()) {
                wrapper.like(PortalBookClubActivity::getTitle, query.getTitle());
            }
            if (query.getStatus() != null && !query.getStatus().isEmpty()) {
                wrapper.eq(PortalBookClubActivity::getStatus, query.getStatus());
            }
            if (query.getBookId() != null) {
                wrapper.eq(PortalBookClubActivity::getBookId, query.getBookId());
            }
        }
        wrapper.orderByDesc(PortalBookClubActivity::getId);
        return activityMapper.selectPage(page, wrapper);
    }

    @Override
    public PortalBookClubActivity selectActivityById(Long id) {
        return activityMapper.selectById(id);
    }

    @Override
    public int insertActivity(PortalBookClubActivity activity) {
        if (activity.getStatus() == null || activity.getStatus().isEmpty()) {
            activity.setStatus("upcoming");
        }
        if (activity.getCurrentParticipants() == null) {
            activity.setCurrentParticipants(0);
        }
        return activityMapper.insert(activity);
    }

    @Override
    public int updateActivity(PortalBookClubActivity activity) {
        PortalBookClubActivity existing = activityMapper.selectById(activity.getId());
        if (existing == null) {
            throw new ServiceException("活动不存在");
        }
        return activityMapper.updateById(activity);
    }

    @Override
    public int updateActivityStatus(Long id, String status) {
        PortalBookClubActivity existing = activityMapper.selectById(id);
        if (existing == null) {
            throw new ServiceException("活动不存在");
        }
        LambdaUpdateWrapper<PortalBookClubActivity> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(PortalBookClubActivity::getId, id)
                .set(PortalBookClubActivity::getStatus, status);
        return activityMapper.update(null, wrapper);
    }

    @Override
    public int deleteActivityByIds(Long[] ids) {
        return activityMapper.deleteBatchIds(Arrays.asList(ids));
    }
}
