package com.moyun.ext.cms.service;

import java.util.Map;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.moyun.core.base.page.PageDomain;
import com.moyun.ext.cms.domain.vo.BookClubActivityVO;
import com.moyun.ext.cms.domain.vo.BookClubRecordVO;

/**
 * 共读活动模块 Service 接口
 *
 * @author moyun
 */
public interface IBookClubService {

    /**
     * 分页查询活动列表（公开，含参与人数/记录数聚合及 isJoined 标记）
     */
    Page<BookClubActivityVO> getActivityList(PageDomain pageDomain, Long currentUserId);

    /**
     * 活动详情（含统计与 isJoined 标记）
     */
    BookClubActivityVO getActivityDetail(Long id, Long currentUserId);

    /**
     * 加入活动
     */
    Map<String, Object> joinActivity(Long activityId, Long userId);

    /**
     * 退出活动
     */
    Map<String, Object> leaveActivity(Long activityId, Long userId);

    /**
     * 是否已加入活动
     */
    boolean isJoined(Long activityId, Long userId);

    /**
     * 活动打卡记录列表（公开，含作者信息及 isLiked 标记）
     */
    Page<BookClubRecordVO> listRecords(Long activityId, PageDomain pageDomain, Long currentUserId);

    /**
     * 提交共读记录（读后感/摘抄）
     *
     * @param activityId 活动ID
     * @param userId     用户ID
     * @param content    打卡内容
     * @param recordType 记录类型:reflection-读后感,excerpt-摘抄（空则默认 reflection）
     */
    Map<String, Object> submitRecord(Long activityId, Long userId, String content, String recordType);

    /**
     * 删除自己的共读记录
     */
    boolean deleteRecord(Long recordId, Long userId);

    /**
     * 切换打卡记录点赞状态（toggle）
     */
    Map<String, Object> toggleRecordLike(Long recordId, Long userId);
}
