package com.moyun.ext.cms.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.ext.cms.domain.query.CmsArticleQuery;
import com.moyun.ext.cms.domain.vo.CmsArticleVO;
import com.moyun.ext.cms.service.ICmsArticleService;
import com.moyun.portal.domain.entity.PortalArticle;
import com.moyun.portal.domain.entity.PortalUser;
import com.moyun.portal.domain.query.ArticleQuery;
import com.moyun.portal.mapper.PortalArticleMapper;
import com.moyun.portal.mapper.PortalUserMapper;
import com.moyun.portal.service.IPortalCategoryService;
import com.moyun.system.domain.entity.SysNotification;
import com.moyun.system.service.ISysNotificationService;
import com.moyun.util.file.Base64ImageUtils;
import com.moyun.util.security.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * CMS文章服务实现类（标准重构版）
 *
 * @author moyun
 */
@Slf4j
@Service
public class CmsArticleServiceImpl implements ICmsArticleService {

    @Autowired
    private PortalArticleMapper portalArticleMapper;

    @Autowired
    private Base64ImageUtils base64ImageUtils;

    @Autowired
    private PortalUserMapper portalUserMapper;

    @Autowired
    private IPortalCategoryService portalCategoryService;

    @Autowired
    private ISysNotificationService notificationService;

    @Autowired
    private com.moyun.system.service.ISensitiveWordService sensitiveWordService;

    @Autowired
    private com.moyun.portal.service.IPortalTagService portalTagService;

    @Autowired
    private com.moyun.portal.service.IPortalGrowthService portalGrowthService;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    // ==================== 查询方法 ====================

    @Override
    public Page<CmsArticleVO> selectArticlePage(Page<CmsArticleVO> page, CmsArticleQuery query) {
        // 使用 CMS 专用查询（queryWhereCms 不硬编码 status='published'，CMS 可见所有状态文章）
        return portalArticleMapper.selectCmsArticlePage(page, query);
    }

    @Override
    public List<PortalArticle> selectArticleList(CmsArticleQuery query) {
        ArticleQuery articleQuery = BeanUtil.copyProperties(query, ArticleQuery.class);
        return portalArticleMapper.selectPortalArticleList(articleQuery);
    }

    @Override
    public CmsArticleVO selectArticleById(Long id) {
        if (id == null) {
            return null;
        }
        return portalArticleMapper.selectCmsArticleById(id);
    }

