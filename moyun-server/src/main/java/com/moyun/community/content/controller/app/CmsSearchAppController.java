package com.moyun.community.content.controller.app;

import com.moyun.common.core.controller.BaseController;
import com.moyun.common.core.domain.AjaxResult;
import com.moyun.common.core.domain.TableDataInfo;
import com.moyun.common.utils.SecurityUtils;
import com.moyun.community.content.entity.CmsArticle;
import com.moyun.community.content.entity.CmsSearchHistory;
import com.moyun.community.content.entity.CmsSearchHotword;
import com.moyun.community.content.service.ICmsSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/app/search")
public class CmsSearchAppController extends BaseController {

    @Autowired
    private ICmsSearchService searchService;

    @GetMapping("/hotwords")
    public AjaxResult hotwords() {
        List<CmsSearchHotword> list = searchService.selectHotwordList();
        return success(list);
    }

    @GetMapping("/articles")
    public TableDataInfo searchArticles(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId) {
        startPage();
        List<CmsArticle> list = searchService.searchArticles(keyword, categoryId);
        return getDataTable(list);
    }

    @GetMapping("/history")
    public TableDataInfo history() {
        Long userId = SecurityUtils.getUserId();
        startPage();
        List<CmsSearchHistory> list = searchService.selectSearchHistoryList(userId);
        return getDataTable(list);
    }

    @PostMapping("/history")
    public AjaxResult addHistory(@RequestBody CmsSearchHistory history) {
        history.setUserId(SecurityUtils.getUserId());
        return toAjax(searchService.addSearchHistory(history));
    }

    @DeleteMapping("/history")
    public AjaxResult deleteHistory() {
        Long userId = SecurityUtils.getUserId();
        return toAjax(searchService.deleteUserSearchHistory(userId));
    }
}