package com.moyun.system.service.impl;

import com.moyun.system.domain.entity.SysPlatform;
import com.moyun.system.mapper.SysPlatformMapper;
import com.moyun.system.service.ISysPlatformService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 端定义 业务层处理
 *
 * @author allen-zyg
 */
@Service
public class SysPlatformServiceImpl implements ISysPlatformService {

    @Autowired
    private SysPlatformMapper platformMapper;

    @Override
    public List<SysPlatform> selectPlatformOptionselect() {
        return platformMapper.selectPlatformOptionselect();
    }
}
