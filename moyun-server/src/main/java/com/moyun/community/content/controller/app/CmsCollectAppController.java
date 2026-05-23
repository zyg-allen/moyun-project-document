package com.moyun.community.content.controller.app;

import com.moyun.common.core.controller.BaseController;
import com.moyun.common.core.domain.AjaxResult;
import com.moyun.common.core.domain.TableDataInfo;
import com.moyun.common.utils.SecurityUtils;
import com.moyun.community.content.entity.CmsArticle;
import com.moyun.community.content.service.ICmsCollectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 前台收藏管理Controller
 *
 * @author moyun
 * @date 2026-04-21
 */
@Tag(name = "前台收藏管理", description = "文章收藏、取消收藏等接口")
@RestController
@RequestMapping("/app/collect")
public class CmsCollectAppController extends BaseController {

    @Autowired
    private ICmsCollectService collectService;

    /**
     * 切换收藏状态
     */
    @Operation(summary = "切换收藏", description = "收藏或取消收藏文章")
    @PostMapping("/toggle")
    public AjaxResult toggle(
            @Parameter(description = "文章ID", required = true, example = "1")
            @RequestParam Long articleId) {
        Long userId = SecurityUtils.getUserId();
        return toAjax(collectService.toggleCollect(userId, articleId));
    }

    /**
     * 获取我的收藏列表
     */
    @Operation(summary = "我的收藏", description = "查询当前用户收藏的文章列表")
    @GetMapping("/my")
    public TableDataInfo myCollects() {
        Long userId = SecurityUtils.getUserId();
        startPage();
        List<CmsArticle> list = collectService.selectCollectedArticles(userId);
        return getDataTable(list);
    }

    /**
     * 检查是否已收藏
     */
    @Operation(summary = "检查收藏状态", description = "检查当前用户是否已收藏指定文章")
    @GetMapping("/check")
    public AjaxResult check(
            @Parameter(description = "文章ID", required = true, example = "1")
            @RequestParam Long articleId) {
        Long userId = SecurityUtils.getUserId();
        boolean collected = collectService.isCollected(userId, articleId);
        return success(collected);
    }
}