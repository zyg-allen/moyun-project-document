package com.moyun.ext.cms.controller;

import java.util.Map;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.moyun.common.annotation.Log;
import com.moyun.common.enums.BusinessType;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.portal.domain.vo.TopicCommentVO;
import com.moyun.portal.domain.vo.TopicListVO;
import com.moyun.portal.domain.vo.TopicPostVO;
import com.moyun.portal.domain.vo.TopicVO;
import com.moyun.portal.service.IPortalTopicCommentService;
import com.moyun.portal.service.IPortalTopicPostService;
import com.moyun.portal.service.IPortalTopicService;

/**
 * CMS 话题后台管理 Controller
 *
 * <p>路径前缀 /cms/topic。提供话题/观点/评论的后台管理能力：
 * 列表查询、状态流转、置顶、加精、删除等。
 * 所有接口均通过 @PreAuthorize 校验 CMS 权限。</p>
 *
 * @author moyun
 */
@Tag(name = "CMS话题管理", description = "话题讨论模块后台管理接口")
@RestController
@RequestMapping("/cms/topic")
public class CmsTopicController extends BaseController {

    @Autowired
    private IPortalTopicService portalTopicService;

    @Autowired
    private IPortalTopicPostService portalTopicPostService;

    @Autowired
    private IPortalTopicCommentService portalTopicCommentService;

    // ==================== 话题相关 ====================

    @Operation(summary = "查询话题列表", description = "分页查询所有话题（含所有状态，支持关键词和状态筛选）")
    @PreAuthorize("@ss.hasAnyPermi('cms:topic:list,cms:topic:audit')")
    @GetMapping("/list")
    public AjaxResult list(@RequestParam(defaultValue = "1") Integer pageNum,
                           @RequestParam(defaultValue = "10") Integer pageSize,
                           @RequestParam(required = false) String keyword,
                           @RequestParam(required = false) String status) {
        Page<TopicListVO> page = portalTopicService.getCmsTopicList(pageNum, pageSize, keyword, status);
        return success(page);
    }

    @Operation(summary = "获取话题详情", description = "根据ID获取话题详情")
    @PreAuthorize("@ss.hasAnyPermi('cms:topic:query,cms:topic:audit')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@Parameter(description = "话题ID") @PathVariable Long id) {
        TopicVO vo = portalTopicService.getTopicDetail(id, null);
        if (vo == null) {
            return error("话题不存在");
        }
        return success(vo);
    }

    @Operation(summary = "更新话题状态", description = "active/archived/deleted 状态流转")
    @PreAuthorize("@ss.hasPermi('cms:topic:edit')")
    @Log(title = "话题管理", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}/status")
    public AjaxResult changeStatus(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        String status = body.get("status") == null ? null : String.valueOf(body.get("status"));
        try {
            portalTopicService.updateTopicStatus(id, status);
            return success();
        } catch (RuntimeException e) {
            return error(e.getMessage() != null ? e.getMessage() : "状态更新失败");
        }
    }

    @Operation(summary = "审核话题", description = "审核待处理话题：active=通过 / rejected=驳回，支持审核意见，结果通知发起人")
    @PreAuthorize("@ss.hasPermi('cms:topic:audit')")
    @Log(title = "话题审核", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}/audit")
    public AjaxResult audit(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        String status = body.get("status") == null ? null : String.valueOf(body.get("status"));
        String auditRemark = body.get("auditRemark") == null ? null : String.valueOf(body.get("auditRemark"));
        Long auditorId = getUserId();
        try {
            portalTopicService.auditTopic(id, status, auditRemark, auditorId);
            return success();
        } catch (RuntimeException e) {
            return error(e.getMessage() != null ? e.getMessage() : "审核失败");
        }
    }

