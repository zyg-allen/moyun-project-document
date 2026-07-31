package com.moyun.portal.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moyun.ext.cms.service.IFeedService;
import com.moyun.portal.domain.entity.PortalArticle;
import com.moyun.portal.domain.entity.PortalArticleVersion;
import com.moyun.portal.domain.entity.PortalBookmark;
import com.moyun.portal.domain.entity.PortalCategory;
import com.moyun.portal.domain.entity.PortalComment;
import com.moyun.portal.domain.entity.PortalLike;
import com.moyun.portal.domain.entity.PortalTipOrder;
import com.moyun.portal.domain.entity.PortalUser;
import com.moyun.portal.domain.query.ArticleQuery;
import com.moyun.portal.mapper.PortalArticleMapper;
import com.moyun.portal.mapper.PortalArticleVersionMapper;
import com.moyun.portal.mapper.PortalBookmarkMapper;
import com.moyun.portal.mapper.PortalCommentMapper;
import com.moyun.portal.mapper.PortalLikeMapper;
import com.moyun.portal.mapper.PortalTipOrderMapper;
import com.moyun.portal.mapper.PortalUserMapper;
import com.moyun.portal.mapper.PortalUserStatsMapper;
import com.moyun.portal.service.IPortalArticleService;
import com.moyun.portal.service.IPortalArticleVersionService;
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
    private PortalUserMapper portalUserMapper;

    @Autowired
    private Base64ImageUtils base64ImageUtils;

    @Autowired
    private IPortalGrowthService portalGrowthService;

    @Autowired
    private PortalUserStatsMapper userStatsMapper;

    @Autowired
    private IFeedService feedService;

    @Autowired
    private IPortalArticleVersionService articleVersionService;

    // 以下 Mapper 用于删除文章时级联清理关联数据（评论/点赞/收藏/版本/打赏订单）
    @Autowired
    private PortalCommentMapper portalCommentMapper;

    @Autowired
    private PortalLikeMapper portalLikeMapper;

    @Autowired
    private PortalBookmarkMapper portalBookmarkMapper;

    @Autowired
    private PortalArticleVersionMapper portalArticleVersionMapper;

    @Autowired
    private PortalTipOrderMapper portalTipOrderMapper;

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
     * 修改文章信息
     *
     * @param portalArticle 文章信息
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updatePortalArticle(PortalArticle portalArticle) {
        // 归属校验：当 id 非空时（更新场景），校验当前用户为文章作者，防止越权修改他人文章
        if (portalArticle.getId() != null) {
            checkOwnership(portalArticle.getId(), PortalSecurityUtils.getUserId());
        }
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
    @Transactional(rollbackFor = Exception.class)
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
        // 付费阅读字段默认值
        if (portalArticle.getIsPaid() == null) {
            portalArticle.setIsPaid(0);
        }
        if (portalArticle.getPreviewLength() == null) {
            portalArticle.setPreviewLength(0);
        }
        if (portalArticle.getPrice() == null) {
            portalArticle.setPrice(java.math.BigDecimal.ZERO);
        }
        // 幂等去重：有 id 走更新；无 id 但有 sessionToken 时按 token 查找已有记录
        // 保证一次编辑会话只产生一条文章记录（草稿→发布沿用同一条）
        if (portalArticle.getId() == null && portalArticle.getSessionToken() != null
                && !portalArticle.getSessionToken().isBlank()) {
            PortalArticle existing = baseMapper.selectOne(new LambdaQueryWrapper<PortalArticle>()
                    .eq(PortalArticle::getSessionToken, portalArticle.getSessionToken())
                    .eq(PortalArticle::getAuthorId, portalArticle.getAuthorId())
                    .last("LIMIT 1"));
            if (existing != null) {
                portalArticle.setId(existing.getId());
            }
        }

        // 归属校验：当 id 非空时（草稿转发布走更新路径），校验当前用户为文章作者，
        // 防止越权发布/覆盖他人文章（sessionToken 解析出的 id 已按 authorId 过滤，此处为二次兜底）
        if (portalArticle.getId() != null) {
            checkOwnership(portalArticle.getId(), PortalSecurityUtils.getUserId());
        }

        // 有 id 时走更新（草稿发布），无 id 时新建
        boolean isNew = portalArticle.getId() == null;
        // 是否为首次发布（草稿→待审核）。更新场景下需查询原状态：
        // 仅当原状态为 draft 且从未发布过（published_at 为空）时，才视为首次发布，
        // 触发成长事件/Feed。updatePortalArticle 采用 NOT_NULL 策略，publishedAt 在草稿
        // 阶段不会被清空，因此 publishedAt==null 可靠地表示"从未发布过"，
        // 避免：草稿转发布已触发成长事件 → 被拒 → 重新编辑再发布时重复触发。
        boolean isFirstPublish = isNew;
        if (!isNew) {
            PortalArticle before = baseMapper.selectPortalArticleById(portalArticle.getId());
            if (before != null && "draft".equals(before.getStatus()) && before.getPublishedAt() == null) {
                isFirstPublish = true;
            }
        }
        int rows;
        if (isNew) {
            rows = baseMapper.insertPortalArticle(portalArticle);
        } else {
            rows = baseMapper.updatePortalArticle(portalArticle);
        }

        // 记录成长事件 + 更新创作字数统计（首次发布时触发，避免重复加成长值）
        if (rows > 0 && isFirstPublish && portalArticle.getAuthorId() != null) {
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

            // 发布动态事件（Feed 流）。try-catch 包裹，避免 Feed 失败影响文章发布主流程
            try {
                feedService.publishEvent(portalArticle.getAuthorId(), "publish_article", "article",
                        portalArticle.getId(), portalArticle.getTitle(),
                        portalArticle.getExcerpt(), portalArticle.getCover());
            } catch (Exception e) {
                org.slf4j.LoggerFactory.getLogger(PortalArticleServiceImpl.class)
                        .error("[Feed] 文章发布动态事件失败：articleId={}", portalArticle.getId(), e);
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
    @Transactional(rollbackFor = Exception.class)
    public PortalArticle saveDraft(PortalArticle portalArticle) {
        // 自动处理Base64图片
        processArticleImages(portalArticle);
        // 自动设置前台作者信息
        fillPortalAuthorAndCategory(portalArticle);
        // 自动生成或校验 slug（草稿阶段也填充，便于后续发布）
        fillSlug(portalArticle);
        // 草稿强制状态为 draft
        portalArticle.setStatus("draft");
        // 付费阅读字段默认值
        if (portalArticle.getIsPaid() == null) {
            portalArticle.setIsPaid(0);
        }
        if (portalArticle.getPreviewLength() == null) {
            portalArticle.setPreviewLength(0);
        }
        if (portalArticle.getPrice() == null) {
            portalArticle.setPrice(java.math.BigDecimal.ZERO);
        }

        // 幂等去重：优先按 id 更新，无 id 时按 sessionToken 查找已有记录
        // 保证一次编辑会话只产生一条文章记录
        if (portalArticle.getId() == null && portalArticle.getSessionToken() != null
                && !portalArticle.getSessionToken().isBlank()) {
            // 双重保障：按 sessionToken 查找同会话已有记录
            PortalArticle existing = baseMapper.selectOne(new LambdaQueryWrapper<PortalArticle>()
                    .eq(PortalArticle::getSessionToken, portalArticle.getSessionToken())
                    .eq(PortalArticle::getAuthorId, portalArticle.getAuthorId())
                    .last("LIMIT 1"));
            if (existing != null) {
                portalArticle.setId(existing.getId());
            }
        }

        // 归属校验：当 id 非空时（更新已有草稿），校验当前用户为文章作者，
        // 防止越权覆盖他人草稿（sessionToken 解析出的 id 已按 authorId 过滤，此处为二次兜底）
        if (portalArticle.getId() != null) {
            checkOwnership(portalArticle.getId(), PortalSecurityUtils.getUserId());
        }

        if (portalArticle.getId() == null) {
            // 新建草稿
            baseMapper.insertPortalArticle(portalArticle);
        } else {
            // 更新已有记录（保持草稿状态不被覆盖）
            // 注意：不限制原状态，允许 draft→draft、pending→draft（打回重编）等场景
            baseMapper.updatePortalArticle(portalArticle);
        }
        // 重新查询返回完整实体（含 createTime / updateTime 等数据库默认值）
        PortalArticle saved = baseMapper.selectPortalArticleById(portalArticle.getId());
        // 生成草稿版本快照（保存文章时调用，version_no 自增；saveVersion 内置内容去重，
        // 避免草稿自动保存产生冗余版本）。try-catch 保证版本失败不影响草稿保存主流程。
        try {
            articleVersionService.saveVersion(saved, saved.getAuthorId());
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(PortalArticleServiceImpl.class)
                    .warn("[版本] 草稿版本快照失败：articleId={}", saved.getId(), e);
        }
        return saved;
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
     * 批量删除文章信息
     *
     * @param ids 需要删除的文章ID
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deletePortalArticleByIds(Long[] ids) {
        if (ids == null || ids.length == 0) {
            return 0;
        }
        // 逐条校验归属：仅作者本人可删除自己的文章，防止越权删除他人文章
        Long currentUserId = PortalSecurityUtils.getUserId();
        for (Long id : ids) {
            checkOwnership(id, currentUserId);
        }
        // 逐条级联清理关联数据（评论/点赞/收藏/版本/打赏订单），避免脏数据
        for (Long id : ids) {
            cascadeDeleteByArticleId(id);
        }
        return baseMapper.deletePortalArticleByIds(ids);
    }

    /**
     * 文章归属校验：校验文章存在且 authorId 与当前用户一致
     * 用于 update/publish/saveDraft/delete 等写操作前的越权防护
     *
     * @param articleId    文章ID
     * @param currentUserId 当前登录用户ID
     * @throws RuntimeException 文章不存在或无权操作他人文章时抛出
     */
    private void checkOwnership(Long articleId, Long currentUserId) {
        if (articleId == null) {
            throw new RuntimeException("文章ID不能为空");
        }
        PortalArticle existing = baseMapper.selectPortalArticleById(articleId);
        if (existing == null) {
            throw new RuntimeException("文章不存在");
        }
        if (currentUserId == null || !existing.getAuthorId().equals(currentUserId)) {
            throw new RuntimeException("无权操作他人文章");
        }
    }

    /**
     * 级联清理文章关联数据（评论/点赞/收藏/版本/打赏订单）
     * 在删除主表文章记录前调用，避免出现脏数据。使用 LambdaQueryWrapper 批量条件删除。
     *
     * @param articleId 文章ID
     */
    private void cascadeDeleteByArticleId(Long articleId) {
        // 删除关联评论
        portalCommentMapper.delete(new LambdaQueryWrapper<PortalComment>()
                .eq(PortalComment::getArticleId, articleId));
        // 删除关联点赞
        portalLikeMapper.delete(new LambdaQueryWrapper<PortalLike>()
                .eq(PortalLike::getArticleId, articleId));
        // 删除关联收藏
        portalBookmarkMapper.delete(new LambdaQueryWrapper<PortalBookmark>()
                .eq(PortalBookmark::getArticleId, articleId));
        // 删除关联版本
        portalArticleVersionMapper.delete(new LambdaQueryWrapper<PortalArticleVersion>()
                .eq(PortalArticleVersion::getArticleId, articleId));
        // 删除关联打赏订单（article 与 article_paid 两种 targetType）
        portalTipOrderMapper.delete(new LambdaQueryWrapper<PortalTipOrder>()
                .eq(PortalTipOrder::getTargetId, articleId)
                .in(PortalTipOrder::getTargetType, "article", "article_paid"));
    }
}
