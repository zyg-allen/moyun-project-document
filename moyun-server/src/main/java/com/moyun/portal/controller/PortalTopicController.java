package com.moyun.portal.controller;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.moyun.common.annotation.Log;
import com.moyun.common.constant.HttpStatus;
import com.moyun.common.enums.BusinessType;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.portal.domain.dto.TopicPostCreateDTO;
import com.moyun.portal.domain.entity.PortalTopic;
import com.moyun.portal.domain.entity.PortalTopicComment;
import com.moyun.portal.domain.entity.PortalTopicPost;
import com.moyun.portal.domain.vo.TopicCommentVO;
import com.moyun.portal.domain.vo.TopicListVO;
import com.moyun.portal.domain.vo.TopicPostVO;
import com.moyun.portal.domain.vo.TopicVO;
import com.moyun.portal.service.IPortalTopicCommentService;
import com.moyun.portal.service.IPortalTopicPostService;
import com.moyun.portal.service.IPortalTopicService;
import com.moyun.portal.util.PortalSecurityUtils;

/**
 * 门户话题 Controller（前台）
 *
 * <p>路径前缀 /portal/topic。公开接口：list / detail / posts / comment list。
 * 其他接口需登录，由 PortalSecurityUtils.getUserId() 校验。</p>
 *
 * @author moyun
 */
@Tag(name = "门户话题", description = "门户话题讨论模块接口")
@RestController
@RequestMapping("/portal/topic")
public class PortalTopicController extends BaseController {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private IPortalTopicService portalTopicService;

    @Autowired
    private IPortalTopicPostService portalTopicPostService;

    @Autowired
    private IPortalTopicCommentService portalTopicCommentService;

    // ==================== 话题相关 ====================

    @Operation(summary = "话题列表", description = "分页查询话题，支持 latest/hot/active 排序")
    @GetMapping("/list")
    public AjaxResult list(@RequestParam(defaultValue = "1") Integer pageNum,
                           @RequestParam(defaultValue = "10") Integer pageSize,
                           @RequestParam(defaultValue = "latest") String sort,
                           @RequestParam(required = false) String keyword) {
        Page<TopicListVO> page = portalTopicService.getTopicList(pageNum, pageSize, sort, keyword);
        return success(page);
    }

