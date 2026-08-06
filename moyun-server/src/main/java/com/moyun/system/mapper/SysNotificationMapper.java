package com.moyun.system.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.moyun.system.domain.entity.SysNotification;

/**
 * 系统通知主体表 数据层
 *
 * @author moyun
 */
@Mapper
public interface SysNotificationMapper extends BaseMapper<SysNotification> {

    /**
     * 分页查询通知列表（后台管理用，可按 type/scope/userId/status 过滤）
     */
    Page<SysNotification> selectNotificationPage(Page<SysNotification> page, @Param("params") SysNotification query);

    /**
     * 不分页查询通知列表（用于导出等场景）
     */
    List<SysNotification> selectNotificationList(@Param("params") SysNotification query);

    /**
     * 根据ID查询通知
     */
    SysNotification selectNotificationById(Long id);

    /**
     * 新增通知
     */
    int insertNotification(SysNotification notification);

    /**
     * 修改通知
     */
    int updateNotification(SysNotification notification);

    /**
     * 通过ID删除通知
     */
    int deleteNotificationById(Long id);

    /**
     * 批量删除通知
     */
    int deleteNotificationByIds(Long[] ids);

    /**
     * 查询用户的未读通知（个人 + 广播，排除已读）
     * scope=user AND user_id=#{userId} AND user_type=#{userType} OR scope=all
     * NOT EXISTS 已读关系表
     *
     * @param page     分页参数
     * @param userId   用户ID
     * @param userType 用户类型：portal=门户用户 / sys=系统用户
     * @return 未读通知分页
     */
    Page<SysNotification> selectUnreadByUserId(Page<SysNotification> page,
                                               @Param("userId") Long userId,
                                               @Param("userType") String userType);

    /**
     * 查询用户的所有通知（个人 + 广播，含已读状态）
     *
     * @param page     分页参数
     * @param userId   用户ID
     * @param userType 用户类型：portal=门户用户 / sys=系统用户
     * @return 通知分页（isRead 字段通过 NOT EXISTS 计算）
     */
    Page<SysNotification> selectAllByUserId(Page<SysNotification> page,
                                            @Param("userId") Long userId,
                                            @Param("userType") String userType);

    /**
     * 统计用户未读通知数
     *
     * @param userId   用户ID
     * @param userType 用户类型：portal=门户用户 / sys=系统用户
     */
    int countUnreadByUserId(@Param("userId") Long userId,
                            @Param("userType") String userType);

    /**
     * 查询公开广播通知（scope=all，未登录用户也可查看）
     *
     * <p>userId/userType 为 null 时不计算已读状态（isRead 固定为 false）。
     * 用于门户公开页面，如公告列表、版本发布通知。</p>
     *
     * @param page     分页参数
     * @param userId   用户ID（可空）
     * @param userType 用户类型（可空）
     */
    Page<SysNotification> selectBroadcastAll(Page<SysNotification> page,
                                             @Param("userId") Long userId,
                                             @Param("userType") String userType);

    /**
     * 关闭待办：按 data 字段中的 bizType + id 精确匹配所有 type=todo 的通知，
     * 将 status 从 0（正常）更新为 1（关闭/已办）。
     * <p>用于业务审核完成后，把对应的待办通知从用户的待办列表中移除。
     * 依赖 data 字段 JSON 结构：{@code {"bizType":"xxx","id":123}}。
     *
     * @param bizType  业务类型（如 creator_certification）
     * @param entityId 业务实体 ID（如认证申请 ID）
     * @return 受影响行数
     */
    int completeTodoByBizData(@Param("bizType") String bizType,
                              @Param("entityId") Long entityId);
}
