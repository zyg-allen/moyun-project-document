package com.moyun.portal.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.moyun.portal.domain.entity.PortalReadingProgress;
import com.moyun.portal.domain.query.ReadingProgressQuery;

import java.util.List;

/**
 * 阅读进度 业务层接口
 *
 * @author moyun
 */
public interface IPortalReadingProgressService extends IService<PortalReadingProgress> {

    Page<PortalReadingProgress> selectPortalReadingProgressPage(Page<PortalReadingProgress> page, ReadingProgressQuery query);

    List<PortalReadingProgress> selectPortalReadingProgressList(ReadingProgressQuery query);

    PortalReadingProgress selectPortalReadingProgressById(Long id);

    int insertPortalReadingProgress(PortalReadingProgress portalReadingProgress);

    int updatePortalReadingProgress(PortalReadingProgress portalReadingProgress);

    int deletePortalReadingProgressById(Long id);

    int deletePortalReadingProgressByIds(Long[] ids);

    /**
     * 查询用户阅读进度（按用户ID+书籍ID）
     */
    PortalReadingProgress selectByUserAndBook(Long userId, Long bookId);

    /**
     * 查询用户书架（所有阅读记录）
     */
    List<PortalReadingProgress> selectByUserId(Long userId, String status);
}
