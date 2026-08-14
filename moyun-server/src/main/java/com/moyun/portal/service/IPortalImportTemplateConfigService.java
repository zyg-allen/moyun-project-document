package com.moyun.portal.service;

import com.moyun.portal.domain.entity.PortalImportTemplateConfig;

import java.util.List;

/**
 * 导入模板字段配置 服务层
 * <p>
 * 提供模板字段查询与保存能力：
 * - 查询：供 {@code importTemplate} 接口动态生成 Excel 模板表头/说明/示例
 * - 保存：供运营在「导入模板配置」菜单维护字段（整批覆盖保存）
 *
 * @author moyun
 */
public interface IPortalImportTemplateConfigService {

    /**
     * 查询某业务启用的字段配置（生成模板用，按 sort 升序）
     *
     * @param businessKey 业务标识
     * @return 启用的字段配置列表
     */
    List<PortalImportTemplateConfig> selectEnabledByBusinessKey(String businessKey);

    /**
     * 查询某业务全部字段配置（含停用，运营管理用）
     *
     * @param businessKey 业务标识
     * @return 全部字段配置列表
     */
    List<PortalImportTemplateConfig> selectAllByBusinessKey(String businessKey);

    /**
     * 整批保存某业务的字段配置（先删后插，事务保证原子性）
     * <p>
     * 用于运营在配置页一次性提交全部字段，避免逐条维护的复杂度。
     *
     * @param businessKey 业务标识
     * @param configs     字段配置列表
     * @param operName    操作人
     * @return 保存后的字段配置列表（含新生成的 id）
     */
    List<PortalImportTemplateConfig> saveBatch(String businessKey, List<PortalImportTemplateConfig> configs, String operName);
}
