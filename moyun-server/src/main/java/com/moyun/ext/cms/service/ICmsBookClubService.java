package com.moyun.ext.cms.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.portal.domain.entity.PortalBookClubActivity;

/**
 * CMS 共读活动管理 Service 接口
 *
 * <p>提供活动 CRUD 与状态流转（上下架）。</p>
 *
 * @author moyun
 */
public interface ICmsBookClubService {

    /**
     * 分页查询活动列表（含所有状态）
     */
    Page<PortalBookClubActivity> selectActivityPage(Page<PortalBookClubActivity> page, PortalBookClubActivity query);

    /**
     * 根据ID获取活动详情
     */
    PortalBookClubActivity selectActivityById(Long id);

    /**
     * 新增活动
     */
    int insertActivity(PortalBookClubActivity activity);

    /**
     * 修改活动
     */
    int updateActivity(PortalBookClubActivity activity);

    /**
     * 更新活动状态（上下架：upcoming/ongoing/ended）
     */
    int updateActivityStatus(Long id, String status);

    /**
     * 批量删除活动
     */
    int deleteActivityByIds(Long[] ids);
}
