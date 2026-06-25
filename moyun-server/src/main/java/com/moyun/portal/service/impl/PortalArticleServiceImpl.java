package com.moyun.portal.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.moyun.portal.domain.entity.PortalArticle;
import com.moyun.portal.domain.query.ArticleQuery;
import com.moyun.portal.mapper.PortalArticleMapper;
import com.moyun.portal.mapper.PortalUserStatsMapper;
import com.moyun.portal.service.IPortalArticleService;
import com.moyun.portal.service.IPortalCategoryService;
import com.moyun.portal.service.IPortalGrowthService;
import com.moyun.portal.util.PortalSecurityUtils;
import com.moyun.util.file.Base64ImageUtils;

/**
 * 门户文章 业务层处理
 *
 * @author moyun
 */
@Service
public class PortalArticleServiceImpl extends ServiceImpl<PortalArticleMapper, PortalArticle> implements IPortalArticleService {

    @Autowired
    private IPortalCategoryService portalCategoryService;

    @Autowired
    private Base64ImageUtils base64ImageUtils;

    @Autowired
    private IPortalGrowthService portalGrowthService;

    @Autowired
    private PortalUserStatsMapper userStatsMapper;

    /**
     * 根据条件分页查询文章列表
     *
     * @param page 分页参数
     * @param portalArticle 文章信息
     * @return 分页结果
     */
    @Override
    public Page<PortalArticle> selectPortalArticlePage(Page<PortalArticle> page, ArticleQuery portalArticle) {
        return baseMapper.selectPortalArticlePage(page, portalArticle);
    }

    /**
     * 查询"我的文章"分页列表（按 authorId 过滤，不强制 status=published）
     * 用于作者查看自己所有状态的文章（草稿/待审核/已发布/已拒绝）
     *
     * @param page 分页参数
     * @param query 查询条件（authorId 必填，status 可选）
     * @return 分页结果
     */
    @Override
    public Page<PortalArticle> selectMyArticlesPage(Page<PortalArticle> page, ArticleQuery query) {
        if (query.getAuthorId() == null) {
            throw new com.moyun.common.exception.system.ServiceException("查询我的文章必须提供作者ID");
        }
        return baseMapper.selectMyArticlesPage(page, query);
    }

    /**
     * 根据条件查询文章列表（不分页，用于导出等场景）
     *
     * @param portalArticle 文章信息
     * @return 文章信息集合
     */
    @Override
    public List<PortalArticle> selectPortalArticleList(ArticleQuery portalArticle) {
        return baseMapper.selectPortalArticleList(portalArticle);
    }

    /**
     * 通过文章ID查询文章
     *
     * @param id 文章ID
     * @return 文章对象信息
     */
    @Override
    public PortalArticle selectPortalArticleById(Long id) {
        return baseMapper.selectPortalArticleById(id);
    }

    /**
     * 新增文章信息（前台用户发布）
     * 自动设置作者ID为当前登录的门户用户
     *
     * @param portalArticle 文章信息
     * @return 结果
     */
    @Override
    public int insertPortalArticle(PortalArticle portalArticle) {
        // 自动处理Base64图片
        processArticleImages(portalArticle);
        // 自动设置前台作者信息
        fillPortalAuthorAndCategory(portalArticle);
        return baseMapper.insertPortalArticle(portalArticle);
    }

    /**
     * 修改文章信息
     *
     * @param portalArticle 文章信息
     * @return 结果
     */
    @Override
    public int updatePortalArticle(PortalArticle portalArticle) {
        // 自动处理Base64图片
        processArticleImages(portalArticle);
        // 切换分类或新建分类时同步维护 category_path 与 root_category_id
        fillCategoryPath(portalArticle);
        // 维护 slug 唯一性（允许用户自定义时校验）
        fillSlug(portalArticle);
        return baseMapper.updatePortalArticle(portalArticle);
    }
    
