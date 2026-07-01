package com.moyun.portal.service.impl;

import java.math.BigDecimal;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.moyun.portal.domain.entity.PortalReadingPreference;
import com.moyun.portal.mapper.PortalReadingPreferenceMapper;
import com.moyun.portal.service.IPortalReadingPreferenceService;

/**
 * 用户阅读偏好 业务层实现
 *
 * @author moyun
 */
@Service
public class PortalReadingPreferenceServiceImpl
        extends ServiceImpl<PortalReadingPreferenceMapper, PortalReadingPreference>
        implements IPortalReadingPreferenceService {

    @Autowired
    private PortalReadingPreferenceMapper preferenceMapper;

    @Override
    public PortalReadingPreference getOrCreateByUserId(Long userId) {
        PortalReadingPreference pref = preferenceMapper.selectByUserId(userId);
        if (pref != null) {
            return pref;
        }
        // 返回默认偏好（不写入 DB，由首次保存时 upsert）
        return defaultPreference(userId);
    }

    @Override
    public int savePreference(PortalReadingPreference preference) {
        if (preference.getUserId() == null) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        // 参数边界校验：保留宽容策略，超界值不抛异常，由前端做严格校验
        if (preference.getFontSize() != null && (preference.getFontSize() < 12 || preference.getFontSize() > 32)) {
            preference.setFontSize(18);
        }
        return preferenceMapper.upsertPreference(preference);
    }

    private PortalReadingPreference defaultPreference(Long userId) {
        PortalReadingPreference pref = new PortalReadingPreference();
        pref.setUserId(userId);
        pref.setFontSize(18);
        pref.setLineHeight(new BigDecimal("1.8"));
        pref.setTheme("default");
        pref.setFontFamily("system");
        pref.setLetterSpacing(new BigDecimal("0.0"));
        pref.setParagraphSpacing(new BigDecimal("1.2"));
        return pref;
    }
}
