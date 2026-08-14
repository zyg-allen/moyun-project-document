package com.moyun.portal.service.impl;

import com.moyun.portal.domain.entity.PortalImportTemplateConfig;
import com.moyun.portal.mapper.PortalImportTemplateConfigMapper;
import com.moyun.portal.service.IPortalImportTemplateConfigService;
import com.moyun.util.string.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 导入模板字段配置 服务实现
 *
 * @author moyun
 */
@Service
public class PortalImportTemplateConfigServiceImpl implements IPortalImportTemplateConfigService {

    @Autowired
    private PortalImportTemplateConfigMapper configMapper;

    @Override
    public List<PortalImportTemplateConfig> selectEnabledByBusinessKey(String businessKey) {
        if (StringUtils.isEmpty(businessKey)) {
            return new ArrayList<>();
        }
        return configMapper.selectEnabledByBusinessKey(businessKey);
    }

    @Override
    public List<PortalImportTemplateConfig> selectAllByBusinessKey(String businessKey) {
        if (StringUtils.isEmpty(businessKey)) {
            return new ArrayList<>();
        }
        return configMapper.selectAllByBusinessKey(businessKey);
    }

    /**
     * 整批保存：先删后插，事务保证原子性
     * <p>
     * 实现说明：
     * 1. businessKey 必填校验
     * 2. 删除该业务下所有旧配置（含停用）
     * 3. 为新配置补全 businessKey / createBy / createTime / 默认值，逐条插入
     * 4. 返回插入后的列表（含新生成的 id），便于前端一次拿到完整配置
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<PortalImportTemplateConfig> saveBatch(String businessKey, List<PortalImportTemplateConfig> configs, String operName) {
        if (StringUtils.isEmpty(businessKey)) {
            throw new IllegalArgumentException("businessKey 不能为空");
        }
        // 先删旧
        configMapper.deleteByBusinessKey(businessKey);
        // 再插新
        List<PortalImportTemplateConfig> result = new ArrayList<>();
        if (configs == null || configs.isEmpty()) {
            return result;
        }
        LocalDateTime now = LocalDateTime.now();
        int sort = 1;
        for (PortalImportTemplateConfig c : configs) {
            c.setId(null); // 强制新增，避免误更新
            c.setBusinessKey(businessKey);
            c.setCreateBy(operName);
            c.setCreateTime(now);
            c.setUpdateBy(operName);
            c.setUpdateTime(now);
            // 默认值兜底
            if (c.getRequired() == null) c.setRequired(0);
            if (StringUtils.isEmpty(c.getStatus())) c.setStatus("0");
            if (StringUtils.isEmpty(c.getFieldType())) c.setFieldType("string");
            if (c.getColumnWidth() == null) c.setColumnWidth(20);
            if (c.getSort() == null) c.setSort(sort);
            sort++;
            configMapper.insert(c);
            result.add(c);
        }
        return result;
    }
}
