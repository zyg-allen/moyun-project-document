package com.moyun.portal.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moyun.portal.domain.entity.PortalReadingProgress;
import com.moyun.portal.domain.query.ReadingProgressQuery;
import com.moyun.portal.mapper.PortalReadingProgressMapper;
import com.moyun.portal.service.IPortalGrowthService;
import com.moyun.portal.service.IPortalReadingProgressService;
import com.moyun.ext.cms.service.IFeedService;

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

    @Autowired
    private IFeedService feedService;

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
    @Transactional(rollbackFor = Exception.class)
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
            if (portalReadingProgress.getFinishDate() == null) {
                portalReadingProgress.setFinishDate(LocalDate.now());
            }
            // 仅在之前未完成时触发成长事件（避免重复）
            if (!"finished".equals(previousStatus)) {
                justFinished = true;
            }
        }

        int rows = portalReadingProgressMapper.updatePortalReadingProgress(portalReadingProgress);

        // 首次完成阅读：触发成长事件 + Feed 动态（与 upsertChapterProgress 共享同一副作用入口）
        if (rows > 0 && justFinished && portalReadingProgress.getUserId() != null) {
            triggerFinishEventsIfNeeded(portalReadingProgress.getUserId(), portalReadingProgress.getBookId());
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int upsertChapterProgress(PortalReadingProgress progress) {
        if (progress.getLastReadTime() == null) {
            progress.setLastReadTime(LocalDateTime.now());
        }

        // v1.1 阅读闭环：查询旧记录，判断是否首次完成（避免重复触发成长事件和 Feed 动态）
        // 注意：upsertChapterProgress 是前端实际调用路径（30s 节流上报 + 章节切换强制上报），
        //       必须在此处承担完成事件触发职责，否则用户读完章节/全书后状态永远无法闭环到 finished
        String previousStatus = null;
        if (progress.getUserId() != null && progress.getBookId() != null) {
            PortalReadingProgress existing = portalReadingProgressMapper.selectByUserAndBook(
                    progress.getUserId(), progress.getBookId());
            if (existing != null) {
                previousStatus = existing.getStatus();
            }
        }

        // 判断本次上报是否会触发完成：
        //   1. 前端显式上报 chapterFinished=true（用户已读到章节底部，且是最后一章时由前端置 true）
        //   2. progress >= 100（兼容前端直接计算 progress 的旧路径）
        boolean willFinish = (progress.getProgress() != null && progress.getProgress() >= 100)
                || Boolean.TRUE.equals(progress.getChapterFinished());
        if (willFinish) {
            progress.setStatus("finished");
            if (progress.getProgress() == null || progress.getProgress() < 100) {
                progress.setProgress(100);
            }
            if (progress.getFinishDate() == null) {
                progress.setFinishDate(LocalDate.now());
            }
        }

        int rows = portalReadingProgressMapper.upsertChapterProgress(progress);

        // 首次完成阅读：触发成长事件 + Feed 动态
        // 注意 SQL 层也有 progress>=100 自动 finished 的兜底，但事件触发只在 Service 层（避免重复）
        if (rows > 0 && willFinish && !"finished".equals(previousStatus)
                && progress.getUserId() != null && progress.getBookId() != null) {
            triggerFinishEventsIfNeeded(progress.getUserId(), progress.getBookId());
        }

        return rows;
    }

    /**
     * 触发完成阅读的副作用：成长事件埋点 + Feed 动态发布
     * <p>由 updatePortalReadingProgress（管理后台路径）与 upsertChapterProgress（前台阅读器路径）
     * 共享，避免重复代码，且保证两个入口的行为一致。</p>
     * <p>容错策略：成长事件失败会回滚事务（重）；Feed 动态失败静默忽略（轻），不影响主流程。</p>
     */
    private void triggerFinishEventsIfNeeded(Long userId, Long bookId) {
        portalGrowthService.recordEvent("reading", "finish_book", userId, "book", bookId);
        try {
            feedService.publishEvent(userId, "finish_book", "book", bookId, "完成阅读", null, null);
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(PortalReadingProgressServiceImpl.class)
                    .error("[Feed] 完成阅读动态事件失败：bookId={}", bookId, e);
        }
    }

    @Override
    public List<PortalReadingProgress> selectRecentReading(Long userId, int limit) {
        if (limit <= 0 || limit > 50) {
            limit = 10;
        }
        return portalReadingProgressMapper.selectRecentReading(userId, limit);
    }
}
