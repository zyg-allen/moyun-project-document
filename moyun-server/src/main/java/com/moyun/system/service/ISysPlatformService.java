package com.moyun.system.service;

import com.moyun.system.domain.entity.SysPlatform;

import java.util.List;

/**
 * 端定义 业务层
 *
 * @author allen-zyg
 */
public interface ISysPlatformService {

    /**
     * 查询启用状态的端列表（下拉选项）
     *
     * @return 端列表
     */
    List<SysPlatform> selectPlatformOptionselect();
}
