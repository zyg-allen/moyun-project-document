package com.moyun.ext.cms.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.common.exception.system.ServiceException;
import com.moyun.util.string.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moyun.core.base.page.PageDomain;
import com.moyun.ext.cms.domain.vo.BookClubActivityVO;
import com.moyun.ext.cms.domain.vo.BookClubRecordVO;
import com.moyun.ext.cms.service.IBookClubService;
import com.moyun.portal.domain.entity.PortalBookClubActivity;
import com.moyun.portal.domain.entity.PortalBookClubParticipant;
import com.moyun.portal.domain.entity.PortalBookClubRecord;
import com.moyun.portal.domain.entity.PortalBookClubRecordLike;
import com.moyun.portal.mapper.PortalBookClubActivityMapper;
import com.moyun.portal.mapper.PortalBookClubParticipantMapper;
import com.moyun.portal.mapper.PortalBookClubRecordLikeMapper;
import com.moyun.portal.mapper.PortalBookClubRecordMapper;
import com.moyun.util.bean.PageUtils;

/**
 * 共读活动模块 Service 实现
 *
 * @author moyun
 */
@Service
public class BookClubServiceImpl implements IBookClubService {

    @Autowired private PortalBookClubActivityMapper activityMapper;
    @Autowired private PortalBookClubParticipantMapper participantMapper;
    @Autowired private PortalBookClubRecordMapper recordMapper;
    @Autowired private PortalBookClubRecordLikeMapper recordLikeMapper;

    // ========================================================================
    // 活动列表 / 详情
    // ========================================================================

    @Override
    public Page<BookClubActivityVO> getActivityList(PageDomain pageDomain, Long currentUserId) {
        Page<BookClubActivityVO> page = PageUtils.buildPage(pageDomain);
        return activityMapper.selectListWithCount(page, currentUserId);
    }

    @Override
    public BookClubActivityVO getActivityDetail(Long id, Long currentUserId) {
        PortalBookClubActivity activity = activityMapper.selectById(id);
        if (activity == null) {
            return null;
        }
        BookClubActivityVO vo = new BookClubActivityVO();
        BeanUtils.copyProperties(activity, vo);
        vo.setParticipantsCount(participantMapper.countByActivity(id));
        vo.setRecordsCount(recordMapper.selectCount(
                Wrappers.<PortalBookClubRecord>lambdaQuery().eq(PortalBookClubRecord::getActivityId, id)));
        vo.setIsJoined(currentUserId != null
                && participantMapper.selectByActivityAndUser(id, currentUserId) != null);
        return vo;
    }

