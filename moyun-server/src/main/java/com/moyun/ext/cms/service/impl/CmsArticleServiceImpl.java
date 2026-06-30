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
        return portalArticleMapper.insert(article);
    }

    @Override
    public int updateArticle(PortalArticle article) {
        if (article == null || article.getId() == null) {
            return 0;
        }
        processArticleImages(article);
        // 编辑时同步维护分类路径（切换分类场景）
        fillCategoryPath(article);
        // 维护 slug 唯一性（用户自定义时校验）
        fillSlug(article);
        return portalArticleMapper.updateById(article);
    }

    // ==================== 状态更新（使用 LambdaUpdateWrapper） ====================

    @Override
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
                .set(PortalArticle::getStatus, newStatus);
        // 审核通过设置发布时间
        if ("published".equals(newStatus)) {
            wrapper.set(PortalArticle::getPublishedAt, LocalDateTime.now());
        }
        // 审核意见（通过选填、驳回必填）一律写入 remark，便于"已办理"列表展示
        if (article.getRemark() != null && !article.getRemark().isEmpty()) {
            wrapper.set(PortalArticle::getRemark, article.getRemark());
        }
        int rows = portalArticleMapper.update(null, wrapper);
        if (rows == 0) {
            throw new com.moyun.common.exception.system.ServiceException("审核失败：文章状态已变更，请刷新后重试");
        }
        // 审核结果通知作者（非阻塞，失败不影响主流程）
        sendAuditNotification(existing, newStatus, article.getRemark());
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
            if ("published".equals(status)) {
                notification.setTitle("文章审核通过");
                notification.setContent("您的文章《" + article.getTitle() + "》已通过审核并发布。");
            } else {
                notification.setTitle("文章审核未通过");
                String reason = StringUtils.hasText(remark) ? remark : "内容不符合平台规范";
                notification.setContent("您的文章《" + article.getTitle() + "》未通过审核，原因：" + reason);
            }
            notification.setScope("user");
            notification.setUserId(article.getAuthorId());
            notification.setUserType("portal");
            notification.setStatus("0");
            notificationService.insertNotification(notification);
            log.info("审核通知已发送，articleId={}, authorId={}, status={}",
                    article.getId(), article.getAuthorId(), status);
        } catch (Exception e) {
            log.error("审核通知发送失败（不影响审核主流程），articleId={}, error={}",
                    article.getId(), e.getMessage());
        }
    }

    @Override
    public int publishArticle(PortalArticle article) {
        if (article == null || article.getId() == null) {
            return 0;
        }
        LambdaUpdateWrapper<PortalArticle> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(PortalArticle::getId, article.getId())
                .set(PortalArticle::getStatus, article.getStatus())
                .set(PortalArticle::getPublishedAt, LocalDateTime.now());
        return portalArticleMapper.update(null, wrapper);
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