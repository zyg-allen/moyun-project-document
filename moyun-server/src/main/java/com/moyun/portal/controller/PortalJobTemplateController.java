package com.moyun.portal.controller;

import com.moyun.common.annotation.Anonymous;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.ext.cms.service.IPortalJobTemplateService;
import com.moyun.portal.domain.entity.PortalJobTemplate;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 岗位模板 Controller（门户端，只读公开）
 *
 * <p><b>全 portal 岗位配置的唯一读取入口</b>。</p>
 *
 * <p>消费场景：语音面试准备页岗位下拉（选中后回填「岗位要求」JD / 难度 / 题量）、
 * 用户档案目标岗位选择、简历岗位匹配评分、画像抽题。</p>
 *
 * @author moyun
 */
@Tag(name = "岗位模板", description = "岗位配置查询接口（驱动岗位选择、JD 回填与画像抽题）")
@RestController
@RequestMapping("/portal/interview/jobTemplate")
public class PortalJobTemplateController extends BaseController {

    @Autowired
    private IPortalJobTemplateService jobTemplateService;

    @Operation(summary = "获取启用的岗位模板列表",
            description = "按 sort/category/name 升序返回所有 status=active 的岗位模板；"
                    + "仅暴露前端选岗与回填所需字段，含 jobDescription（岗位 JD 原文）、"
                    + "difficulty、questionCount、requiredSkills、hotCompanies")
    @GetMapping("/list")
    @Anonymous
    public AjaxResult listActive() {
        List<PortalJobTemplate> templates = jobTemplateService.listActive();
        List<Map<String, Object>> views = new ArrayList<>();
        for (PortalJobTemplate t : templates) {
            Map<String, Object> vo = new LinkedHashMap<>();
            vo.put("id", t.getId());
            vo.put("name", t.getName());
            vo.put("code", t.getCode());
            vo.put("category", t.getCategory());
            vo.put("industry", t.getIndustry());
            vo.put("level", t.getLevel());
            vo.put("description", t.getDescription());
            // 统一以 jobDescription 暴露 JD 原文（前端据此回填「岗位要求」并允许用户修改）
            vo.put("jobDescription", t.getJdText());
            vo.put("difficulty", t.getDifficulty());
            vo.put("questionCount", t.getQuestionCount());
            vo.put("requiredSkills", t.getRequiredSkills());
            vo.put("hotCompanies", t.getHotCompanies());
            vo.put("sort", t.getSort());
            views.add(vo);
        }
        return AjaxResult.success(views);
    }
}
