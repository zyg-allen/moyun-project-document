package com.moyun.community.content.controller.app;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.community.common.domain.ApiResponse;
import com.moyun.community.common.domain.PageResult;
import com.moyun.community.content.entity.CmsArticle;
import com.moyun.community.content.mapper.CmsArticleMapper;
import com.moyun.community.content.model.vo.PortalArticleVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 前台搜索 Controller
 *
 * @author moyun
 */
@Tag(name = "搜索模块", description = "文章搜索接口")
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class PortalSearchController {

    private final CmsArticleMapper articleMapper;

    /**
     * 搜索文章
     */
    @Operation(summary = "搜索文章", description = "根据关键词搜索文章")
    @GetMapping
    public ApiResponse<PageResult<PortalArticleVo>> search(
            @RequestParam(required = true) String keyword,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {

        Page<CmsArticle> pageObj = new Page<>(page, pageSize);
        LambdaQueryWrapper<CmsArticle> wrapper = new LambdaQueryWrapper<>();
        
        wrapper.eq(CmsArticle::getStatus, "published");
        
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w
                    .like(CmsArticle::getTitle, keyword)
                    .or()
                    .like(CmsArticle::getSummary, keyword)
                    .or()
                    .like(CmsArticle::getContent, keyword));
        }
        
        wrapper.orderByDesc(CmsArticle::getCreateTime);

        Page<CmsArticle> resultPage = articleMapper.selectPage(pageObj, wrapper);

        List<PortalArticleVo> voList = new ArrayList<>();
        for (CmsArticle article : resultPage.getRecords()) {
            PortalArticleVo vo = new PortalArticleVo();
            vo.setId(String.valueOf(article.getArticleId()));
            vo.setTitle(article.getTitle());
            vo.setExcerpt(article.getSummary());
            vo.setCover(article.getCoverUrl());
            vo.setViews(article.getViewCount() != null ? article.getViewCount().intValue() : 0);
            vo.setLikes(article.getLikeCount() != null ? article.getLikeCount().intValue() : 0);
            vo.setCommentsCount(article.getCommentCount());
            vo.setStatus(article.getStatus());
            
            if (article.getCreateTime() != null) {
                vo.setCreatedAt(article.getCreateTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            }
            
            if (article.getCategoryId() != null) {
                vo.setCategoryId(String.valueOf(article.getCategoryId()));
            }
            
            if (article.getAuthorId() != null) {
                vo.setAuthorId(String.valueOf(article.getAuthorId()));
            }

            voList.add(vo);
        }

        PageResult<PortalArticleVo> pageResult = PageResult.of(
                voList,
                resultPage.getTotal(),
                page,
                pageSize
        );

        return ApiResponse.success(pageResult);
    }
}
