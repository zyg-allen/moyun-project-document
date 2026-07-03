package com.moyun.portal.service;

import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.extension.service.IService;

import com.moyun.portal.domain.entity.PortalArticle;
import com.moyun.portal.domain.entity.PortalArticleVersion;

/**
 * 文章版本快照 业务层
 * <p>
 * 保存文章时调用 saveVersion 生成版本快照（version_no 自增），
 * 支持版本列表、详情、回滚、对比（对比仅返回两个版本文本，不实现真正的 diff 算法）。
 *
 * @author moyun
 */
public interface IPortalArticleVersionService extends IService<PortalArticleVersion> {

    /**
     * 保存版本快照（保存文章时调用，version_no 自增）。
     * <p>
     * 内容去重：与最新版本内容完全一致时跳过，避免草稿自动保存产生冗余版本。
     *
     * @param article    文章实体（取 title/content/contentMarkdown/excerpt 作为快照源）
     * @param operatorId 操作人ID
     * @return 新建版本快照；内容未变化时返回 null
     */
    PortalArticleVersion saveVersion(PortalArticle article, Long operatorId);

    /**
     * 版本列表（按版本号降序，不含大字段）
     *
     * @param articleId 文章ID
     * @return 版本列表
     */
    List<PortalArticleVersion> listVersions(Long articleId);

    /**
     * 版本详情（含完整内容）
     *
     * @param versionId 版本ID
     * @return 版本快照
     */
    PortalArticleVersion getVersion(Long versionId);

    /**
     * 回滚：将文章内容覆盖回指定版本，并基于回滚后的内容生成新版本快照。
     *
     * @param articleId  文章ID
     * @param versionId  目标版本ID
     * @param operatorId 操作人ID
     * @return 新版本快照（回滚产生的快照）
     */
    PortalArticleVersion rollback(Long articleId, Long versionId, Long operatorId);

    /**
     * 版本对比：直接返回两个版本的 title + content 文本，前端做展示（不实现真正的 diff 算法）。
     *
     * @param articleId 文章ID
     * @param v1        版本号1
     * @param v2        版本号2
     * @return Map 包含 v1 / v2 两个版本的 id/versionNo/title/content/contentMarkdown/excerpt
     */
    Map<String, Object> diff(Long articleId, Integer v1, Integer v2);
}
