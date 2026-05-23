package com.moyun.community.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.moyun.community.content.entity.*;
import com.moyun.community.content.mapper.*;
import com.moyun.community.content.model.dto.ArticleDetailDto;
import com.moyun.community.content.model.dto.ArticlePublishDto;
import com.moyun.community.content.model.vo.ArticleListVo;
import com.moyun.community.content.model.vo.HomeVo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ArticleServiceImpl extends ServiceImpl<ArticleMapper, Article> implements com.moyun.community.content.service.ArticleService {

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private BannerMapper bannerMapper;

    @Autowired
    private AuditRecordMapper auditRecordMapper;

    @Autowired
    private TagMapper tagMapper;

    @Autowired
    private LikeMapper likeMapper;

    @Autowired
    private CollectMapper collectMapper;

    @Autowired
    private UserProfileMapper userProfileMapper;

    @Override
    public HomeVo getHomeData() {
        HomeVo vo = new HomeVo();

        List<Banner> banners = bannerMapper.selectActiveBanners("home", LocalDateTime.now());
        vo.setBanners(banners.stream().map(b -> {
            HomeVo.BannerVo bannerVo = new HomeVo.BannerVo();
            bannerVo.setBannerId(b.getBannerId());
            bannerVo.setTitle(b.getTitle());
            bannerVo.setImageUrl(b.getImageUrl());
            bannerVo.setJumpType(b.getJumpType());
            bannerVo.setJumpValue(b.getJumpValue());
            return bannerVo;
        }).collect(Collectors.toList()));

        List<Category> categories = categoryMapper.selectShowInHome();
        vo.setCategories(categories.stream().map(c -> {
            HomeVo.CategoryVo categoryVo = new HomeVo.CategoryVo();
            categoryVo.setCategoryId(c.getCategoryId());
            categoryVo.setCategoryName(c.getCategoryName());
            categoryVo.setCategoryCode(c.getCategoryCode());
            categoryVo.setCategoryType(c.getCategoryType());
            categoryVo.setIcon(c.getIcon());
            categoryVo.setCoverImage(c.getCoverImage());
            return categoryVo;
        }).collect(Collectors.toList()));

        vo.setRecommendArticles(getRecommendArticles(10));
        vo.setHotArticles(getHotArticles(10));
        vo.setLatestArticles(getLatestArticles(10));

        return vo;
    }

    @Override
    public List<ArticleListVo> getLatestArticles(int limit) {
        List<Article> articles = articleMapper.selectLatestPublished(limit);
        return convertToListVo(articles);
    }

    @Override
    public List<ArticleListVo> getHotArticles(int limit) {
        List<Article> articles = articleMapper.selectHotPublished(limit);
        return convertToListVo(articles);
    }

    @Override
    public List<ArticleListVo> getRecommendArticles(int limit) {
        List<Article> articles = articleMapper.selectRecommendPublished(limit);
        return convertToListVo(articles);
    }

    @Override
    public List<ArticleListVo> getArticlesByCategory(Long categoryId, int limit) {
        List<Article> articles = articleMapper.selectByCategoryId(categoryId, limit);
        return convertToListVo(articles);
    }

    @Override
    public List<ArticleListVo> getArticlesByType(String articleType, int limit) {
        List<Article> articles = articleMapper.selectByArticleType(articleType, limit);
        return convertToListVo(articles);
    }

    @Override
    public ArticleDetailDto getArticleDetail(Long articleId, Long userId) {
        Article article = articleMapper.selectById(articleId);
        if (article == null || article.getIsDeleted() == 1) {
            return null;
        }

        ArticleDetailDto dto = new ArticleDetailDto();
        BeanUtils.copyProperties(article, dto);

        Category category = categoryMapper.selectById(article.getCategoryId());
        if (category != null) {
            ArticleDetailDto.CategoryInfo categoryInfo = new ArticleDetailDto.CategoryInfo();
            categoryInfo.setCategoryId(category.getCategoryId());
            categoryInfo.setCategoryName(category.getCategoryName());
            categoryInfo.setCategoryCode(category.getCategoryCode());
            categoryInfo.setCategoryType(category.getCategoryType());
            dto.setCategory(categoryInfo);
        }

        UserProfile authorProfile = userProfileMapper.selectById(article.getAuthorId());
        if (authorProfile != null) {
            ArticleDetailDto.AuthorInfo authorInfo = new ArticleDetailDto.AuthorInfo();
            authorInfo.setUserId(article.getAuthorId());
            authorInfo.setNickname(authorProfile.getNickname());
            authorInfo.setAvatar(authorProfile.getAvatar());
            authorInfo.setArticleCount(authorProfile.getArticleCount());
            authorInfo.setFollowerCount(authorProfile.getFollowerCount());
            authorInfo.setIsAuthor(authorProfile.getIsAuthor());
            dto.setAuthor(authorInfo);
        }

        List<Tag> tags = tagMapper.selectTagsByArticleId(articleId);
        dto.setTags(tags.stream().map(t -> {
            ArticleDetailDto.TagInfo tagInfo = new ArticleDetailDto.TagInfo();
            tagInfo.setTagId(t.getTagId());
            tagInfo.setTagName(t.getTagName());
            tagInfo.setTagColor(t.getTagColor());
            return tagInfo;
        }).collect(Collectors.toList()));

        if (userId != null) {
            dto.setIsLiked(likeMapper.existsLike(userId, "article", articleId));
            dto.setIsCollected(collectMapper.existsCollect(userId, articleId));
        } else {
            dto.setIsLiked(false);
            dto.setIsCollected(false);
        }

        return dto;
    }

    @Override
    @Transactional
    public Article saveArticle(ArticlePublishDto dto, Long userId) {
        Article article = new Article();
        BeanUtils.copyProperties(dto, article);
        article.setAuthorId(userId);
        article.setStatus("draft");
        article.setIsDeleted(0);
        article.setViewCount(0L);
        article.setLikeCount(0L);
        article.setCollectCount(0L);
        article.setCommentCount(0);
        article.setShareCount(0);
        article.setRewardCount(0);
        article.setAllowComment(dto.getAllowComment() != null ? dto.getAllowComment() : 1);

        if (dto.getWordCount() == null) {
            article.setWordCount(calculateWordCount(dto.getContent()));
        }
        if (dto.getReadingTime() == null && article.getWordCount() != null) {
            article.setReadingTime(article.getWordCount() / 300 + 1);
        }

        articleMapper.insert(article);

        if (dto.getTagIds() != null && !dto.getTagIds().isEmpty()) {
            tagMapper.insertArticleTags(article.getArticleId(), dto.getTagIds());
        }

        userProfileMapper.incrementArticleCount(userId);

        return article;
    }

    @Override
    @Transactional
    public Article updateArticle(ArticlePublishDto dto, Long userId) {
        Article article = articleMapper.selectById(dto.getArticleId());
        if (article == null) {
            throw new RuntimeException("文章不存在");
        }
        if (!article.getAuthorId().equals(userId)) {
            throw new RuntimeException("无权修改此文章");
        }
        if ("published".equals(article.getStatus()) || "pending".equals(article.getStatus())) {
            throw new RuntimeException("已发布或审核中的文章不能直接修改");
        }

        BeanUtils.copyProperties(dto, article, "articleId", "authorId", "createTime");

        if (dto.getWordCount() == null) {
            article.setWordCount(calculateWordCount(dto.getContent()));
        }

        articleMapper.updateById(article);

        if (dto.getTagIds() != null) {
            tagMapper.deleteArticleTags(article.getArticleId());
            tagMapper.insertArticleTags(article.getArticleId(), dto.getTagIds());
        }

        return article;
    }

    @Override
    @Transactional
    public Article submitForReview(Long articleId, Long userId) {
        Article article = articleMapper.selectById(articleId);
        if (article == null) {
            throw new RuntimeException("文章不存在");
        }
        if (!article.getAuthorId().equals(userId)) {
            throw new RuntimeException("无权提交此文章");
        }
        if (!"draft".equals(article.getStatus()) && !"rejected".equals(article.getStatus())) {
            throw new RuntimeException("只有草稿或驳回状态的文章可以提交审核");
        }

        article.setStatus("pending");
        article.setPublishTime(LocalDateTime.now());
        articleMapper.updateById(article);

        AuditRecord record = new AuditRecord();
        record.setArticleId(articleId);
        record.setSubmitUserId(userId);
        record.setAuditStatus("pending");
        record.setAuditType("first");
        record.setSubmitTime(LocalDateTime.now());
        auditRecordMapper.insert(record);

        return article;
    }

    @Override
    public List<ArticleListVo> getMyArticles(Long userId) {
        List<Article> articles = articleMapper.selectByAuthorId(userId);
        return convertToListVo(articles);
    }

    @Override
    public List<ArticleListVo> getPendingArticles() {
        List<Article> articles = articleMapper.selectByStatus("pending");
        return convertToListVo(articles);
    }

    @Override
    @Transactional
    public boolean approveArticle(Long articleId, Long auditorId, String reason) {
        Article article = articleMapper.selectById(articleId);
        if (article == null) {
            return false;
        }

        article.setStatus("published");
        article.setPublishTime(LocalDateTime.now());
        article.setAuditTime(LocalDateTime.now());
        articleMapper.updateById(article);

        AuditRecord record = auditRecordMapper.selectLatestByArticleId(articleId);
        if (record != null) {
            record.setAuditStatus("approved");
            record.setAuditorId(auditorId);
            record.setAuditReason(reason);
            record.setAuditTime(LocalDateTime.now());
            auditRecordMapper.updateById(record);
        }

        return true;
    }

    @Override
    @Transactional
    public boolean rejectArticle(Long articleId, Long auditorId, String reason) {
        Article article = articleMapper.selectById(articleId);
        if (article == null) {
            return false;
        }

        article.setStatus("rejected");
        article.setAuditTime(LocalDateTime.now());
        article.setAuditReason(reason);
        articleMapper.updateById(article);

        AuditRecord record = auditRecordMapper.selectLatestByArticleId(articleId);
        if (record != null) {
            record.setAuditStatus("rejected");
            record.setAuditorId(auditorId);
            record.setAuditReason(reason);
            record.setAuditTime(LocalDateTime.now());
            auditRecordMapper.updateById(record);
        }

        return true;
    }

    @Override
    @Transactional
    public boolean deleteArticle(Long articleId, Long userId) {
        Article article = articleMapper.selectById(articleId);
        if (article == null) {
            return false;
        }
        if (!article.getAuthorId().equals(userId)) {
            throw new RuntimeException("无权删除此文章");
        }

        article.setIsDeleted(1);
        article.setDeleteTime(LocalDateTime.now());
        articleMapper.updateById(article);

        userProfileMapper.decrementArticleCount(userId);

        return true;
    }

    @Override
    public void incrementViewCount(Long articleId) {
        articleMapper.incrementViewCount(articleId);
    }

    @Override
    @Transactional
    public boolean toggleLike(Long articleId, Long userId) {
        boolean exists = likeMapper.existsLike(userId, "article", articleId);
        if (exists) {
            likeMapper.cancelLike(userId, "article", articleId);
            articleMapper.decrementLikeCount(articleId);
        } else {
            likeMapper.addLike(userId, "article", articleId);
            articleMapper.incrementLikeCount(articleId);
        }
        return !exists;
    }

    @Override
    @Transactional
    public boolean toggleCollect(Long articleId, Long userId) {
        boolean exists = collectMapper.existsCollect(userId, articleId);
        if (exists) {
            collectMapper.cancelCollect(userId, articleId);
            articleMapper.decrementCollectCount(articleId);
        } else {
            collectMapper.addCollect(userId, articleId);
            articleMapper.incrementCollectCount(articleId);
        }
        return !exists;
    }

    private List<ArticleListVo> convertToListVo(List<Article> articles) {
        if (articles == null || articles.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> categoryIds = articles.stream().map(Article::getCategoryId).collect(Collectors.toSet());
        final Map<Long, Category> categoryMap;
        if (!categoryIds.isEmpty()) {
            List<Category> categories = categoryMapper.selectBatchIds(categoryIds);
            categoryMap = categories.stream().collect(Collectors.toMap(Category::getCategoryId, c -> c));
        } else {
            categoryMap = new HashMap<>();
        }

        Set<Long> authorIds = articles.stream().map(Article::getAuthorId).collect(Collectors.toSet());
        final Map<Long, UserProfile> authorMap;
        if (!authorIds.isEmpty()) {
            List<UserProfile> profiles = userProfileMapper.selectBatchIds(authorIds);
            authorMap = profiles.stream().collect(Collectors.toMap(UserProfile::getUserId, p -> p));
        } else {
            authorMap = new HashMap<>();
        }

        return articles.stream().map(article -> {
            ArticleListVo vo = new ArticleListVo();
            BeanUtils.copyProperties(article, vo);

            Category category = categoryMap.get(article.getCategoryId());
            if (category != null) {
                vo.setCategoryName(category.getCategoryName());
            }

            UserProfile author = authorMap.get(article.getAuthorId());
            if (author != null) {
                vo.setAuthorNickname(author.getNickname());
                vo.setAuthorAvatar(author.getAvatar());
            }

            List<Tag> tags = tagMapper.selectTagsByArticleId(article.getArticleId());
            vo.setTagNames(tags.stream().map(Tag::getTagName).collect(Collectors.toList()));

            return vo;
        }).collect(Collectors.toList());
    }

    private int calculateWordCount(String content) {
        if (content == null || content.isEmpty()) {
            return 0;
        }
        return content.length();
    }
}
