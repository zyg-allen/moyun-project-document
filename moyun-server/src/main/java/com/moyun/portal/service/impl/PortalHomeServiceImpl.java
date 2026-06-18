package com.moyun.portal.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.moyun.core.config.redis.RedisCache;
import com.moyun.portal.domain.entity.PortalArticle;
import com.moyun.portal.domain.entity.PortalCategory;
import com.moyun.portal.domain.entity.PortalFriendLink;
import com.moyun.portal.domain.entity.PortalTag;
import com.moyun.portal.domain.entity.PortalUser;
import com.moyun.portal.domain.query.ArticleQuery;
import com.moyun.portal.domain.query.CategoryQuery;
import com.moyun.portal.domain.query.FriendLinkQuery;
import com.moyun.portal.domain.query.TagQuery;
import com.moyun.portal.domain.query.UserQuery;
import com.moyun.portal.domain.vo.ArticleVO;
import com.moyun.portal.domain.vo.HomeModuleVO;
import com.moyun.portal.domain.vo.HomePageVO;
import com.moyun.portal.domain.vo.UserStatsVO;
import com.moyun.portal.mapper.PortalArticleMapper;
import com.moyun.portal.service.IPortalCategoryService;
import com.moyun.portal.service.IPortalFriendLinkService;
import com.moyun.portal.service.IPortalGrowthService;
import com.moyun.portal.service.IPortalHomeService;
import com.moyun.portal.service.IPortalTagService;
import com.moyun.portal.service.IPortalUserService;
import com.moyun.portal.util.ArticleConvertUtil;

/**
 * 首页数据服务实现类
 * 方案A：核心文章聚合 + 非核心模块独立缓存
 *
 * @author moyun
 */
@Slf4j
@Service
public class PortalHomeServiceImpl implements IPortalHomeService {

    // ==================== 缓存Key定义 ====================
    /** 核心文章数据（轮播+精选+热门+最新），10分钟缓存 */
    private static final String CACHE_ARTICLES = "portal:home:articles";
    private static final int TTL_ARTICLES = 10;

    /** 分类列表，30分钟缓存（变动频率低） */
    private static final String CACHE_CATEGORIES = "portal:home:categories";
    private static final int TTL_CATEGORIES = 30;

    /** 热门标签，30分钟缓存 */
    private static final String CACHE_TAGS = "portal:home:tags";
    private static final int TTL_TAGS = 30;

    /** 友情链接，60分钟缓存（变动极少） */
    private static final String CACHE_FRIEND_LINKS = "portal:home:friendLinks";
    private static final int TTL_FRIEND_LINKS = 60;

    /** 名家列表，20分钟缓存 */
    private static final String CACHE_AUTHORS = "portal:home:authors";
    private static final int TTL_AUTHORS = 20;

    /** 分类推荐文章，15分钟缓存 */
    private static final String CACHE_CATEGORY_ARTICLES = "portal:home:categoryArticles";
    private static final int TTL_CATEGORY_ARTICLES = 15;

    /** 所有首页缓存Key集合，用于批量清除 */
    private static final List<String> ALL_CACHE_KEYS = Arrays.asList(
            CACHE_ARTICLES, CACHE_CATEGORIES, CACHE_TAGS,
            CACHE_FRIEND_LINKS, CACHE_AUTHORS, CACHE_CATEGORY_ARTICLES
    );

    @Autowired
    private RedisCache redisCache;

    @Autowired
    private PortalArticleMapper portalArticleMapper;

    @Autowired
    private IPortalCategoryService portalCategoryService;

    @Autowired
    private IPortalTagService portalTagService;

    @Autowired
    private IPortalFriendLinkService portalFriendLinkService;

    @Autowired
    private IPortalUserService portalUserService;

    @Autowired
    private IPortalGrowthService portalGrowthService;

    // ==================== 核心模块（聚合接口） ====================

