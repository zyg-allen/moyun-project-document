package com.moyun.system.service.impl;

import com.moyun.system.domain.entity.SysNotice;
import com.moyun.system.mapper.SysNoticeMapper;
import com.moyun.system.service.ISysNoticeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 公告 服务层实现
 *
 * @author ruoyi
 */
@Service
public class SysNoticeServiceImpl implements ISysNoticeService {

    @Autowired
    private SysNoticeMapper noticeMapper;

    /**
     * 查询公告信息
     *
     * @param noticeId 公告ID
     * @return 公告信息
     */
    @Override
    public SysNotice selectNoticeById(Long noticeId) {
        return noticeMapper.selectNoticeById(noticeId);
    }

    /**
     * 查询公告列表
     *
     * @param notice 公告信息
     * @return 公告集合
     */
    @Override
    public List<SysNotice> selectNoticeList(SysNotice notice) {
        return noticeMapper.selectNoticeList(notice);
    }

    /**
     * 新增公告
     *
     * @param notice 公告信息
     * @return 结果
     */
    @Override
    public int insertNotice(SysNotice notice) {
        return noticeMapper.insertNotice(notice);
    }

    /**
     * 修改公告
     *
     * @param notice 公告信息
     * @return 结果
     */
    @Override
    public int updateNotice(SysNotice notice) {
        return noticeMapper.updateNotice(notice);
    }

    /**
     * 删除公告信息
     *
     * @param noticeId 公告ID
     * @return 结果
     */
    @Override
    public int deleteNoticeById(Long noticeId) {
        return noticeMapper.deleteNoticeById(noticeId);
    }

    /**
     * 批量删除公告信息
     *
     * @param noticeIds 需要删除的公告ID
     * @return 结果
     */
    @Override
    public int deleteNoticeByIds(Long[] noticeIds) {
        return noticeMapper.deleteNoticeByIds(noticeIds);
    }

    /**
     * 获取首页顶部公告列表
     *
     * @return 公告集合
     */
    @Override
    public List<SysNotice> selectNoticeTop() {
        SysNotice notice = new SysNotice();
        return noticeMapper.selectNoticeList(notice);
    }

    /**
     * 标记公告已读
     *
     * @param noticeId 公告ID
     * @param userId   用户ID
     * @return 结果
     */
    @Override
    public int markNoticeRead(Long noticeId, Long userId) {
        // TODO: 实现标记公告已读功能
        return 1;
    }

    /**
     * 批量标记公告已读
     *
     * @param ids    公告ID数组
     * @param userId 用户ID
     * @return 结果
     */
    @Override
    public int markNoticeReadAll(Long[] ids, Long userId) {
        // TODO: 实现批量标记公告已读功能
        return 1;
    }
}
