package com.moyun.system.service;

import com.moyun.system.domain.SysNotice;

import java.util.List;

/**
 * 公告 业务层
 *
 * @author ruoyi
 */
public interface ISysNoticeService {

    /**
     * 查询公告信息
     *
     * @param noticeId 公告ID
     * @return 公告信息
     */
    SysNotice selectNoticeById(Long noticeId);

    /**
     * 查询公告列表
     *
     * @param notice 公告信息
     * @return 公告集合
     */
    List<SysNotice> selectNoticeList(SysNotice notice);

    /**
     * 新增公告
     *
     * @param notice 公告信息
     * @return 结果
     */
    int insertNotice(SysNotice notice);

    /**
     * 修改公告
     *
     * @param notice 公告信息
     * @return 结果
     */
    int updateNotice(SysNotice notice);

    /**
     * 删除公告信息
     *
     * @param noticeId 公告ID
     * @return 结果
     */
    int deleteNoticeById(Long noticeId);

    /**
     * 批量删除公告信息
     *
     * @param noticeIds 需要删除的公告ID
     * @return 结果
     */
    int deleteNoticeByIds(Long[] noticeIds);

    /**
     * 获取首页顶部公告列表
     *
     * @return 公告集合
     */
    List<SysNotice> selectNoticeTop();

    /**
     * 标记公告已读
     *
     * @param noticeId 公告ID
     * @param userId 用户ID
     * @return 结果
     */
    int markNoticeRead(Long noticeId, Long userId);

    /**
     * 批量标记公告已读
     *
     * @param ids 公告ID数组
     * @param userId 用户ID
     * @return 结果
     */
    int markNoticeReadAll(Long[] ids, Long userId);
}