    // ==================== 新增 / 修改 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertArticle(PortalArticle article) {
        if (article == null) {
            return 0;
        }
        processArticleImages(article);
        // 后台发布：自动将当前 sys_user 映射到 portal_user（无账户则自动建户），保证 author_id 非空
        fillAdminAuthorInfo(article);
        // 维护分类路径与顶级分类ID（与前台发布链路保持一致）
        fillCategoryPath(article);
        // 生成 SEO slug（为空时按标题生成，保证 uk_slug 唯一索引不冲突）
        fillSlug(article);
        // 后台新建文章默认进入待审核（走审核流程）；管理员可显式传 status=published 直接发布
        if (!StringUtils.hasText(article.getStatus())) {
            article.setStatus("pending");
        }
        if ("published".equals(article.getStatus()) && article.getPublishedAt() == null) {
            article.setPublishedAt(LocalDateTime.now());
        }
        int rows = portalArticleMapper.insert(article);
        // 敏感词轻量扫描：命中即写日志（action=pending），已 pending 的转人工重点审核，
        // 直发 published 的命中文章强制回退 pending，避免违规内容绕过审核直接曝光
        if (rows > 0 && article.getId() != null) {
            try {
                StringBuilder scanText = new StringBuilder();
                if (article.getTitle() != null) scanText.append(article.getTitle());
                if (article.getExcerpt() != null) scanText.append(" ").append(article.getExcerpt());
                if (article.getContent() != null) scanText.append(" ").append(article.getContent());
                List<String> hits = sensitiveWordService.detectAndLog(
                        "article", article.getId(), article.getAuthorId(),
                        scanText.toString(), "pending");
                if (hits != null && !hits.isEmpty()) {
                    log.warn("CMS 文章命中敏感词，强制转待审核：articleId={}, hits={}", article.getId(), hits);
                    if ("published".equals(article.getStatus())) {
                        LambdaUpdateWrapper<PortalArticle> uw = new LambdaUpdateWrapper<>();
                        uw.eq(PortalArticle::getId, article.getId())
                                .set(PortalArticle::getStatus, "pending");
                        portalArticleMapper.update(null, uw);
                        article.setStatus("pending");
                    }
                }
            } catch (Exception e) {
                log.warn("CMS 文章敏感词扫描异常：articleId={}, err={}", article.getId(), e.getMessage());
            }
        }
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateArticle(PortalArticle article) {
        if (article == null || article.getId() == null) {
            return 0;
        }
        // ⚠️ 安全防护：剥离审核相关字段，禁止通过 edit 接口绕过 auditArticle 流程
        // 仅 auditArticle 接口可修改这些字段（带乐观锁与审计日志）
        article.setStatus(null);
        article.setAuditorId(null);
        article.setAuditTime(null);
        article.setAuditRemark(null);
        // publishedAt 仅在审核通过时由 auditArticle 写入，编辑时禁止修改
        article.setPublishedAt(null);

        processArticleImages(article);
        // 编辑时同步维护分类路径（切换分类场景）
        fillCategoryPath(article);
        // 维护 slug 唯一性（用户自定义时校验）
        fillSlug(article);
        return portalArticleMapper.updateById(article);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertArticleWithTags(PortalArticle article, List<Long> tagIds, List<String> tagNames) {
        int rows = insertArticle(article);
        // 标签绑定与文章插入在同一事务内，失败可回滚
        if (rows > 0 && article.getId() != null) {
            portalTagService.bindTags("article", article.getId(), tagIds, tagNames, "article");
        }
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateArticleWithTags(PortalArticle article, List<Long> tagIds, List<String> tagNames) {
        int rows = updateArticle(article);
        // 标签绑定与文章更新在同一事务内
        if (rows > 0 && article.getId() != null) {
            portalTagService.bindTags("article", article.getId(), tagIds, tagNames, "article");
        }
        return rows;
    }

    // ==================== 状态更新（使用 LambdaUpdateWrapper） ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int auditArticle(PortalArticle article) {
        if (article == null || article.getId() == null) {
            return 0;
        }
        // 状态校验：仅允许审核为 published / rejected
        String newStatus = article.getStatus();
        if (!"published".equals(newStatus) && !"rejected".equals(newStatus)) {
            throw new com.moyun.common.exception.system.ServiceException("审核状态仅支持 published / rejected");
        }
        // 仅 pending 状态的文章可被审核（防止重复审核已发布/已拒绝的文章）
        PortalArticle existing = portalArticleMapper.selectById(article.getId());
        if (existing == null) {
            throw new com.moyun.common.exception.system.ServiceException("文章不存在");
        }
        if (!"pending".equals(existing.getStatus())) {
            throw new com.moyun.common.exception.system.ServiceException("仅待审核（pending）状态的文章可审核，当前状态：" + existing.getStatus());
        }

        LambdaUpdateWrapper<PortalArticle> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(PortalArticle::getId, article.getId())
                .eq(PortalArticle::getStatus, "pending") // 乐观锁：仅 pending 可审核
                .set(PortalArticle::getStatus, newStatus)
                .set(PortalArticle::getAuditorId, SecurityUtils.getUserId())   // 审核人
                .set(PortalArticle::getAuditTime, LocalDateTime.now());        // 审核时间
        // 审核通过设置发布时间
        if ("published".equals(newStatus)) {
            wrapper.set(PortalArticle::getPublishedAt, LocalDateTime.now());
        }
        // 审核意见统一写入 audit_remark（独立字段，不再复用通用 remark）
        String auditRemark = article.getAuditRemark() != null ? article.getAuditRemark() : article.getRemark();
        if (auditRemark != null && !auditRemark.isEmpty()) {
            wrapper.set(PortalArticle::getAuditRemark, auditRemark);
        }
        int rows = portalArticleMapper.update(null, wrapper);
        if (rows == 0) {
            throw new com.moyun.common.exception.system.ServiceException("审核失败：文章状态已变更，请刷新后重试");
        }
        // 审核驳回：回滚发布文章时获得的成长值
        // 原始成长值在 PortalArticleServiceImpl.publishArticle 中通过 recordEvent("publish_article") 发放，
        // 这里按 entity 精确回滚当初获得的 growthDelta（含 VIP 加成后的实际值），
        // 幂等：通过 rollbackAction=publish_article_rollback 标记，避免重复扣减
        if ("rejected".equals(newStatus) && existing.getAuthorId() != null) {
            try {
                portalGrowthService.deductGrowthForEntity(
                        "article", "publish_article",
                        existing.getAuthorId(), "article", existing.getId(),
                        "publish_article_rollback");
            } catch (Exception e) {
                // 成长值回滚失败不应阻断审核主流程，但需记录便于人工对账
                log.error("审核驳回成长值回滚失败（不影响审核主流程），articleId={}, authorId={}, err={}",
                        existing.getId(), existing.getAuthorId(), e.getMessage());
            }
        }
        // 审核通过：发布事件，触发 Feed 流补发 + 积分联动（监听器做幂等检查，避免重复）
        // 设计：使用 Spring Event 解耦，监听器在事务提交后异步处理，不影响审核主流程响应
        if ("published".equals(newStatus) && existing.getAuthorId() != null) {
            eventPublisher.publishEvent(new com.moyun.ext.cms.event.ArticlePublishedEvent(
                    this,
                    existing.getId(),
                    existing.getAuthorId(),
                    existing.getTitle(),
                    existing.getExcerpt(),
                    existing.getCover()
            ));
        }
        // 审核结果通知作者（非阻塞，失败不影响主流程）
        sendAuditNotification(existing, newStatus, auditRemark);
        return rows;
    }

    /**
     * 文章审核结果站内信通知作者
     * @param article  文章（用于获取作者ID和标题）
     * @param status   published / rejected
     * @param remark   拒绝原因（仅拒绝时有效）
     */
    private void sendAuditNotification(PortalArticle article, String status, String remark) {
        try {
            if (article.getAuthorId() == null) {
                log.warn("文章 author_id 为空，跳过审核通知：articleId={}", article.getId());
                return;
            }
            SysNotification notification = new SysNotification();
            notification.setType("system");
            notification.setScope("user");
            notification.setUserId(article.getAuthorId());
            notification.setUserType("portal");
            notification.setNoticeType("1");
            notification.setStatus("0");
            if ("published".equals(status)) {
                notification.setTitle("文章审核通过：" + article.getTitle());
                notification.setContent("您的文章《" + article.getTitle() + "》已通过审核并发布。可在「我的文章」中查看详情。");
            } else {
                notification.setTitle("文章审核未通过：" + article.getTitle());
                String reason = StringUtils.hasText(remark) ? remark : "内容不符合平台规范";
                notification.setContent("您的文章《" + article.getTitle() + "》未通过审核，原因：" + reason + "。可在「我的文章」中修改后重新提交。");
            }
            // data 字段供前端点击通知跳转到文章详情
            notification.setData("{\"bizType\":\"article\",\"id\":" + article.getId() + ",\"status\":\"" + status + "\"}");
            notificationService.insertNotification(notification);
            log.info("审核通知已发送，articleId={}, authorId={}, status={}",
                    article.getId(), article.getAuthorId(), status);
        } catch (Exception e) {
            log.error("审核通知发送失败（不影响审核主流程），articleId={}, error={}",
                    article.getId(), e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int publishArticle(PortalArticle article) {
        if (article == null || article.getId() == null) {
            return 0;
        }
        String newStatus = article.getStatus();
        // 上下架仅允许在 published / archived 之间流转
        // 禁止通过此接口把 pending/rejected 直接改为 published（必须走 auditArticle 审核流程）
        if (!"published".equals(newStatus) && !"archived".equals(newStatus)) {
            throw new com.moyun.common.exception.system.ServiceException(
                    "上下架仅支持 published / archived 状态，待审核或被拒文章请走审核接口");
        }
        // 查询当前状态，校验流转合法性
        PortalArticle existing = portalArticleMapper.selectById(article.getId());
        if (existing == null) {
            throw new com.moyun.common.exception.system.ServiceException("文章不存在");
        }
        String oldStatus = existing.getStatus();
        // 允许：published → archived（下架）、archived → published（重新上架）
        // 禁止：pending → published（必须审核）、rejected → published（必须重新提交审核）
        if ("published".equals(newStatus) && ("pending".equals(oldStatus) || "rejected".equals(oldStatus))) {
            throw new com.moyun.common.exception.system.ServiceException(
                    "当前状态为 " + oldStatus + "，不可直接上架，请通过审核接口处理");
        }
        LambdaUpdateWrapper<PortalArticle> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(PortalArticle::getId, article.getId())
                .eq(PortalArticle::getStatus, oldStatus) // 乐观锁：基于原状态
                .set(PortalArticle::getStatus, newStatus);
        if ("published".equals(newStatus)) {
            wrapper.set(PortalArticle::getPublishedAt, LocalDateTime.now());
        }
        int rows = portalArticleMapper.update(null, wrapper);
        if (rows == 0) {
            throw new com.moyun.common.exception.system.ServiceException("上下架失败：文章状态已变更，请刷新后重试");
        }
        return rows;
    }

    @Override
    public int setFeatured(PortalArticle article) {
        if (article == null || article.getId() == null) {
            return 0;
        }
        LambdaUpdateWrapper<PortalArticle> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(PortalArticle::getId, article.getId())
                .set(PortalArticle::getIsFeatured, article.getIsFeatured());
        return portalArticleMapper.update(null, wrapper);
    }

    @Override
    public int setTop(PortalArticle article) {
        if (article == null || article.getId() == null) {
            return 0;
        }
        LambdaUpdateWrapper<PortalArticle> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(PortalArticle::getId, article.getId())
                .set(PortalArticle::getIsTop, article.getIsTop());
        return portalArticleMapper.update(null, wrapper);
    }

    @Override
    public int setCarousel(PortalArticle article) {
        if (article == null || article.getId() == null) {
            return 0;
        }
        LambdaUpdateWrapper<PortalArticle> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(PortalArticle::getId, article.getId())
                .set(PortalArticle::getIsCarousel, article.getIsCarousel());
        return portalArticleMapper.update(null, wrapper);
    }

    // ==================== 删除 ====================

    @Override
    public int deleteArticleByIds(Long[] ids) {
        if (ids == null || ids.length == 0) {
            return 0;
        }
        return portalArticleMapper.deleteBatchIds(Arrays.asList(ids));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteArticleWithTags(Long[] ids) {
        if (ids == null || ids.length == 0) {
            return 0;
        }
        int rows = portalArticleMapper.deleteBatchIds(Arrays.asList(ids));
        // 标签解绑与文章删除在同一事务内，失败可回滚
        if (rows > 0) {
            for (Long id : ids) {
                portalTagService.unbindTags("article", id);
            }
        }
        return rows;
    }

    // ==================== 私有方法 ====================

    /**
     * 后台发布时填充作者信息：
     * 1. 若前端已显式选择 portal_user（authorId 非空），则尊重该选择，直接使用；
     * 2. 否则取当前登录的 sys_user.userId，反查 portal_user.user_id 是否已关联门户账户：
     *    - 已关联：使用该 portal_user.id 作为 author_id；
     *    - 未关联：按"之前协定"自动创建一个 portal_user 账户（携带 sys_user 的基础信息），
     *      并将其 id 作为 author_id，保证 portal_article.author_id NOT NULL 约束不报错。
     *
     * 这样后台管理员发布的文章也会落到门户作者体系，前台作者主页、粉丝、统计均可正常展示。
     */
    private void fillAdminAuthorInfo(PortalArticle article) {
        if (article.getAuthorId() != null) {
            // 前端已指定作者，直接采用
            return;
        }
        Long sysUserId;
        try {
            sysUserId = SecurityUtils.getUserId();
        } catch (Exception e) {
            log.warn("后台发布文章时获取当前 sys_user 失败，author_id 将留空：{}", e.getMessage());
            return;
        }
        if (sysUserId == null) {
            return;
        }
        // 反查 portal_user.user_id 关联记录
        PortalUser portalUser = portalUserMapper.selectOne(
                new LambdaQueryWrapper<PortalUser>().eq(PortalUser::getUserId, sysUserId)
        );
        if (portalUser == null) {
            // 无门户账户，自动建户（关联 sys_user）
            portalUser = createPortalUserForSysUser(sysUserId);
        }
        if (portalUser != null && portalUser.getId() != null) {
            article.setAuthorId(portalUser.getId());
        }
    }

    /**
     * 为后台 sys_user 自动创建门户账户（关联 user_id），返回含 id 的新建实体
     */
    private PortalUser createPortalUserForSysUser(Long sysUserId) {
        com.moyun.core.base.model.LoginUser loginUser = SecurityUtils.getLoginUser();
        com.moyun.core.base.entity.SysUser sysUser = loginUser != null ? loginUser.getUser() : null;

        PortalUser portalUser = new PortalUser();
        portalUser.setUserId(sysUserId);
        // 用户名：优先使用 sys_user.userName，冲突时追加 _{userId}
        String username = sysUser != null ? sysUser.getUserName() : ("sys_" + sysUserId);
        if (portalUserMapper.selectPortalUserByUsername(username) != null) {
            username = username + "_" + sysUserId;
        }
        portalUser.setUsername(username);
        if (sysUser != null) {
            portalUser.setNickname(StringUtils.hasText(sysUser.getNickName()) ? sysUser.getNickName() : username);
            if (StringUtils.hasText(sysUser.getEmail())) {
                portalUser.setEmail(sysUser.getEmail());
            }
            if (StringUtils.hasText(sysUser.getPhonenumber())) {
                portalUser.setPhone(sysUser.getPhonenumber());
            }
            if (StringUtils.hasText(sysUser.getAvatar())) {
                portalUser.setAvatar(sysUser.getAvatar());
            }
        }
        portalUser.setRole("admin");
        portalUser.setStatus("0");
        portalUser.setDelFlag("0");
        // 默认开启通知
        portalUser.setNotifyLike(true);
        portalUser.setNotifyComment(true);
        portalUser.setNotifyFollow(true);
        portalUser.setNotifySystem(true);
        portalUser.setPrivacyFollow(true);

        portalUserMapper.insert(portalUser);
        log.info("为后台用户 sys_user_id={} 自动创建门户账户 portal_user.id={}, username={}",
                sysUserId, portalUser.getId(), portalUser.getUsername());
        return portalUser;
    }

    /**
     * 维护分类路径与顶级分类ID（与前台 PortalArticleServiceImpl.fillCategoryPath 保持一致）
     */
    private void fillCategoryPath(PortalArticle article) {
        if (article.getCategoryId() != null) {
            article.setCategoryPath(portalCategoryService.getCategoryPath(article.getCategoryId()));
            article.setRootCategoryId(portalCategoryService.getRootCategoryId(article.getCategoryId()));
        }
    }

    /**
     * 生成文章 slug（SEO 语义化路径）：
     * - 用户自定义 slug：清洗为合法形式；
     * - 否则按标题生成；
     * - 通过 uk_slug 唯一索引校验，冲突时追加 -2/-3 后缀。
     * 与前台 PortalArticleServiceImpl.fillSlug 行为对齐，保证后台发布的文章也具备 SEO 友好 URL。
     */
    private void fillSlug(PortalArticle article) {
        // 更新场景：未显式提供 slug 时保持原值，避免覆盖已索引 URL
        if (article.getId() != null) {
            PortalArticle existing = portalArticleMapper.selectPortalArticleById(article.getId());
            if (existing != null && StringUtils.hasText(existing.getSlug())) {
                if (!StringUtils.hasText(article.getSlug())) {
                    article.setSlug(existing.getSlug());
                    return;
                }
            }
        }

        String rawSlug = article.getSlug() == null ? "" : article.getSlug().trim();
        if (rawSlug.isEmpty()) {
            rawSlug = generateSlugFromTitle(article.getTitle());
        } else {
            rawSlug = sanitizeSlug(rawSlug);
            if (rawSlug.isEmpty()) {
                rawSlug = generateSlugFromTitle(article.getTitle());
            }
        }
        article.setSlug(ensureSlugUnique(rawSlug, article.getId()));
    }

    private String generateSlugFromTitle(String title) {
        if (!StringUtils.hasText(title)) {
            return "article-" + System.currentTimeMillis();
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < title.length(); i++) {
            char c = title.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == ' ' || c == '-' || c == '_') {
                sb.append(c);
            }
        }
        String cleaned = sb.toString().trim().toLowerCase();
        if (cleaned.isEmpty()) {
            // 纯中文标题无 ASCII，回退到 article-{时间戳}-{随机}
            return "article-" + System.currentTimeMillis() + "-" + (int) (Math.random() * 1000);
        }
        return sanitizeSlug(cleaned);
    }

    private String sanitizeSlug(String slug) {
        if (!StringUtils.hasText(slug)) {
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
                if (!lastWasHyphen && sb.length() > 0) {
                    sb.append('-');
                    lastWasHyphen = true;
                }
            }
        }
        while (sb.length() > 0 && sb.charAt(sb.length() - 1) == '-') {
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }

    private String ensureSlugUnique(String slug, Long excludeId) {
        if (!StringUtils.hasText(slug)) {
            return slug;
        }
        String candidate = slug;
        int suffix = 2;
        while (true) {
            PortalArticle existed = portalArticleMapper.selectPortalArticleBySlug(candidate);
            if (existed == null) {
                return candidate;
            }
            if (excludeId != null && existed.getId() != null && existed.getId().equals(excludeId)) {
                return candidate;
            }
            candidate = slug + "-" + suffix;
            suffix++;
            if (suffix > 1000) {
                return slug + "-" + System.currentTimeMillis();
            }
        }
    }

    private void processArticleImages(PortalArticle article) {
        if (StringUtils.hasText(article.getCover()) && article.getCover().startsWith("data:image")) {
            article.setCover(base64ImageUtils.uploadBase64Image(article.getCover()));
        }
        if (StringUtils.hasText(article.getContent()) && article.getContent().contains("data:image")) {
            article.setContent(base64ImageUtils.processContentImages(article.getContent()));
        }
        if (StringUtils.hasText(article.getContentMarkdown()) && article.getContentMarkdown().contains("data:image")) {
            article.setContentMarkdown(base64ImageUtils.processContentImages(article.getContentMarkdown()));
        }
    }
}