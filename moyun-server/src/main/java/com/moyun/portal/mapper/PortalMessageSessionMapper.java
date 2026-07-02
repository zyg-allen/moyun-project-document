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
     * 查询我参与的所有会话（分页），关联 portal_user 取对方信息，按最后消息时间倒序
     *
     * @param page   分页参数
     * @param userId 当前用户ID
     * @return 会话列表
     */
    Page<MessageSessionVO> selectMySessions(Page<MessageSessionVO> page, @Param("userId") Long userId);

    /**
     * 按 userA/userB 查询会话
     *
     * @param userA 较小用户ID
     * @param userB 较大用户ID
     * @return 会话对象
     */
    PortalMessageSession selectByUsers(@Param("userA") Long userA, @Param("userB") Long userB);

    /**
     * 插入会话（遇到唯一键冲突时忽略，避免并发创建重复会话）
     *
     * @param session 会话对象
     * @return 影响行数
     */
    int insertIgnore(PortalMessageSession session);
}