    @Operation(summary = "话题详情", description = "根据ID获取话题详情")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@Parameter(description = "话题ID") @PathVariable Long id) {
        Long currentUserId = PortalSecurityUtils.getUserId();
        TopicVO vo = portalTopicService.getTopicDetail(id, currentUserId);
        if (vo == null) {
            return error("话题不存在");
        }
        // 浏览数 +1（异步触发，不影响响应）
        try {
            portalTopicService.incrementViewCount(id);
        } catch (Exception ignored) {
        }
        return success(vo);
    }

    @Operation(summary = "创建话题", description = "认证创作者发起话题")
    @Log(title = "门户话题", businessType = BusinessType.INSERT)
    @PostMapping("/save")
    public AjaxResult save(@RequestBody PortalTopic topic) {
        Long userId = PortalSecurityUtils.getUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "请先登录");
        }
        try {
            PortalTopic created = portalTopicService.createTopic(topic, userId);
            return success(created);
        } catch (RuntimeException e) {
            return AjaxResult.error(e.getMessage() != null ? e.getMessage() : "创建失败");
        }
    }

    @Operation(summary = "编辑话题", description = "仅话题发起人可编辑")
    @Log(title = "门户话题", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}")
    public AjaxResult edit(@PathVariable Long id, @RequestBody PortalTopic topic) {
        Long userId = PortalSecurityUtils.getUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "请先登录");
        }
        try {
            PortalTopic updated = portalTopicService.updateTopic(id, topic, userId);
            return success(updated);
        } catch (RuntimeException e) {
            return AjaxResult.error(e.getMessage() != null ? e.getMessage() : "修改失败");
        }
    }

    @Operation(summary = "删除话题", description = "creator 或 admin 可删除（软删）")
    @Log(title = "门户话题", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public AjaxResult remove(@PathVariable Long id) {
        Long userId = PortalSecurityUtils.getUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "请先登录");
        }
        try {
            portalTopicService.deleteTopic(id, userId);
            return success();
        } catch (RuntimeException e) {
            return AjaxResult.error(e.getMessage() != null ? e.getMessage() : "删除失败");
        }
    }

    @Operation(summary = "话题点赞/取消", description = "幂等 toggle，返回 isLiked 和 likeCount")
    @PostMapping("/{id}/like")
    public AjaxResult toggleTopicLike(@PathVariable Long id) {
        Long userId = PortalSecurityUtils.getUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "请先登录");
        }
        try {
            Map<String, Object> result = portalTopicService.toggleTopicLike(id, userId);
            return success(result);
        } catch (RuntimeException e) {
            return AjaxResult.error(e.getMessage() != null ? e.getMessage() : "操作失败");
        }
    }

    @Operation(summary = "我发起的话题", description = "查询当前登录用户发起的话题列表")
    @GetMapping("/my/topics")
    public AjaxResult myTopics(@RequestParam(defaultValue = "1") Integer pageNum,
                               @RequestParam(defaultValue = "10") Integer pageSize) {
        Long userId = PortalSecurityUtils.getUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "请先登录");
        }
        Page<TopicListVO> page = portalTopicService.getMyTopics(pageNum, pageSize, userId);
        return success(page);
    }

    // ==================== 观点相关 ====================

    @Operation(summary = "话题观点列表", description = "分页查询某话题的观点（按楼层正序）")
    @GetMapping("/{id}/posts")
    public AjaxResult getPosts(@Parameter(description = "话题ID") @PathVariable Long id,
                               @RequestParam(defaultValue = "1") Integer pageNum,
                               @RequestParam(defaultValue = "10") Integer pageSize) {
        Long currentUserId = PortalSecurityUtils.getUserId();
        Page<TopicPostVO> page = portalTopicPostService.getPostsByTopic(id, pageNum, pageSize, currentUserId);
        return success(page);
    }

    @Operation(summary = "发表观点", description = "在某话题下发表观点（楼层号并发安全）")
    @Log(title = "话题观点", businessType = BusinessType.INSERT)
    @PostMapping("/{id}/post")
    public AjaxResult createPost(@Parameter(description = "话题ID") @PathVariable Long id,
                                 @Validated @RequestBody TopicPostCreateDTO dto) {
        Long userId = PortalSecurityUtils.getUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "请先登录");
        }
        PortalTopicPost post = new PortalTopicPost();
        post.setContent(dto.getContent());
        // images 字段：前端传 List<String>，序列化为 JSON 字符串持久化
        if (dto.getImages() != null && !dto.getImages().isEmpty()) {
            try {
                post.setImages(OBJECT_MAPPER.writeValueAsString(dto.getImages()));
            } catch (Exception ignored) {
                post.setImages(null);
            }
        }
        // 父观点 ID（楼中楼回复）
        post.setParentPostId(dto.getParentPostId());
        // 回复的目标用户 ID
        post.setReplyToUserId(dto.getReplyToUserId());
        try {
            PortalTopicPost created = portalTopicPostService.createPost(id, post, userId);
            return success(created);
        } catch (RuntimeException e) {
            return AjaxResult.error(e.getMessage() != null ? e.getMessage() : "发表失败");
        }
    }

    @Operation(summary = "删除观点", description = "作者/话题发起人/admin 可删除（软删）")
    @Log(title = "话题观点", businessType = BusinessType.DELETE)
    @DeleteMapping("/post/{postId}")
    public AjaxResult deletePost(@Parameter(description = "观点ID") @PathVariable Long postId) {
        Long userId = PortalSecurityUtils.getUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "请先登录");
        }
        try {
            portalTopicPostService.deletePost(postId, userId);
            return success();
        } catch (RuntimeException e) {
            return AjaxResult.error(e.getMessage() != null ? e.getMessage() : "删除失败");
        }
    }

    @Operation(summary = "观点点赞/取消", description = "幂等 toggle，返回 isLiked 和 likeCount")
    @PostMapping("/post/{postId}/like")
    public AjaxResult togglePostLike(@Parameter(description = "观点ID") @PathVariable Long postId) {
        Long userId = PortalSecurityUtils.getUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "请先登录");
        }
        try {
            Map<String, Object> result = portalTopicPostService.togglePostLike(postId, userId);
            return success(result);
        } catch (RuntimeException e) {
            return AjaxResult.error(e.getMessage() != null ? e.getMessage() : "操作失败");
        }
    }

    @Operation(summary = "我发表的观点", description = "查询当前登录用户发表的观点列表")
    @GetMapping("/my/posts")
    public AjaxResult myPosts(@RequestParam(defaultValue = "1") Integer pageNum,
                              @RequestParam(defaultValue = "10") Integer pageSize) {
        Long userId = PortalSecurityUtils.getUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "请先登录");
        }
        Page<TopicPostVO> page = portalTopicPostService.getMyPosts(pageNum, pageSize, userId);
        return success(page);
    }

    // ==================== 评论相关 ====================

    @Operation(summary = "评论列表", description = "分页查询某目标的评论（两级楼中楼）")
    @GetMapping("/comment/list")
    public AjaxResult commentList(@RequestParam String targetType,
                                  @RequestParam Long targetId,
                                  @RequestParam(defaultValue = "1") Integer pageNum,
                                  @RequestParam(defaultValue = "10") Integer pageSize) {
        Long currentUserId = PortalSecurityUtils.getUserId();
        Page<TopicCommentVO> page = portalTopicCommentService.getComments(targetType, targetId, pageNum, pageSize, currentUserId);
        return success(page);
    }

    @Operation(summary = "发表评论", description = "在话题或观点下发表评论")
    @Log(title = "话题评论", businessType = BusinessType.INSERT)
    @PostMapping("/comment")
    public AjaxResult createComment(@RequestBody PortalTopicComment comment) {
        Long userId = PortalSecurityUtils.getUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "请先登录");
        }
        try {
            PortalTopicComment created = portalTopicCommentService.createComment(comment, userId);
            return success(created);
        } catch (RuntimeException e) {
            return AjaxResult.error(e.getMessage() != null ? e.getMessage() : "评论失败");
        }
    }

    @Operation(summary = "删除评论", description = "作者/admin 可删除（软删，级联软删回复）")
    @Log(title = "话题评论", businessType = BusinessType.DELETE)
    @DeleteMapping("/comment/{commentId}")
    public AjaxResult deleteComment(@Parameter(description = "评论ID") @PathVariable Long commentId) {
        Long userId = PortalSecurityUtils.getUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "请先登录");
        }
        try {
            portalTopicCommentService.deleteComment(commentId, userId);
            return success();
        } catch (RuntimeException e) {
            return AjaxResult.error(e.getMessage() != null ? e.getMessage() : "删除失败");
        }
    }

    @Operation(summary = "评论点赞/取消", description = "幂等 toggle，返回 isLiked 和 likeCount")
    @PostMapping("/comment/{commentId}/like")
    public AjaxResult toggleCommentLike(@Parameter(description = "评论ID") @PathVariable Long commentId) {
        Long userId = PortalSecurityUtils.getUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "请先登录");
        }
        try {
            Map<String, Object> result = portalTopicCommentService.toggleCommentLike(commentId, userId);
            return success(result);
        } catch (RuntimeException e) {
            return AjaxResult.error(e.getMessage() != null ? e.getMessage() : "操作失败");
        }
    }
}
