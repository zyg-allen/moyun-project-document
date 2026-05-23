package com.moyun.community.content.controller.app;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.moyun.common.core.controller.BaseController;
import com.moyun.common.core.domain.AjaxResult;
import com.moyun.common.core.domain.TableDataInfo;
import com.moyun.community.content.entity.CmsArticle;
import com.moyun.community.content.entity.CmsBanner;
import com.moyun.community.content.entity.CmsCategory;
import com.moyun.community.content.mapper.CmsBannerMapper;
import com.moyun.community.content.mapper.CmsCategoryMapper;
import com.moyun.community.content.service.ICmsArticleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/app/home")
public class HomeAppController extends BaseController {

    @Autowired
    private ICmsArticleService articleService;

    @Autowired
    private CmsBannerMapper bannerMapper;

    @Autowired
    private CmsCategoryMapper categoryMapper;

    @GetMapping("/index")
    public AjaxResult index() {
        Map<String, Object> data = new HashMap<>();

        List<CmsBanner> banners = bannerMapper.selectBannerList();
        List<CmsArticle> recommendList = articleService.selectHomeRecommendList();
        List<CmsArticle> hotList = articleService.selectHotArticleList();
        List<CmsArticle> latestList = articleService.selectLatestArticleList();

        data.put("banners", banners);
        data.put("recommendList", recommendList);
        data.put("hotList", hotList);
        data.put("latestList", latestList);

        return success(data);
    }

    @GetMapping("/recommend")
    public TableDataInfo recommend() {
        startPage();
        List<CmsArticle> list = articleService.selectHomeRecommendList();
        return getDataTable(list);
    }

    @GetMapping("/hot")
    public TableDataInfo hot() {
        startPage();
        List<CmsArticle> list = articleService.selectHotArticleList();
        return getDataTable(list);
    }

    @GetMapping("/latest")
    public TableDataInfo latest() {
        startPage();
        List<CmsArticle> list = articleService.selectLatestArticleList();
        return getDataTable(list);
    }

    @GetMapping("/banners")
    public AjaxResult banners() {
        List<CmsBanner> list = bannerMapper.selectBannerList();
        return success(list);
    }

    @GetMapping("/categories")
    public AjaxResult categories() {
        LambdaQueryWrapper<CmsCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CmsCategory::getStatus, "0")
               .eq(CmsCategory::getIsShow, 1)
               .orderByAsc(CmsCategory::getSort);
        List<CmsCategory> list = categoryMapper.selectList(wrapper);
        return success(list);
    }

    @GetMapping("/category/{categoryId}")
    public TableDataInfo categoryArticles(@PathVariable Long categoryId) {
        startPage();
        List<CmsArticle> list = articleService.selectArticleListByCategory(categoryId);
        return getDataTable(list);
    }
}