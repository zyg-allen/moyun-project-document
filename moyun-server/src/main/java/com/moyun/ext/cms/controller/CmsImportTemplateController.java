package com.moyun.ext.cms.controller;

import com.moyun.common.annotation.Log;
import com.moyun.common.enums.BusinessType;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.portal.domain.entity.PortalImportTemplateConfig;
import com.moyun.portal.service.IPortalImportTemplateConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 导入模板字段配置 Controller（后台）
 * <p>
 * 供运营在「导入模板配置」菜单维护各业务 Excel 导入模板的字段说明、示例、必填、字典等。
 * 配置完成后，对应业务的 {@code /importTemplate} 接口会优先读此配置生成动态模板。
 *
 * @author moyun
 */
@Tag(name = "导入模板配置", description = "动态维护各业务 Excel 导入模板字段配置")
@RestController
@RequestMapping("/cms/import-template")
public class CmsImportTemplateController extends BaseController {

    @Autowired
    private IPortalImportTemplateConfigService configService;

    /**
     * 查询某业务的全部字段配置（含停用，运营管理用）
     */
    @Operation(summary = "查询模板字段配置", description = "按业务标识查询全部字段配置（含停用）")
    @PreAuthorize("@ss.hasPermi('cms:importTemplate:list')")
    @GetMapping("/list")
    public AjaxResult list(@Parameter(description = "业务标识") @RequestParam String businessKey) {
        List<PortalImportTemplateConfig> list = configService.selectAllByBusinessKey(businessKey);
        return success(list);
    }

    /**
     * 整批保存某业务的字段配置（先删后插）
     */
    @Operation(summary = "保存模板字段配置", description = "整批保存某业务的字段配置（先删后插，事务保证原子性）")
    @PreAuthorize("@ss.hasPermi('cms:importTemplate:edit')")
    @Log(title = "导入模板配置", businessType = BusinessType.UPDATE)
    @PostMapping("/save")
    public AjaxResult save(@RequestBody SaveBody body) {
        if (body.getBusinessKey() == null || body.getBusinessKey().trim().isEmpty()) {
            return error("businessKey 不能为空");
        }
        List<PortalImportTemplateConfig> saved = configService.saveBatch(
                body.getBusinessKey().trim(),
                body.getConfigs(),
                getUsername()
        );
        return success(saved);
    }

    /**
     * 请求体：业务标识 + 字段配置列表
     */
    @lombok.Data
    public static class SaveBody {
        @Parameter(description = "业务标识")
        private String businessKey;
        @Parameter(description = "字段配置列表")
        private List<PortalImportTemplateConfig> configs;
    }
}
