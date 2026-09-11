package com.moyun.ext.cms.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.ext.cms.service.IPortalJobTemplateService;
import com.moyun.portal.domain.entity.PortalInterviewQuestion;
import com.moyun.portal.mapper.PortalInterviewQuestionMapper;
import com.moyun.portal.domain.entity.PortalJobTemplate;
import com.moyun.util.bean.PageUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 岗位模板管理 Controller（v11.x 智能出题）
 *
 * <p>JD/关键词（LLM 提取）/出题权重/关联题目管理。</p>
 *
 * @author moyun
 */
@Tag(name = "岗位模板管理")
@RestController
@RequestMapping("/cms/interview/jobTemplate")
public class CmsJobTemplateController extends BaseController {

    @Autowired
    private IPortalJobTemplateService jobTemplateService;

    @Autowired
    private PortalInterviewQuestionMapper questionMapper;

    @Operation(summary = "岗位模板分页列表")
    @PreAuthorize("@ss.hasPermi('cms:interview:jobTemplate:list')")
    @GetMapping("/list")
    public AjaxResult list(PortalJobTemplate query,
                           @RequestParam(defaultValue = "1") Integer pageNum,
                           @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<PortalJobTemplate> page = new Page<>(pageNum, pageSize);
        page = jobTemplateService.page(page, new LambdaQueryWrapper<PortalJobTemplate>()
                .like(query.getName() != null && !query.getName().isBlank(),
                        PortalJobTemplate::getName, query.getName())
                .eq(query.getCategory() != null && !query.getCategory().isBlank(),
                        PortalJobTemplate::getCategory, query.getCategory())
                .eq(query.getStatus() != null && !query.getStatus().isBlank(),
                        PortalJobTemplate::getStatus, query.getStatus())
                .orderByDesc(PortalJobTemplate::getCreateTime));
        return success(page);
    }

    @Operation(summary = "岗位模板详情")
    @PreAuthorize("@ss.hasPermi('cms:interview:jobTemplate:query')")
    @GetMapping("/{id}")
    public AjaxResult getDetail(@PathVariable Long id) {
        return success(jobTemplateService.getById(id));
    }

    @Operation(summary = "新增岗位模板")
    @PreAuthorize("@ss.hasPermi('cms:interview:jobTemplate:create')")
    @PostMapping
    public AjaxResult create(@RequestBody PortalJobTemplate template) {
        if (template.getName() == null || template.getName().isBlank()) {
            return error("模板名称不能为空");
        }
        if (template.getStatus() == null || template.getStatus().isBlank()) {
            template.setStatus("active");
        }
        return toAjax(jobTemplateService.save(template));
    }

    @Operation(summary = "修改岗位模板")
    @PreAuthorize("@ss.hasPermi('cms:interview:jobTemplate:update')")
    @PutMapping
    public AjaxResult update(@RequestBody PortalJobTemplate template) {
        if (template.getId() == null) {
            return error("ID不能为空");
        }
        return toAjax(jobTemplateService.updateById(template));
    }

    @Operation(summary = "删除岗位模板")
    @PreAuthorize("@ss.hasPermi('cms:interview:jobTemplate:remove')")
    @DeleteMapping("/{id}")
    public AjaxResult remove(@PathVariable Long id) {
        return toAjax(jobTemplateService.removeById(id));
    }

    @Operation(summary = "JD 关键词 LLM 提取（失败自动回退规则分词）")
    @PreAuthorize("@ss.hasPermi('cms:interview:jobTemplate:create')")
    @PostMapping("/extract-keywords")
    public AjaxResult extractKeywords(@RequestBody Map<String, String> body) {
        String jdText = body == null ? null : body.get("jdText");
        List<String> keywords = jobTemplateService.extractKeywords(jdText);
        return success(keywords);
    }

    @Operation(summary = "关联题目（全量覆盖题目归属）")
    @PreAuthorize("@ss.hasPermi('cms:interview:jobTemplate:update')")
    @PostMapping("/{id}/questions")
    public AjaxResult bindQuestions(@PathVariable Long id, @RequestBody List<Long> questionIds) {
        return success(jobTemplateService.bindQuestions(id, questionIds));
    }

    @Operation(summary = "获取模板已关联题目ID列表")
    @PreAuthorize("@ss.hasPermi('cms:interview:jobTemplate:query')")
    @GetMapping("/{id}/questions")
    public AjaxResult getBoundQuestions(@PathVariable Long id) {
        return success(questionMapper.selectList(new LambdaQueryWrapper<PortalInterviewQuestion>()
                .eq(PortalInterviewQuestion::getJobTemplateId, id)
                .select(PortalInterviewQuestion::getId))
                .stream().map(PortalInterviewQuestion::getId).collect(java.util.stream.Collectors.toList()));
    }
}