package com.moyun.portal.controller;

import com.moyun.common.annotation.Anonymous;
import com.moyun.common.constant.HttpStatus;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.core.base.page.PageDomain;
import com.moyun.ext.cms.domain.query.TopicQuery;
import com.moyun.ext.cms.domain.vo.TopicListItemVO;
import com.moyun.ext.cms.domain.vo.TopicPostVO;
import com.moyun.ext.cms.domain.vo.TopicVO;
import com.moyun.ext.cms.service.ITopicService;
import com.moyun.portal.util.PortalSecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 话题/超话 Controller（门户端，社交深化与商业化 4.2）
 * <p>
 * 公开接口：列表、热门、详情、话题动态；关注接口需登录。
 * 基于 portal_entity_tag 扩展，话题动态聚合带该话题标签的文章。
 *
 * @author moyun
 */
@Tag(name = "话题/超话", description = "话题聚合、关注、话题动态")
@RestController
@RequestMapping("/portal/topic")
public class PortalTopicController extends BaseController {

    @Autowired
    private ITopicService topicService;

    private Long currentUserId() {
        return PortalSecurityUtils.getUserId();
    }

    // ==================== 公开接口 ====================

    @Operation(summary = "话题列表", description = "公开分页查询话题（支持关键词、首字母筛选）")
    @GetMapping("/list")
    @Anonymous
    public AjaxResult list(TopicQuery query) {
        return AjaxResult.success(topicService.listTopics(query));
    }

    @Operation(summary = "热门话题", description = "公开分页查询热门话题（按关注数倒序）")
    @GetMapping("/hot")
    @Anonymous
    public AjaxResult hot(PageDomain query) {
        return AjaxResult.success(topicService.hotTopics(query));
    }

    @Operation(summary = "话题详情", description = "公开查询话题详情（含当前用户是否关注）")
    @GetMapping("/{slug}")
    @Anonymous
    public AjaxResult detail(@PathVariable("slug") String slug) {
        TopicVO vo = topicService.getTopicDetail(slug, currentUserId());
        if (vo == null) {
            return AjaxResult.error("话题不存在");
        }
        return AjaxResult.success(vo);
    }

    @Operation(summary = "话题动态", description = "公开分页查询话题下的文章（基于 portal_entity_tag 聚合）")
    @GetMapping("/{slug}/posts")
    @Anonymous
    public AjaxResult posts(@PathVariable("slug") String slug, PageDomain query) {
        return AjaxResult.success(topicService.listTopicPosts(slug, query));
    }

    // ==================== 关注（需登录） ====================

    @Operation(summary = "关注/取消关注话题", description = "toggle 切换关注状态，返回操作后的关注状态")
    @PostMapping("/{topicId}/follow")
    public AjaxResult follow(@PathVariable("topicId") Long topicId) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        return AjaxResult.success(topicService.toggleFollow(topicId, userId));
    }
}