    @Operation(summary = "置顶/取消置顶", description = "更新话题 pinned 字段：0 否/1 是")
    @PreAuthorize("@ss.hasPermi('cms:topic:edit')")
    @Log(title = "话题管理", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}/pinned")
    public AjaxResult changePinned(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Object pinnedObj = body.get("pinned");
        Integer pinned = null;
        if (pinnedObj != null) {
            try {
                pinned = Integer.valueOf(String.valueOf(pinnedObj));
            } catch (NumberFormatException ignored) {
            }
        }
        try {
            portalTopicService.updateTopicPinned(id, pinned);
            return success();
        } catch (RuntimeException e) {
            return error(e.getMessage() != null ? e.getMessage() : "置顶更新失败");
        }
    }

    @Operation(summary = "加精话题", description = "将话题设为精选（触发 topic_featured 成长事件）")
    @PreAuthorize("@ss.hasPermi('cms:topic:edit')")
    @Log(title = "话题管理", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}/featured")
    public AjaxResult feature(@PathVariable Long id) {
        try {
            portalTopicService.featureTopic(id);
            return success();
        } catch (RuntimeException e) {
            return error(e.getMessage() != null ? e.getMessage() : "加精失败");
        }
    }

    @Operation(summary = "删除话题", description = "CMS 后台软删话题（status=deleted）")
    @PreAuthorize("@ss.hasPermi('cms:topic:remove')")
    @Log(title = "话题管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public AjaxResult remove(@Parameter(description = "话题ID") @PathVariable Long id) {
        try {
            portalTopicService.deleteTopic(id, getUserId());
            return success();
        } catch (RuntimeException e) {
            return error(e.getMessage() != null ? e.getMessage() : "删除失败");
        }
    }

    // ==================== 观点相关 ====================

    @Operation(summary = "查询观点列表", description = "分页查询所有话题的观点（可按话题ID筛选）")
    @PreAuthorize("@ss.hasPermi('cms:topic:post')")
    @GetMapping("/post/list")
    public AjaxResult postList(@RequestParam(defaultValue = "1") Integer pageNum,
                               @RequestParam(defaultValue = "10") Integer pageSize,
                               @RequestParam(required = false) Long topicId) {
        Page<TopicPostVO> page = portalTopicPostService.getCmsPostList(pageNum, pageSize, topicId);
        return success(page);
    }

    @Operation(summary = "删除观点", description = "CMS 后台软删观点")
    @PreAuthorize("@ss.hasPermi('cms:topic:post:remove')")
    @Log(title = "话题观点管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/post/{postId}")
    public AjaxResult removePost(@Parameter(description = "观点ID") @PathVariable Long postId) {
        try {
            portalTopicPostService.cmsDeletePost(postId);
            return success();
        } catch (RuntimeException e) {
            return error(e.getMessage() != null ? e.getMessage() : "删除失败");
        }
    }

    // ==================== 评论相关 ====================

    @Operation(summary = "查询评论列表", description = "分页查询所有目标的评论（可按 targetType/targetId 筛选）")
    @PreAuthorize("@ss.hasPermi('cms:topic:comment')")
    @GetMapping("/comment/list")
    public AjaxResult commentList(@RequestParam(defaultValue = "1") Integer pageNum,
                                  @RequestParam(defaultValue = "10") Integer pageSize,
                                  @RequestParam(required = false) String targetType,
                                  @RequestParam(required = false) Long targetId) {
        Page<TopicCommentVO> page = portalTopicCommentService.getCmsCommentList(pageNum, pageSize, targetType, targetId);
        return success(page);
    }

    @Operation(summary = "删除评论", description = "CMS 后台软删评论（级联软删回复）")
    @PreAuthorize("@ss.hasPermi('cms:topic:comment:remove')")
    @Log(title = "话题评论管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/comment/{commentId}")
    public AjaxResult removeComment(@Parameter(description = "评论ID") @PathVariable Long commentId) {
        try {
            portalTopicCommentService.cmsDeleteComment(commentId);
            return success();
        } catch (RuntimeException e) {
            return error(e.getMessage() != null ? e.getMessage() : "删除失败");
        }
    }
}