    /**
     * 前台发布文章（带审核流程）
     * 发布后状态为 pending（待审核），需后台人工审核通过后才变更为 published
     *
     * @param portalArticle 文章信息
     * @return 结果
     */
    @Override
    public int publishArticle(PortalArticle portalArticle) {
        // 自动处理Base64图片
        processArticleImages(portalArticle);
        // 自动设置前台作者信息
        fillPortalAuthorAndCategory(portalArticle);
        // 自动生成或校验 slug（用于 SEO 语义化路径）
        fillSlug(portalArticle);
        // 设置默认状态为待审核（pending），需后台人工审核通过后才变更为 published
        if (portalArticle.getStatus() == null) {
            portalArticle.setStatus("pending");
        }
        // 设置发布时间（提交审核时间）
        if (portalArticle.getPublishedAt() == null) {
            portalArticle.setPublishedAt(LocalDateTime.now());
        }
        int rows = baseMapper.insertPortalArticle(portalArticle);

        // 记录成长事件 + 更新创作字数统计
        if (rows > 0 && portalArticle.getAuthorId() != null) {
            // 统计创作字数（按内容字符数粗略计算）
            long wordCount = 0;
            if (portalArticle.getContent() != null) {
                wordCount = portalArticle.getContent().length();
            }
            if (wordCount > 0) {
                userStatsMapper.insertIfNotExists(portalArticle.getAuthorId());
                userStatsMapper.addArticleWordSum(portalArticle.getAuthorId(), wordCount);
            }
            // 记录发布文章成长事件
            portalGrowthService.recordEvent("article", "publish_article",
                    portalArticle.getAuthorId(), "article", portalArticle.getId());

            // 如果是精选文章，额外记录精选事件
            if (Boolean.TRUE.equals(portalArticle.getIsFeatured())) {
                portalGrowthService.recordEvent("article", "article_featured",
                        portalArticle.getAuthorId(), "article", portalArticle.getId());
            }
        }

        return rows;
    }

    /**
     * 前台保存草稿（真实入库，返回包含 id 的实体）
     * 新建草稿：status = draft
     * 更新草稿：保持 status = draft（避免已存草稿被误改为其他状态）
     *
     * @param portalArticle 文章信息（id 为空时新建，非空时更新）
     * @return 入库后的文章实体（含 id、createTime、updateTime）
     */
    @Override
    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public PortalArticle saveDraft(PortalArticle portalArticle) {
        // 自动处理Base64图片
        processArticleImages(portalArticle);
        // 自动设置前台作者信息
        fillPortalAuthorAndCategory(portalArticle);
        // 自动生成或校验 slug（草稿阶段也填充，便于后续发布）
        fillSlug(portalArticle);
        // 草稿强制状态为 draft
        portalArticle.setStatus("draft");

        if (portalArticle.getId() == null) {
            // 新建草稿
            baseMapper.insertPortalArticle(portalArticle);
        } else {
            // 更新已有草稿
            baseMapper.updatePortalArticle(portalArticle);
        }
        // 重新查询返回完整实体（含 createTime / updateTime 等数据库默认值）
        return baseMapper.selectPortalArticleById(portalArticle.getId());
    }

    /**
     * 自动设置前台作者信息和分类路径
     *
     * @param portalArticle 文章信息
     */
    private void fillPortalAuthorAndCategory(PortalArticle portalArticle) {
        // 自动设置作者ID（如果还没设置）
        if (portalArticle.getAuthorId() == null) {
            portalArticle.setAuthorId(PortalSecurityUtils.getUserId());
        }
        // 同步维护 category_path 与 root_category_id（选择栏目时字段拼接）
        fillCategoryPath(portalArticle);
    }

    /**
     * 处理文章的Base64图片（封面、富文本、Markdown内容）
     *
     * @param portalArticle 文章信息
     */
    private void processArticleImages(PortalArticle portalArticle) {
        // 处理封面
        if (portalArticle.getCover() != null) {
            portalArticle.setCover(base64ImageUtils.uploadBase64Image(portalArticle.getCover()));
        }
        // 处理富文本内容
        if (portalArticle.getContent() != null) {
            portalArticle.setContent(base64ImageUtils.processContentImages(portalArticle.getContent()));
        }
        // 处理Markdown内容
        if (portalArticle.getContentMarkdown() != null) {
            portalArticle.setContentMarkdown(base64ImageUtils.processContentImages(portalArticle.getContentMarkdown()));
        }
    }
    
    /**
     * 自动填充分类路径和顶级分类ID
     * 选择栏目时拼接所有祖先分类ID（逗号分隔），并标记顶级分类ID
     * 例如：分类为"技术 > 后端 > Spring"，category_path = "1,3,5"，root_category_id = 1
     *
     * @param portalArticle 文章信息
     */
    private void fillCategoryPath(PortalArticle portalArticle) {
        if (portalArticle.getCategoryId() != null) {
            portalArticle.setCategoryPath(portalCategoryService.getCategoryPath(portalArticle.getCategoryId()));
            portalArticle.setRootCategoryId(portalCategoryService.getRootCategoryId(portalArticle.getCategoryId()));
        }
    }

