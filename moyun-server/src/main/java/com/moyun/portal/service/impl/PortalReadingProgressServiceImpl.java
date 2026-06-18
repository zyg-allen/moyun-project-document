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
import com.moyun.portal.service.IPortalGrowthService;
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

    @Autowired
    private IPortalGrowthService portalGrowthService;

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

        // 查询更新前的状态，判断是否是首次完成阅读
        String previousStatus = null;
        if (portalReadingProgress.getId() != null) {
            PortalReadingProgress existing = portalReadingProgressMapper.selectPortalReadingProgressById(portalReadingProgress.getId());
            if (existing != null) {
                previousStatus = existing.getStatus();
            }
        }

        // 如果进度达到100%，自动标记为已读
        boolean justFinished = false;
        if (portalReadingProgress.getProgress() != null && portalReadingProgress.getProgress() >= 100) {
            portalReadingProgress.setStatus("finished");
            // 仅在之前未完成时触发成长事件（避免重复）
            if (!"finished".equals(previousStatus)) {
                justFinished = true;
            }
        }

        int rows = portalReadingProgressMapper.updatePortalReadingProgress(portalReadingProgress);

        // 记录完成阅读成长事件
        if (rows > 0 && justFinished && portalReadingProgress.getUserId() != null) {
            portalGrowthService.recordEvent("reading", "finish_book",
                    portalReadingProgress.getUserId(), "book", portalReadingProgress.getBookId());
        }

        return rows;
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
