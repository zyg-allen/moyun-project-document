package com.moyun.portal.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.moyun.portal.domain.entity.PortalResumeParseConfig;
import com.moyun.portal.mapper.PortalResumeParseConfigMapper;
import com.moyun.portal.service.IPortalResumeParseConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 简历解析配置服务实现
 *
 * @author moyun
 */
@Service
public class PortalResumeParseConfigServiceImpl
        extends ServiceImpl<PortalResumeParseConfigMapper, PortalResumeParseConfig>
        implements IPortalResumeParseConfigService {

    private static final Logger log = LoggerFactory.getLogger(PortalResumeParseConfigServiceImpl.class);

    @Override
    public List<PortalResumeParseConfig> listActiveByType(String configType) {
        if (configType == null || configType.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return list(new LambdaQueryWrapper<PortalResumeParseConfig>()
                    .eq(PortalResumeParseConfig::getConfigType, configType)
                    .eq(PortalResumeParseConfig::getStatus, "active")
                    .orderByAsc(PortalResumeParseConfig::getSort)
                    .orderByAsc(PortalResumeParseConfig::getId));
        } catch (Exception e) {
            // 配置表不可用不得阻断解析：降级为内置默认词典
            log.warn("[ResumeParseConfig] 读取配置失败 type={}，降级使用内置默认词典：{}", configType, e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public Map<String, String> loadSectionKeywordMap() {
        Map<String, String> map = new LinkedHashMap<>();
        for (PortalResumeParseConfig cfg : listActiveByType(TYPE_SECTION)) {
            String target = cfg.getItemKey();
            if (target == null || target.isBlank()) {
                continue;
            }
            for (String kw : splitKeywords(cfg.getKeywords())) {
                // 先出现者优先，便于后台用 sort 控制优先级
                map.putIfAbsent(kw, target.trim());
            }
            // 显示名本身也作为可匹配关键词（防止后台只填 itemName 没填 keywords）
            String name = cfg.getItemName();
            if (name != null && !name.isBlank()) {
                map.putIfAbsent(name.trim(), target.trim());
            }
        }
        return map;
    }

    @Override
    public List<String> loadKeywords(String configType) {
        List<String> out = new ArrayList<>();
        for (PortalResumeParseConfig cfg : listActiveByType(configType)) {
            out.addAll(splitKeywords(cfg.getKeywords()));
            // itemKey 在非 section 类型下即词条本身，一并纳入
            if (!TYPE_SECTION.equals(configType) && cfg.getItemKey() != null && !cfg.getItemKey().isBlank()) {
                String k = cfg.getItemKey().trim();
                if (!out.contains(k)) {
                    out.add(k);
                }
            }
        }
        return out;
    }

    /** 逗号/中文逗号/换行分隔 → 去空白去重 */
    private List<String> splitKeywords(String raw) {
        List<String> out = new ArrayList<>();
        if (raw == null || raw.isBlank()) {
            return out;
        }
        for (String part : raw.split("[,，\\n\\r]+")) {
            String k = part.trim();
            if (!k.isEmpty() && !out.contains(k)) {
                out.add(k);
            }
        }
        return out;
    }
}