    // ========================================================================
    // 加入 / 退出
    // ========================================================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> joinActivity(Long activityId, Long userId) {
        PortalBookClubActivity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new ServiceException("活动不存在");
        }
        if (participantMapper.selectByActivityAndUser(activityId, userId) != null) {
            throw new ServiceException("您已加入该活动");
        }
        // 人数上限校验（以实际参与人数为准）
        if (activity.getMaxParticipants() != null && activity.getMaxParticipants() > 0) {
            long joined = participantMapper.countByActivity(activityId);
            if (joined >= activity.getMaxParticipants()) {
                throw new ServiceException("活动参与人数已满");
            }
        }
        PortalBookClubParticipant participant = new PortalBookClubParticipant();
        participant.setActivityId(activityId);
        participant.setUserId(userId);
        participant.setJoinTime(LocalDateTime.now());
        participant.setCreateTime(LocalDateTime.now());
        participant.setUpdateTime(LocalDateTime.now());
        participantMapper.insert(participant);

        // 原子递增冗余参与人数
        activityMapper.incrementCurrentParticipants(activityId);

        Map<String, Object> result = new HashMap<>();
        result.put("joined", true);
        result.put("participantsCount", participantMapper.countByActivity(activityId));
        result.put("message", "加入成功");
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> leaveActivity(Long activityId, Long userId) {
        PortalBookClubParticipant participant = participantMapper.selectByActivityAndUser(activityId, userId);
        if (participant == null) {
            throw new ServiceException("您未加入该活动");
        }
        participantMapper.deleteById(participant.getId());

        // 原子递减冗余参与人数（带非负保护）
        activityMapper.decrementCurrentParticipants(activityId);

        Map<String, Object> result = new HashMap<>();
        result.put("joined", false);
        result.put("participantsCount", participantMapper.countByActivity(activityId));
        result.put("message", "已退出活动");
        return result;
    }

    @Override
    public boolean isJoined(Long activityId, Long userId) {
        return participantMapper.selectByActivityAndUser(activityId, userId) != null;
    }

    // ========================================================================
    // 共读记录
    // ========================================================================

    @Override
    public Page<BookClubRecordVO> listRecords(Long activityId, PageDomain pageDomain, Long currentUserId) {
        Page<BookClubRecordVO> page = PageUtils.buildPage(pageDomain);
        return recordMapper.selectListByActivity(page, activityId, currentUserId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> submitRecord(Long activityId, Long userId, String content, String recordType) {
        PortalBookClubActivity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new ServiceException("活动不存在");
        }
        if (StringUtils.isEmpty(content)) {
            throw new ServiceException("打卡内容不能为空");
        }
        // 记录类型规范化
        if (StringUtils.isEmpty(recordType) || !"excerpt".equals(recordType)) {
            recordType = "reflection";
        }
        // 计算第几天（基于活动开始日期）
        int day = 1;
        if (activity.getStartDate() != null) {
            long daysBetween = ChronoUnit.DAYS.between(activity.getStartDate(), LocalDate.now());
            day = (int) Math.max(1, daysBetween + 1);
        }

        PortalBookClubRecord record = new PortalBookClubRecord();
        record.setActivityId(activityId);
        record.setUserId(userId);
        record.setDay(day);
        record.setContent(content);
        record.setRecordType(recordType);
        record.setLikeCount(0L);
        record.setCreateTime(LocalDateTime.now());
        record.setUpdateTime(LocalDateTime.now());
        recordMapper.insert(record);

        Map<String, Object> result = new HashMap<>();
        result.put("recordId", record.getId());
        result.put("day", day);
        result.put("recordType", recordType);
        result.put("message", "提交成功");
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteRecord(Long recordId, Long userId) {
        PortalBookClubRecord record = recordMapper.selectById(recordId);
        if (record == null) {
            throw new ServiceException("记录不存在");
        }
        if (record.getUserId() == null || !record.getUserId().equals(userId)) {
            throw new ServiceException("无权删除他人的记录");
        }
        recordMapper.deleteById(recordId);
        // 清理关联的点赞记录，保持引用完整性
        recordLikeMapper.delete(Wrappers.<PortalBookClubRecordLike>lambdaQuery()
                .eq(PortalBookClubRecordLike::getRecordId, recordId));
        return true;
    }

    // ========================================================================
    // 点赞（toggle + 原子计数）
    // ========================================================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> toggleRecordLike(Long recordId, Long userId) {
        PortalBookClubRecord record = recordMapper.selectById(recordId);
        if (record == null) {
            throw new ServiceException("记录不存在");
        }
        Map<String, Object> result = new HashMap<>();
        PortalBookClubRecordLike exist = recordLikeMapper.selectByRecordAndUser(recordId, userId);
        if (exist != null) {
            // 已点赞 → 取消
            recordLikeMapper.deleteById(exist.getId());
            recordMapper.decrementLikeCount(recordId);
            result.put("liked", false);
        } else {
            // 未点赞 → 点赞
            PortalBookClubRecordLike like = new PortalBookClubRecordLike();
            like.setRecordId(recordId);
            like.setUserId(userId);
            like.setCreateTime(LocalDateTime.now());
            like.setUpdateTime(LocalDateTime.now());
            recordLikeMapper.insert(like);
            recordMapper.incrementLikeCount(recordId);
            result.put("liked", true);
        }
        // 回查最新点赞数
        PortalBookClubRecord latest = recordMapper.selectById(recordId);
        result.put("likeCount", latest != null && latest.getLikeCount() != null ? latest.getLikeCount() : 0L);
        return result;
    }
}
