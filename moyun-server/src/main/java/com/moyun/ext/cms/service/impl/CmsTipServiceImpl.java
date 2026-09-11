package com.moyun.ext.cms.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.ext.cms.service.ICmsTipService;
import com.moyun.portal.domain.entity.PortalTipOrder;
import com.moyun.portal.mapper.PortalTipOrderMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * CMS 打赏后台管理 Service 实现（只读查询）
 *
 * <p>复用 {@link PortalTipOrderMapper}，列表走 selectAdminListPage（JOIN 用户/作者信息）。</p>
 *
 * @author moyun
 */
@Service
public class CmsTipServiceImpl implements ICmsTipService {

    @Autowired
    private PortalTipOrderMapper tipOrderMapper;

    @Override
    public Page<PortalTipOrder> selectTipPage(Page<PortalTipOrder> page, String targetType, String status,
                                              LocalDateTime startTime, LocalDateTime endTime) {
        return tipOrderMapper.selectAdminListPage(page, targetType, status, startTime, endTime);
    }

    @Override
    public PortalTipOrder selectTipById(Long id) {
        return tipOrderMapper.selectById(id);
    }
}
