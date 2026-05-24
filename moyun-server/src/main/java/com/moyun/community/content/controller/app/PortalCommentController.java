package com.moyun.community.content.controller.app;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.common.utils.SecurityUtils;
import com.moyun.community.common.domain.ApiResponse;
import com.moyun.community.common.domain.PageResult;
import com.moyun.community.content.entity.CmsArticle;
import com.moyun.community.content.entity.CmsComment;
import com.moyun.community.content.entity.UserProfile;
import com.moyun.community.content.mapper.CmsArticleMapper;
import com.moyun.community.content.mapper.CmsCommentMapper;
import com.moyun.community.content.mapper.UserProfileMapper;
import com.moyun.community.content.model.vo.PortalCommentVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 前台评论Controller
 *
 * @author moyun
 */
@Tag(name = "评论模块", description = "评论列表、创建、删除等接口")
@RestController
@RequestMapping("/api/comment")
@RequiredArgsConstructor
public class PortalCommentController {

    private final CmsCommentMapper commentMapper;
    private final CmsArticleMapper articleMapper;
    private final UserProfileMapper userProfileMapper;

    /**
     * 获取评论列表
     */
    @Operation(summary = "获取评论列表", description = "分页获取文章评论列表")
    @GetMapping("/list")
    public ApiResponse<PageResult<PortalCommentVo>> getCommentList(
            @Parameter(description = "文章ID", required = true)
            @RequestParam String articleId,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") Integer pageSize) {
        
        try {
            Long id = Long.parseLong(articleId);
            Page<CmsComment> pageObj = new Page<>(page, pageSize);
            LambdaQueryWrapper<CmsComment> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(CmsComment::getArticleId, id)
                    .isNull(CmsComment::getParentId)
                    .orderByDesc(CmsComment::getCreateTime);
            
            Page<CmsComment> resultPage = commentMapper.selectPage(pageObj, wrapper);
            List<PortalCommentVo> voList = resultPage.getRecords().stream()
                    .map(this::convertToPortalCommentVo)
                    .collect(Collectors.toList());
            
            PageResult<PortalCommentVo> pageResult = PageResult.of(
                    voList,
                    resultPage.getTotal(),
                    page,
                    pageSize
            );
            
            return ApiResponse.success(pageResult);
        } catch (NumberFormatException e) {
            return ApiResponse.error("无效的文章ID");
        }
    }

    /**
     * 创建评论
     */
    @Operation(summary = "创建评论", description = "创建新评论")
    @PostMapping
    public ApiResponse<PortalCommentVo> createComment(
            @Parameter(description = "评论信息", required = true)
            @RequestBody PortalCommentVo commentVo) {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            return ApiResponse.error(401, "未登录");
        }
        
        if (commentVo.getArticleId() == null) {
            return ApiResponse.error("文章ID不能为空");
        }
        
        try {
            Long articleId = Long.parseLong(commentVo.getArticleId());
            CmsArticle article = articleMapper.selectById(articleId);
            if (article == null) {
                return ApiResponse.error(404, "文章不存在");
            }
            
            CmsComment comment = new CmsComment();
            comment.setArticleId(articleId);
            comment.setContent(commentVo.getContent());
            comment.setUserId(userId);
            comment.setLikeCount(0);
            
            if (commentVo.getParentId() != null && !commentVo.getParentId().isEmpty()) {
                try {
                    comment.setParentId(Long.parseLong(commentVo.getParentId()));
                } catch (NumberFormatException ignored) {
                }
            }
            
            comment.setCreateTime(java.time.LocalDateTime.now());
            commentMapper.insert(comment);
            
            article.setCommentCount(article.getCommentCount() != null ? article.getCommentCount() + 1 : 1);
            articleMapper.updateById(article);
            
            PortalCommentVo resultVo = convertToPortalCommentVo(comment);
            return ApiResponse.success(resultVo);
        } catch (NumberFormatException e) {
            return ApiResponse.error("无效的文章ID");
        }
    }

    /**
     * 删除评论
     */
    @Operation(summary = "删除评论", description = "删除评论")
    @DeleteMapping("/{commentId}")
    public ApiResponse<Void> deleteComment(
            @Parameter(description = "评论ID", required = true)
            @PathVariable String commentId) {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            return ApiResponse.error(401, "未登录");
        }
        
        try {
            Long id = Long.parseLong(commentId);
            CmsComment comment = commentMapper.selectById(id);
            if (comment == null) {
                return ApiResponse.error(404, "评论不存在");
            }
            
            if (!comment.getUserId().equals(userId)) {
                return ApiResponse.error(403, "无权删除此评论");
            }
            
            commentMapper.deleteById(id);
            return ApiResponse.success();
        } catch (NumberFormatException e) {
            return ApiResponse.error("无效的评论ID");
        }
    }

    /**
     * 评论点赞
     */
    @Operation(summary = "评论点赞", description = "对评论进行点赞")
    @PostMapping("/{commentId}/like")
    public ApiResponse<Void> likeComment(
            @Parameter(description = "评论ID", required = true)
            @PathVariable String commentId) {
        try {
            Long id = Long.parseLong(commentId);
            CmsComment comment = commentMapper.selectById(id);
            if (comment == null) {
                return ApiResponse.error(404, "评论不存在");
            }
            
            comment.setLikeCount(comment.getLikeCount() != null ? comment.getLikeCount() + 1 : 1);
            commentMapper.updateById(comment);
            return ApiResponse.success();
        } catch (NumberFormatException e) {
            return ApiResponse.error("无效的评论ID");
        }
    }

    /**
     * 取消评论点赞
     */
    @Operation(summary = "取消评论点赞", description = "取消评论点赞")
    @DeleteMapping("/{commentId}/like")
    public ApiResponse<Void> unlikeComment(
            @Parameter(description = "评论ID", required = true)
            @PathVariable String commentId) {
        try {
            Long id = Long.parseLong(commentId);
            CmsComment comment = commentMapper.selectById(id);
            if (comment == null) {
                return ApiResponse.error(404, "评论不存在");
            }
            
            if (comment.getLikeCount() != null && comment.getLikeCount() > 0) {
                comment.setLikeCount(comment.getLikeCount() - 1);
                commentMapper.updateById(comment);
            }
            return ApiResponse.success();
        } catch (NumberFormatException e) {
            return ApiResponse.error("无效的评论ID");
        }
    }

    /**
     * 转换为前台评论VO
     */
    private PortalCommentVo convertToPortalCommentVo(CmsComment comment) {
        PortalCommentVo vo = new PortalCommentVo();
        vo.setId(String.valueOf(comment.getCommentId()));
        vo.setArticleId(String.valueOf(comment.getArticleId()));
        if (comment.getParentId() != null) {
            vo.setParentId(String.valueOf(comment.getParentId()));
        }
        vo.setContent(comment.getContent());
        vo.setUserId(String.valueOf(comment.getUserId()));
        vo.setLikes(comment.getLikeCount() != null ? comment.getLikeCount() : 0);
        vo.setIsLiked(false);
        
        if (comment.getCreateTime() != null) {
            vo.setCreatedAt(comment.getCreateTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }
        
        UserProfile profile = userProfileMapper.selectByUserId(comment.getUserId());
        if (profile != null) {
            vo.setUserNickname(profile.getNickname());
            vo.setUserAvatar(profile.getAvatar());
        }
        
        vo.setReplies(new ArrayList<>());
        return vo;
    }
}