    /**
     * 自动生成或校验文章 slug（用于 SEO 语义化路径）
     * 规则：
     * 1. 若用户自定义 slug，则清洗为合法形式并保证唯一性
     * 2. 否则从标题提取 ASCII 字母数字生成 slug（如 "Spring Boot 最佳实践" → "spring-boot"）
     * 3. 标题无 ASCII 字符（纯中文）时回退为 "article-{时间戳}-{随机数}"
     * 4. 检查 uk_slug 唯一索引冲突，冲突时追加 -2、-3 后缀
     *
     * @param portalArticle 文章信息
     */
    private void fillSlug(PortalArticle portalArticle) {
        // 更新场景：保持已有 slug（避免覆盖已索引的 URL）
        if (portalArticle.getId() != null) {
            PortalArticle existing = baseMapper.selectPortalArticleById(portalArticle.getId());
            if (existing != null && existing.getSlug() != null && !existing.getSlug().isEmpty()) {
                // 仅当用户显式提供新 slug 且与现值不同时才更新
                if (portalArticle.getSlug() == null || portalArticle.getSlug().isEmpty()) {
                    portalArticle.setSlug(existing.getSlug());
                }
                // 若提供了新 slug，下方清洗与唯一性校验仍会执行
            }
        }

        String rawSlug = portalArticle.getSlug();
        if (rawSlug == null) {
            rawSlug = "";
        }
        rawSlug = rawSlug.trim();
        if (rawSlug.isEmpty()) {
            // 从标题生成
            rawSlug = generateSlugFromTitle(portalArticle.getTitle());
        } else {
            // 清洗用户自定义 slug
            rawSlug = sanitizeSlug(rawSlug);
            if (rawSlug.isEmpty()) {
                rawSlug = generateSlugFromTitle(portalArticle.getTitle());
            }
        }

        // 唯一性校验（排除自身）
        String uniqueSlug = ensureSlugUnique(rawSlug, portalArticle.getId());
        portalArticle.setSlug(uniqueSlug);
    }

    /**
     * 从标题生成 slug：提取 ASCII 字母数字，转小写，非字母数字字符替换为连字符
     * 标题无 ASCII 字符时回退为 article-{时间戳}
     *
     * @param title 文章标题
     * @return slug 字符串
     */
    private String generateSlugFromTitle(String title) {
        if (title == null || title.isEmpty()) {
            return "article-" + System.currentTimeMillis();
        }
        // 提取 ASCII 字母数字与空格
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < title.length(); i++) {
            char c = title.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == ' ' || c == '-' || c == '_') {
                sb.append(c);
            }
        }
        String cleaned = sb.toString().trim().toLowerCase();
        if (cleaned.isEmpty()) {
            // 纯中文标题无 ASCII，回退到 article-{时间戳}-{随机后缀}
            return "article-" + System.currentTimeMillis() + "-" + (int) (Math.random() * 1000);
        }
        // 非字母数字字符替换为连字符，合并连续连字符
        return sanitizeSlug(cleaned);
    }

    /**
     * 清洗 slug：转小写，非 [a-z0-9-] 字符替换为连字符，合并连续连字符，去除首尾连字符
     *
     * @param slug 待清洗的 slug
     * @return 清洗后的 slug
     */
    private String sanitizeSlug(String slug) {
        if (slug == null || slug.isEmpty()) {
            return "";
        }
        String cleaned = slug.trim().toLowerCase();
        StringBuilder sb = new StringBuilder();
        boolean lastWasHyphen = false;
        for (int i = 0; i < cleaned.length(); i++) {
            char c = cleaned.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) {
                sb.append(c);
                lastWasHyphen = false;
            } else {
                // 非字母数字统一替换为连字符
                if (!lastWasHyphen && sb.length() > 0) {
                    sb.append('-');
                    lastWasHyphen = true;
                }
            }
        }
        // 去除尾部连字符
        while (sb.length() > 0 && sb.charAt(sb.length() - 1) == '-') {
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }

    /**
     * 确保 slug 唯一：若已存在同名 slug（非自身），追加 -2、-3 后缀
     *
     * @param slug      原始 slug
     * @param excludeId 排除的文章ID（更新场景下排除自身）
     * @return 唯一的 slug
     */
    private String ensureSlugUnique(String slug, Long excludeId) {
        if (slug == null || slug.isEmpty()) {
            return slug;
        }
        String candidate = slug;
        int suffix = 2;
        while (true) {
            PortalArticle existed = baseMapper.selectPortalArticleBySlug(candidate);
            if (existed == null) {
                return candidate;
            }
            // 更新场景下，若是自身则不算冲突
            if (excludeId != null && existed.getId() != null && existed.getId().equals(excludeId)) {
                return candidate;
            }
            candidate = slug + "-" + suffix;
            suffix++;
            // 防止极端情况下死循环
            if (suffix > 1000) {
                return slug + "-" + System.currentTimeMillis();
            }
        }
    }

    /**
     * 通过文章ID删除文章
     *
     * @param id 文章ID
     * @return 结果
     */
    @Override
    public int deletePortalArticleById(Long id) {
        return baseMapper.deletePortalArticleById(id);
    }

    /**
     * 批量删除文章信息
     *
     * @param ids 需要删除的文章ID
     * @return 结果
     */
    @Override
    public int deletePortalArticleByIds(Long[] ids) {
        return baseMapper.deletePortalArticleByIds(ids);
    }
}
