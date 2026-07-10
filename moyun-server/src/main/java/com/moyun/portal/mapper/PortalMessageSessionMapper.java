package com.moyun.portal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.moyun.ext.cms.domain.vo.MessageSessionVO;
import com.moyun.portal.domain.entity.PortalMessageSession;

/**
 * 私信会话 数据层
 *
 * @author moyun
 */
@Mapper
public interface PortalMessageSessionMapper extends BaseMapper<PortalMessageSession> {

    /**
     * 查询我参与的所有会话（分页），关联 portal_user/sys_user 取对方信息，按最后消息时间倒序
     *
     * @param page     分页参数
     * @param userId   当前用户ID
     * @param userType 当前用户类型 portal/sys
     * @return 会话列表
     */
    Page<MessageSessionVO> selectMySessions(Page<MessageSessionVO> page, @Param("userId") Long userId, @Param("userType") String userType);

    /**
     * 按 userA/userB + 类型查询会话
     *
     * @param userA     较小用户ID
     * @param userAType A方类型 portal/sys
     * @param userB     较大用户ID
     * @param userBType B方类型 portal/sys
     * @return 会话对象
     */
    PortalMessageSession selectByUsers(@Param("userA") Long userA, @Param("userAType") String userAType,
                                       @Param("userB") Long userB, @Param("userBType") String userBType);

    /**
     * 插入会话（遇到唯一键冲突时忽略，避免并发创建重复会话）
     *
     * @param session 会话对象
     * @return 影响行数
     */
    int insertIgnore(PortalMessageSession session);

    /**
     * 按 sessionId 查询会话 VO（含对方用户信息与未读数）
     * 用于"按对方用户ID获取或创建会话"后返回前端
     *
     * @param sessionId 会话ID
     * @param userId    当前用户ID（决定 peerUser 与 unreadCount 取哪一边）
     * @param userType  当前用户类型 portal/sys
     * @return 会话 VO
     */
    MessageSessionVO selectSessionVOById(@Param("sessionId") Long sessionId, @Param("userId") Long userId, @Param("userType") String userType);
}