    @Override
    public HomePageVO getHomePageArticles() {
        // 先从缓存获取
        HomePageVO cached = redisCache.getCacheObject(CACHE_ARTICLES);
        if (cached != null) {
            log.debug("命中缓存: {}", CACHE_ARTICLES);
            return cached;
        }

        // 缓存未命中，从数据库加载
        log.debug("缓存未命中，从数据库加载首页文章数据");
        HomePageVO vo = new HomePageVO();

        List<PortalArticle> carousel = portalArticleMapper.selectCarouselArticles();
        vo.setCarouselArticles(ArticleConvertUtil.toArticleVOList(carousel));

        Page<PortalArticle> featuredPage = new Page<>(1, 8);
        vo.setFeaturedArticles(ArticleConvertUtil.toArticleVOList(
                portalArticleMapper.selectFeaturedArticles(featuredPage)));

        Page<PortalArticle> hotPage = new Page<>(1, 10);
        vo.setHotArticles(ArticleConvertUtil.toArticleVOList(
                portalArticleMapper.selectHotArticles(hotPage)));

        Page<PortalArticle> latestPage = new Page<>(1, 20);
        vo.setLatestArticles(ArticleConvertUtil.toArticleVOList(
                portalArticleMapper.selectLatestArticles(latestPage)));

        // 存入缓存
        redisCache.setCacheObject(CACHE_ARTICLES, vo, TTL_ARTICLES, TimeUnit.MINUTES);
        return vo;
    }

    // ==================== 非核心模块（独立接口 + 独立缓存） ====================

    @Override
    public List<HomeModuleVO.CategorySimpleVO> getHomeCategories() {
        List<HomeModuleVO.CategorySimpleVO> cached = redisCache.getCacheObject(CACHE_CATEGORIES);
        if (cached != null) {
            log.debug("命中缓存: {}", CACHE_CATEGORIES);
            return cached;
        }

        CategoryQuery query = new CategoryQuery();
        query.setStatus("0");
        List<PortalCategory> list = portalCategoryService.selectPortalCategoryList(query);

        List<HomeModuleVO.CategorySimpleVO> result = list.stream()
                .map(c -> {
                    HomeModuleVO.CategorySimpleVO vo = new HomeModuleVO.CategorySimpleVO();
                    vo.setId(c.getId());
                    vo.setName(c.getName());
                    vo.setSlug(c.getSlug());
                    vo.setIcon(c.getIcon());
                    return vo;
                })
                .collect(Collectors.toList());

        redisCache.setCacheObject(CACHE_CATEGORIES, result, TTL_CATEGORIES, TimeUnit.MINUTES);
        return result;
    }

    @Override
    public List<HomeModuleVO.TagSimpleVO> getHomeTags() {
        List<HomeModuleVO.TagSimpleVO> cached = redisCache.getCacheObject(CACHE_TAGS);
        if (cached != null) {
            log.debug("命中缓存: {}", CACHE_TAGS);
            return cached;
        }

        TagQuery query = new TagQuery();
        query.setStatus("0");
        List<PortalTag> list = portalTagService.selectPortalTagList(query);

        List<HomeModuleVO.TagSimpleVO> result = list.stream()
                .limit(20)
                .map(t -> {
                    HomeModuleVO.TagSimpleVO vo = new HomeModuleVO.TagSimpleVO();
                    vo.setId(t.getId());
                    vo.setName(t.getName());
                    vo.setSlug(t.getSlug());
                    return vo;
                })
                .collect(Collectors.toList());

        redisCache.setCacheObject(CACHE_TAGS, result, TTL_TAGS, TimeUnit.MINUTES);
        return result;
    }

    @Override
    public List<HomeModuleVO.FriendLinkSimpleVO> getHomeFriendLinks() {
        List<HomeModuleVO.FriendLinkSimpleVO> cached = redisCache.getCacheObject(CACHE_FRIEND_LINKS);
        if (cached != null) {
            log.debug("命中缓存: {}", CACHE_FRIEND_LINKS);
            return cached;
        }

        FriendLinkQuery query = new FriendLinkQuery();
        query.setStatus("0");
        List<PortalFriendLink> list = portalFriendLinkService.selectPortalFriendLinkList(query);

        List<HomeModuleVO.FriendLinkSimpleVO> result = list.stream()
                .map(link -> {
                    HomeModuleVO.FriendLinkSimpleVO vo = new HomeModuleVO.FriendLinkSimpleVO();
                    vo.setId(link.getId());
                    vo.setName(link.getName());
                    vo.setUrl(link.getUrl());
                    vo.setLogo(link.getLogo());
                    return vo;
                })
                .collect(Collectors.toList());

        redisCache.setCacheObject(CACHE_FRIEND_LINKS, result, TTL_FRIEND_LINKS, TimeUnit.MINUTES);
        return result;
    }

