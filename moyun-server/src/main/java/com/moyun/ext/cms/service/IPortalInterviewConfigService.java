package com.moyun.ext.cms.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.moyun.portal.domain.entity.PortalInterviewConfig;

/**
 * 面试配置服务接口（v11.x 智能面试）
 *
 * @author moyun
 */
public interface IPortalInterviewConfigService extends IService<PortalInterviewConfig> {

    /**
     * 获取全局默认面试配置（is_default=1 且 active）；
     * 不存在或已停用时返回 null，调用方走内置默认值
     */
    PortalInterviewConfig getDefaultConfig();
}