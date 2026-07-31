package com.moyun.portal.service.impl;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moyun.common.exception.system.ServiceException;
import com.moyun.portal.domain.entity.PortalArticle;
import com.moyun.portal.domain.entity.PortalArticleVersion;
import com.moyun.portal.domain.entity.PortalUser;
import com.moyun.portal.mapper.PortalArticleMapper;
import com.moyun.portal.mapper.PortalArticleVersionMapper;
import com.moyun.portal.mapper.PortalUserMapper;
import com.moyun.portal.service.IPortalArticleVersionService;
import com.moyun.portal.util.PortalSecurityUtils;

/**
 * 文章版本快照 业务层实现
 *
 * @author moyun
 */
@Service
public class PortalArticleVersionServiceImpl
        extends ServiceImpl<PortalArticleVersionMapper, PortalArticleVersion>
        implements IPortalArticleVersionService {

    @Autowired
    private PortalArticleMapper portalArticleMapper;

    @Autowired
    private PortalUserMapper portalUserMapper;

    /**
     * 保存版本快照（version_no 自增，内容未变化时跳过）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PortalArticleVersion saveVersion(PortalArticle article, Long operatorId) {
        if (article == null || article.getId() == null) {
            return null;
        }
        Integer maxNo = baseMapper.selectMaxVersionNo(article.getId());
        int nextNo = (maxNo == null ? 0 : maxNo) + 1;

        // 内容去重：与最新版本完全一致时跳过（避免草稿自动保存产生冗余版本）
        if (maxNo != null) {
            PortalArticleVersion latest = getLatestVersion(article.getId());
            if (latest != null && contentUnchanged(latest, article)) {
                return null;
            }
        }

        PortalArticleVersion version = new PortalArticleVersion();
        version.setArticleId(article.getId());
        version.setVersionNo(nextNo);
        version.setTitle(article.getTitle());
        version.setContent(article.getContent());
        version.setContentMarkdown(article.getContentMarkdown());
        version.setExcerpt(article.getExcerpt());
        version.setOperatorId(operatorId);
        version.setCreatedTime(LocalDateTime.now());
        baseMapper.insert(version);
        return version;
    }

    @Override
    public List<PortalArticleVersion> listVersions(Long articleId) {
        // 归属校验：仅作者本人可查看自己文章的版本列表（含草稿版本），防止越权读取他人草稿版本
        checkOwnership(articleId, PortalSecurityUtils.getUserId());
        return baseMapper.selectVersionList(articleId);
    }

    @Override
    public PortalArticleVersion getVersion(Long versionId) {
        PortalArticleVersion version = baseMapper.selectById(versionId);
        if (version == null) {
            return null;
        }
        // 归属校验：查到版本后查对应文章，校验当前用户为文章作者，防止越权读取他人草稿版本
        if (version.getArticleId() != null) {
            checkOwnership(version.getArticleId(), PortalSecurityUtils.getUserId());
        }
        return version;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PortalArticleVersion rollback(Long articleId, Long versionId, Long operatorId) {
        PortalArticleVersion target = baseMapper.selectById(versionId);
        if (target == null || !Objects.equals(target.getArticleId(), articleId)) {
            throw new ServiceException("版本不存在或不属于该文章");
        }
        // 权限校验：仅作者本人可回滚
        PortalArticle article = portalArticleMapper.selectById(articleId);
        if (article == null) {
            throw new ServiceException("文章不存在");
        }
        if (operatorId == null || !Objects.equals(article.getAuthorId(), operatorId)) {
            throw new ServiceException("无权回滚该文章");
        }

        // 覆盖文章内容字段（仅内容相关，不影响分类/标签/状态/计数）
        LambdaUpdateWrapper<PortalArticle> update = new LambdaUpdateWrapper<>();
        update.eq(PortalArticle::getId, articleId)
                .set(PortalArticle::getTitle, target.getTitle())
                .set(PortalArticle::getContent, target.getContent())
                .set(PortalArticle::getContentMarkdown, target.getContentMarkdown())
                .set(PortalArticle::getExcerpt, target.getExcerpt());
        portalArticleMapper.update(null, update);

        // 同步内存中的 article，便于 saveVersion 生成回滚快照
        article.setTitle(target.getTitle());
        article.setContent(target.getContent());
        article.setContentMarkdown(target.getContentMarkdown());
        article.setExcerpt(target.getExcerpt());

        // 生成回滚后的新版本快照
        return saveVersion(article, operatorId);
    }

    @Override
    public Map<String, Object> diff(Long articleId, Integer v1, Integer v2) {
        // 归属校验：仅作者本人可对比自己文章的版本，防止越权读取他人草稿版本内容
        checkOwnership(articleId, PortalSecurityUtils.getUserId());
        PortalArticleVersion va = findByArticleAndVersionNo(articleId, v1);
        PortalArticleVersion vb = findByArticleAndVersionNo(articleId, v2);
        Map<String, Object> result = new HashMap<>();
        result.put("v1", toDiffMap(va));
        result.put("v2", toDiffMap(vb));
        return result;
    }

    // ==================== 私有方法 ====================

    /**
     * 文章归属校验：校验文章存在且 authorId 与当前用户一致
     * 参照 rollback 方法的归属校验写法，用于版本列表/详情/对比接口的越权防护
     *
     * @param articleId  文章ID
     * @param operatorId 当前登录用户ID
     * @throws ServiceException 文章不存在或无权操作时抛出
     */
    private void checkOwnership(Long articleId, Long operatorId) {
        if (articleId == null) {
            throw new ServiceException("文章ID不能为空");
        }
        PortalArticle article = portalArticleMapper.selectById(articleId);
        if (article == null) {
            throw new ServiceException("文章不存在");
        }
        if (operatorId == null || !Objects.equals(article.getAuthorId(), operatorId)) {
            throw new ServiceException("无权查看该文章版本");
        }
    }

    private PortalArticleVersion getLatestVersion(Long articleId) {
        LambdaQueryWrapper<PortalArticleVersion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PortalArticleVersion::getArticleId, articleId)
                .orderByDesc(PortalArticleVersion::getVersionNo)
                .last("LIMIT 1");
        return baseMapper.selectOne(wrapper);
    }

    private PortalArticleVersion findByArticleAndVersionNo(Long articleId, Integer versionNo) {
        LambdaQueryWrapper<PortalArticleVersion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PortalArticleVersion::getArticleId, articleId)
                .eq(PortalArticleVersion::getVersionNo, versionNo);
        return baseMapper.selectOne(wrapper);
    }

    private boolean contentUnchanged(PortalArticleVersion latest, PortalArticle article) {
        return Objects.equals(str(latest.getTitle()), str(article.getTitle()))
                && Objects.equals(str(latest.getContent()), str(article.getContent()))
                && Objects.equals(str(latest.getContentMarkdown()), str(article.getContentMarkdown()))
                && Objects.equals(str(latest.getExcerpt()), str(article.getExcerpt()));
    }

    private String str(String s) {
        return s == null ? "" : s;
    }

    private Map<String, Object> toDiffMap(PortalArticleVersion v) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (v == null) {
            map.put("found", false);
            return map;
        }
        map.put("found", true);
        map.put("id", v.getId());
        map.put("versionNo", v.getVersionNo());
        map.put("title", v.getTitle());
        map.put("content", v.getContent());
        map.put("contentMarkdown", v.getContentMarkdown());
        map.put("excerpt", v.getExcerpt());
        map.put("createdTime", v.getCreatedTime());
        return map;
    }
}
