package com.moyun.portal.controller;

import com.moyun.common.annotation.Log;
import com.moyun.common.enums.BusinessType;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.ext.cms.domain.query.TopicQuery;
import com.moyun.ext.cms.service.ITopicService;
import com.moyun.portal.domain.entity.PortalTopic;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 话题/超话 后台 Controller（社交深化与商业化 4.2）
 *
 * <p>供后台管理页面调用，提供话题 CRUD 能力。</p>
 *
 * <p>路径前缀 /cms/topic，权限标识 portal:topic:list / portal:topic:add / portal:topic:edit / portal:topic:remove。</p>
 *
 * @author moyun
 */
@Tag(name = "话题管理", description = "话题后台 CRUD 接口")
@RestController
@RequestMapping("/cms/topic")
public class CmsTopicController extends BaseController {

    @Autowired
    private ITopicService topicService;

    @Operation(summary = "话题分页列表", description = "分页查询话题，支持按状态、关键词筛选")
    @PreAuthorize("@ss.hasPermi('portal:topic:list')")
    @GetMapping("/list")
    public AjaxResult list(TopicQuery query) {
        return AjaxResult.success(topicService.cmsListTopics(query));
    }

    @Operation(summary = "新增话题", description = "新增话题（slug 可选，未提供则按名称生成）")
    @PreAuthorize("@ss.hasPermi('portal:topic:add')")
    @Log(title = "话题新增", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody PortalTopic topic) {
        try {
            return AjaxResult.success(topicService.cmsAddTopic(topic));
        } catch (RuntimeException e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    @Operation(summary = "修改话题", description = "修改话题信息")
    @PreAuthorize("@ss.hasPermi('portal:topic:edit')")
    @Log(title = "话题修改", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody PortalTopic topic) {
        try {
            return AjaxResult.success(topicService.cmsUpdateTopic(topic));
        } catch (RuntimeException e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    @Operation(summary = "删除话题", description = "删除话题（级联删除关注记录）")
    @PreAuthorize("@ss.hasPermi('portal:topic:remove')")
    @Log(title = "话题删除", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public AjaxResult remove(@PathVariable Long id) {
        return AjaxResult.success(topicService.cmsDeleteTopic(id));
    }
}
