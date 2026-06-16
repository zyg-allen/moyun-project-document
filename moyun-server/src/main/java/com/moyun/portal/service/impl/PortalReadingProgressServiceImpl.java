package com.moyun.portal.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.moyun.portal.domain.entity.PortalReadingProgress;
import com.moyun.portal.domain.query.ReadingProgressQuery;
import com.moyun.portal.mapper.PortalReadingProgressMapper;
import com.moyun.portal.service.IPortalReadingProgressService;

/**
 * 阅读进度 业务层实现
 *
 * @author moyun
 */
@Service
public class PortalReadingProgressServiceImpl extends ServiceImpl<PortalReadingProgressMapper, PortalReadingProgress> implements IPortalReadingProgressService {

    @Autowired
    private PortalReadingProgressMapper portalReadingProgressMapper;

    @Override
    public Page<PortalReadingProgress> selectPortalReadingProgressPage(Page<PortalReadingProgress> page, ReadingProgressQuery query) {
        return portalReadingProgressMapper.selectPortalReadingProgressPage(page, query);
    }

    @Override
    public List<PortalReadingProgress> selectPortalReadingProgressList(ReadingProgressQuery query) {
        return portalReadingProgressMapper.selectPortalReadingProgressList(query);
    }

    @Override
    public PortalReadingProgress selectPortalReadingProgressById(Long id) {
        return portalReadingProgressMapper.selectPortalReadingProgressById(id);
    }

    @Override
    public int insertPortalReadingProgress(PortalReadingProgress portalReadingProgress) {
        if (portalReadingProgress.getCreateTime() == null) {
            portalReadingProgress.setCreateTime(LocalDateTime.now());
        }
        if (portalReadingProgress.getStatus() == null || portalReadingProgress.getStatus().isEmpty()) {
            portalReadingProgress.setStatus("reading");
        }
        if (portalReadingProgress.getProgress() == null) {
            portalReadingProgress.setProgress(0);
        }
        if (portalReadingProgress.getPagesRead() == null) {
            portalReadingProgress.setPagesRead(0);
        }
        return portalReadingProgressMapper.insertPortalReadingProgress(portalReadingProgress);
    }

    @Override
    public int updatePortalReadingProgress(PortalReadingProgress portalReadingProgress) {
        portalReadingProgress.setUpdateTime(LocalDateTime.now());
        // 如果进度达到100%，自动标记为已读
        if (portalReadingProgress.getProgress() != null && portalReadingProgress.getProgress() >= 100) {
            portalReadingProgress.setStatus("finished");
        }
        return portalReadingProgressMapper.updatePortalReadingProgress(portalReadingProgress);
    }

    @Override
    public int deletePortalReadingProgressById(Long id) {
        return portalReadingProgressMapper.deletePortalReadingProgressById(id);
    }

    @Override
    public int deletePortalReadingProgressByIds(Long[] ids) {
        return portalReadingProgressMapper.deletePortalReadingProgressByIds(ids);
    }

    @Override
    public PortalReadingProgress selectByUserAndBook(Long userId, Long bookId) {
        return portalReadingProgressMapper.selectByUserAndBook(userId, bookId);
    }

    @Override
    public List<PortalReadingProgress> selectByUserId(Long userId, String status) {
        ReadingProgressQuery query = new ReadingProgressQuery();
        query.setUserId(userId);
        if (status != null && !status.isEmpty()) {
            query.setStatus(status);
        }
        return portalReadingProgressMapper.selectPortalReadingProgressList(query);
    }
}
