package com.moyun.ext.cms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.moyun.ext.cms.service.IPortalInterviewConfigService;
import com.moyun.portal.domain.entity.PortalInterviewConfig;
import com.moyun.portal.mapper.PortalInterviewConfigMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 面试配置服务实现（v11.x 智能面试）
 *
 * <p>is_default 互斥：设为默认时，其余配置自动降级为非默认（单事务保证全局唯一）。</p>
 *
 * @author moyun
 */
@Service
public class PortalInterviewConfigServiceImpl extends ServiceImpl<PortalInterviewConfigMapper, PortalInterviewConfig>
        implements IPortalInterviewConfigService {

    private static final Logger log = LoggerFactory.getLogger(PortalInterviewConfigServiceImpl.class);

    @Override
    public PortalInterviewConfig getDefaultConfig() {
        try {
            return getOne(new LambdaQueryWrapper<PortalInterviewConfig>()
                    .eq(PortalInterviewConfig::getIsDefault, 1)
                    .eq(PortalInterviewConfig::getStatus, "active")
                    .last("LIMIT 1"));
        } catch (Exception e) {
            log.warn("[InterviewConfig] 读取默认面试配置失败，走内置默认值：{}", e.getMessage());
            return null;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(PortalInterviewConfig entity) {
        normalizeDefaults(entity, null);
        boolean ok = super.save(entity);
        if (ok && Integer.valueOf(1).equals(entity.getIsDefault())) {
            clearOtherDefaults(entity.getId(), null);
        }
        return ok;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateById(PortalInterviewConfig entity) {
        normalizeDefaults(entity, entity.getId());
        boolean ok = super.updateById(entity);
        if (ok && Integer.valueOf(1).equals(entity.getIsDefault())) {
            clearOtherDefaults(entity.getId(), entity.getId());
        }
        return ok;
    }

    /** 基础默认值兜底 */
    private void normalizeDefaults(PortalInterviewConfig entity, Long excludeId) {
        if (entity.getStatus() == null || entity.getStatus().isBlank()) {
            entity.setStatus("active");
        }
        if (entity.getPersonaType() == null || entity.getPersonaType().isBlank()) {
            entity.setPersonaType("professional");
        }
        if (entity.getEnableSelfIntro() == null) {
            entity.setEnableSelfIntro(0);
        }
        if (entity.getIsDefault() == null) {
            entity.setIsDefault(0);
        }
    }

    /** is_default 互斥：将其他配置置为非默认 */
    private void clearOtherDefaults(Long keepId, Long excludeId) {
        LambdaUpdateWrapper<PortalInterviewConfig> uw = new LambdaUpdateWrapper<PortalInterviewConfig>()
                .eq(PortalInterviewConfig::getIsDefault, 1)
                .set(PortalInterviewConfig::getIsDefault, 0);
        if (excludeId != null) {
            uw.ne(PortalInterviewConfig::getId, excludeId);
        }
        update(uw);
        log.info("[InterviewConfig] 配置 {} 设为默认，其余已降级", keepId);
    }
}