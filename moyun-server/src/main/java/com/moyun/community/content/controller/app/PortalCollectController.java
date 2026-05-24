package com.moyun.community.content.controller.app;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.common.utils.SecurityUtils;
import com.moyun.community.common.domain.ApiResponse;
import com.moyun.community.common.domain.PageResult;
import com.moyun.community.content.entity.CmsArticle;
import com.moyun.community.content.entity.CmsCollect;
import com.moyun.community.content.mapper.CmsArticleMapper;
import com.moyun.community.content.mapper.CmsCollectMapper;
import com.moyun.community.content.model.vo.PortalCollectVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 前台收藏 Controller
 *
 * @author moyun
 */
@Tag(name = "收藏模块", description = "文章收藏管理接口")
@RestController
@RequestMapping("/api/bookmark")
@RequiredArgsConstructor
public class PortalCollectController {

    private final CmsCollectMapper collectMapper;
    private final CmsArticleMapper articleMapper;

    /**
     * 获取收藏列表
     */
    @Operation(summary = "获取收藏列表", description = "分页获取当前用户的收藏列表")
    @GetMapping("/list")
    public ApiResponse<PageResult<PortalCollectVo>> getCollectList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {

        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            return ApiResponse.error(401, "未登录");
        }

        Page<CmsCollect> pageObj = new Page<>(page, pageSize);
        LambdaQueryWrapper<CmsCollect> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CmsCollect::getUserId, userId);
        wrapper.orderByDesc(CmsCollect::getCreateTime);

        Page<CmsCollect> resultPage = collectMapper.selectPage(pageObj, wrapper);

        List<PortalCollectVo> voList = new ArrayList<>();
        for (CmsCollect collect : resultPage.getRecords()) {
            PortalCollectVo vo = new PortalCollectVo();
            vo.setId(collect.getId());
            vo.setUserId(collect.getUserId());
            vo.setArticleId(collect.getArticleId());
            vo.setCreatedAt(collect.getCreateTime() != null 
                    ? collect.getCreateTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) 
                    : null);

            CmsArticle article = articleMapper.selectById(collect.getArticleId());
            if (article != null) {
                vo.setArticleTitle(article.getTitle());
                vo.setArticleCover(article.getCoverUrl());
                vo.setArticleSummary(article.getSummary());
            }

            voList.add(vo);
        }

        PageResult<PortalCollectVo> pageResult = PageResult.of(
                voList,
                resultPage.getTotal(),
                page,
                pageSize
        );

        return ApiResponse.success(pageResult);
    }

    /**
     * 添加收藏
     */
    @Operation(summary = "添加收藏", description = "收藏一篇文章")
    @PostMapping
    public ApiResponse<Void> addCollect(@RequestBody Map<String, String> params) {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            return ApiResponse.error(401, "未登录");
        }

        String articleIdStr = params.get("articleId");
        if (articleIdStr == null || articleIdStr.isEmpty()) {
            return ApiResponse.error("文章ID不能为空");
        }

        Long articleId = Long.parseLong(articleIdStr);

        LambdaQueryWrapper<CmsCollect> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CmsCollect::getUserId, userId);
        wrapper.eq(CmsCollect::getArticleId, articleId);

        if (collectMapper.selectCount(wrapper) > 0) {
            return ApiResponse.error("已收藏该文章");
        }

        CmsCollect collect = new CmsCollect();
        collect.setUserId(userId);
        collect.setArticleId(articleId);
        collect.setCreateTime(java.time.LocalDateTime.now());

        collectMapper.insert(collect);

        return ApiResponse.success();
    }

    /**
     * 取消收藏
     */
    @Operation(summary = "取消收藏", description = "取消收藏一篇文章")
    @DeleteMapping("/{collectId}")
    public ApiResponse<Void> removeCollect(@PathVariable Long collectId) {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            return ApiResponse.error(401, "未登录");
        }

        CmsCollect collect = collectMapper.selectById(collectId);
        if (collect == null) {
            return ApiResponse.error(404, "收藏不存在");
        }

        if (!collect.getUserId().equals(userId)) {
            return ApiResponse.error(403, "无权删除该收藏");
        }

        collectMapper.deleteById(collectId);

        return ApiResponse.success();
    }

    /**
     * 检查是否已收藏
     */
    @Operation(summary = "检查收藏状态", description = "检查当前用户是否已收藏某篇文章")
    @GetMapping("/check")
    public ApiResponse<Boolean> checkCollect(
            @RequestParam Long articleId) {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            return ApiResponse.success(false);
        }

        LambdaQueryWrapper<CmsCollect> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CmsCollect::getUserId, userId);
        wrapper.eq(CmsCollect::getArticleId, articleId);

        boolean isCollected = collectMapper.selectCount(wrapper) > 0;

        return ApiResponse.success(isCollected);
    }
}
