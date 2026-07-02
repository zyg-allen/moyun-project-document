package com.moyun.portal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.moyun.ext.cms.domain.vo.MessageVO;
import com.moyun.portal.domain.entity.PortalMessage;

/**
 * 私信消息 数据层
 *
 * @author moyun
 */
@Mapper
public interface PortalMessageMapper extends BaseMapper<PortalMessage> {

    /**
     * 按会话分页查询历史消息，关联 portal_user 取发送者信息
     *
     * @param page      分页参数
     * @param sessionId 会话ID
     * @return 消息列表
     */
    Page<MessageVO> selectBySession(Page<MessageVO> page, @Param("sessionId") Long sessionId);

    /**
     * 查询用户总未读数（汇总当前用户在所有会话中的未读数）
     *
     * @param userId 用户ID
     * @return 总未读数
     */
    Integer selectUnreadCount(@Param("userId") Long userId);
}