    @Override
    public List<HomeModuleVO.AuthorSimpleVO> getHomeAuthors() {
        List<HomeModuleVO.AuthorSimpleVO> cached = redisCache.getCacheObject(CACHE_AUTHORS);
        if (cached != null) {
            log.debug("命中缓存: {}", CACHE_AUTHORS);
            return cached;
        }

        UserQuery query = new UserQuery();
        query.setStatus("0");
        List<PortalUser> list = portalUserService.selectPortalUserList(query);

        List<HomeModuleVO.AuthorSimpleVO> result = list.stream()
                .limit(10)
                .map(u -> {
                    HomeModuleVO.AuthorSimpleVO vo = new HomeModuleVO.AuthorSimpleVO();
                    vo.setId(u.getId());
                    vo.setUsername(u.getUsername());
                    vo.setNickname(u.getNickname());
                    vo.setAvatar(u.getAvatar());
                    vo.setBio(u.getBio());
                    // 从成长体系统计聚合表读取真实数据
                    UserStatsVO stats = portalGrowthService.getUserStats(u.getId());
                    vo.setArticleCount(stats.getArticles() != null ? stats.getArticles() : 0);
                    vo.setFollowerCount(stats.getFollowers() != null ? stats.getFollowers() : 0);
                    return vo;
                })
                .collect(Collectors.toList());

        redisCache.setCacheObject(CACHE_AUTHORS, result, TTL_AUTHORS, TimeUnit.MINUTES);
        return result;
    }

    @Override
    public List<HomeModuleVO.CategoryArticlesVO> getHomeCategoryArticles() {
        List<HomeModuleVO.CategoryArticlesVO> cached = redisCache.getCacheObject(CACHE_CATEGORY_ARTICLES);
        if (cached != null) {
            log.debug("命中缓存: {}", CACHE_CATEGORY_ARTICLES);
            return cached;
        }

        List<HomeModuleVO.CategoryArticlesVO> result = new ArrayList<>();

        CategoryQuery catQuery = new CategoryQuery();
        catQuery.setStatus("0");
        List<PortalCategory> categories = portalCategoryService.selectPortalCategoryList(catQuery);

        for (PortalCategory category : categories) {
            HomeModuleVO.CategoryArticlesVO vo = new HomeModuleVO.CategoryArticlesVO();
            vo.setCategoryId(category.getId());
            vo.setCategoryName(category.getName());

            ArticleQuery artQuery = new ArticleQuery();
            artQuery.setCategoryId(category.getId());
            artQuery.setIsCategoryRecommended(true);
            Page<PortalArticle> page = new Page<>(1, 8);
            List<PortalArticle> articles = portalArticleMapper.selectPortalArticlePage(page, artQuery).getRecords();
            vo.setArticles(ArticleConvertUtil.toArticleVOList(articles));

            result.add(vo);
            if (result.size() >= 5) {
                break;
            }
        }

        redisCache.setCacheObject(CACHE_CATEGORY_ARTICLES, result, TTL_CATEGORY_ARTICLES, TimeUnit.MINUTES);
        return result;
    }

    // ==================== 缓存管理 ====================

    @Override
    public void refreshAllHomeCache() {
        log.info("刷新首页所有缓存");
        clearAllHomeCache();
        // 重新加载所有缓存
        getHomePageArticles();
        getHomeCategories();
        getHomeTags();
        getHomeFriendLinks();
        getHomeAuthors();
        getHomeCategoryArticles();
        log.info("首页所有缓存刷新完成");
    }

    @Override
    public void clearAllHomeCache() {
        log.info("清除首页所有缓存");
        redisCache.deleteObject(ALL_CACHE_KEYS);
    }
}
