package com.moyun.portal.service;

import com.baomidou.mybatisplus.extension.service.IService;

import com.moyun.portal.domain.entity.PortalReadingPreference;

/**
 * 用户阅读偏好 业务层接口
 *
 * @author moyun
 */
public interface IPortalReadingPreferenceService extends IService<PortalReadingPreference> {

    /**
     * 按用户ID查询偏好，不存在则返回默认偏好
     */
    PortalReadingPreference getOrCreateByUserId(Long userId);

    /**
     * 保存（upsert）偏好
     */
    int savePreference(PortalReadingPreference preference);
}
