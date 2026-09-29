package com.moyun.ext.cms.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.portal.domain.entity.PortalResumeParseConfig;
import com.moyun.portal.service.IPortalResumeParseConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 简历解析配置管理 Controller
 *
 * <p>规则解析（{@code ResumeRuleParser}）的词表维护入口，四类配置：</p>
 * <ul>
 *   <li>{@code section}  章节标题词典 → 决定「大类划分」（教育/工作/项目/技能/自评…）</li>
 *   <li>{@code skill}    技能词域（规则引擎还会自动聚合 portal_job_template.required_skills）</li>
 *   <li>{@code degree}   学历词</li>
 *   <li>{@code position} 岗位词（与 portal_job_template.name 互补）</li>
 * </ul>
 *
 * <p><b>降级语义</b>：本表为空时规则引擎使用内置默认词典，<b>不会</b>导致解析失败；
 * 因此本页可放心整类停用或清空。</p>
 *
 * @author moyun
 */
@Tag(name = "简历解析配置管理")
@RestController
@RequestMapping("/cms/interview/resumeParseConfig")
public class CmsResumeParseConfigController extends BaseController {

    @Autowired
    private IPortalResumeParseConfigService resumeParseConfigService;

    @Operation(summary = "解析配置分页列表")
    @PreAuthorize("@ss.hasPermi('cms:interview:resumeParseConfig:list')")
    @GetMapping("/list")
    public AjaxResult list(PortalResumeParseConfig query,
                           @RequestParam(defaultValue = "1") Integer pageNum,
                           @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<PortalResumeParseConfig> page = new Page<>(pageNum, pageSize);
        page = resumeParseConfigService.page(page, new LambdaQueryWrapper<PortalResumeParseConfig>()
                .eq(query.getConfigType() != null && !query.getConfigType().isBlank(),
                        PortalResumeParseConfig::getConfigType, query.getConfigType())
                .like(query.getItemName() != null && !query.getItemName().isBlank(),
                        PortalResumeParseConfig::getItemName, query.getItemName())
                .eq(query.getStatus() != null && !query.getStatus().isBlank(),
                        PortalResumeParseConfig::getStatus, query.getStatus())
                .orderByAsc(PortalResumeParseConfig::getConfigType)
                .orderByAsc(PortalResumeParseConfig::getSort)
                .orderByAsc(PortalResumeParseConfig::getId));
        return success(page);
    }

    @Operation(summary = "按类型取全部启用配置（供规则引擎/下拉选择）")
    @PreAuthorize("@ss.hasPermi('cms:interview:resumeParseConfig:list')")
    @GetMapping("/byType/{configType}")
    public AjaxResult listByType(@PathVariable String configType) {
        List<PortalResumeParseConfig> list = resumeParseConfigService.listActiveByType(configType);
        return success(list);
    }

    @Operation(summary = "解析配置详情")
    @PreAuthorize("@ss.hasPermi('cms:interview:resumeParseConfig:query')")
    @GetMapping("/{id}")
    public AjaxResult getDetail(@PathVariable Long id) {
        return success(resumeParseConfigService.getById(id));
    }

    @Operation(summary = "新增解析配置")
    @PreAuthorize("@ss.hasPermi('cms:interview:resumeParseConfig:create')")
    @PostMapping
    public AjaxResult create(@RequestBody PortalResumeParseConfig config) {
        if (config.getConfigType() == null || config.getConfigType().isBlank()) {
            return error("配置类型不能为空");
        }
        if (config.getItemKey() == null || config.getItemKey().isBlank()) {
            return error("配置键不能为空");
        }
        if (config.getItemName() == null || config.getItemName().isBlank()) {
            return error("显示名称不能为空");
        }
        if (config.getStatus() == null || config.getStatus().isBlank()) {
            config.setStatus("active");
        }
        if (config.getSort() == null) {
            config.setSort(0);
        }
        // section 类型校验目标大类合法，避免写出规则引擎不认的键
        if (IPortalResumeParseConfigService.TYPE_SECTION.equals(config.getConfigType())
                && !isValidSectionKey(config.getItemKey())) {
            return error("章节目标大类非法，仅支持：basic/intention/edu/work/project/skill/self/other");
        }
        return toAjax(resumeParseConfigService.save(config));
    }

    @Operation(summary = "修改解析配置")
    @PreAuthorize("@ss.hasPermi('cms:interview:resumeParseConfig:update')")
    @PutMapping
    public AjaxResult update(@RequestBody PortalResumeParseConfig config) {
        if (config.getId() == null) {
            return error("ID不能为空");
        }
        if (IPortalResumeParseConfigService.TYPE_SECTION.equals(config.getConfigType())
                && config.getItemKey() != null && !config.getItemKey().isBlank()
                && !isValidSectionKey(config.getItemKey())) {
            return error("章节目标大类非法，仅支持：basic/intention/edu/work/project/skill/self/other");
        }
        return toAjax(resumeParseConfigService.updateById(config));
    }

    @Operation(summary = "删除解析配置")
    @PreAuthorize("@ss.hasPermi('cms:interview:resumeParseConfig:remove')")
    @DeleteMapping("/{id}")
    public AjaxResult remove(@PathVariable Long id) {
        return toAjax(resumeParseConfigService.removeById(id));
    }

    /** 章节目标大类白名单（与 ResumeRuleParser 的 Section 枚举保持一致） */
    private boolean isValidSectionKey(String key) {
        return switch (key.trim()) {
            case "basic", "intention", "edu", "work", "project", "skill", "self", "other" -> true;
            default -> false;
        };
    }
}
