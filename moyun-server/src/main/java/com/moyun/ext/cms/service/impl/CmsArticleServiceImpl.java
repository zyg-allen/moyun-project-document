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
import com.moyun.portal.domain.query.ArticleQuery;
import com.moyun.portal.mapper.PortalArticleMapper;
import com.moyun.util.file.Base64ImageUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
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

    // ==================== 查询方法 ====================

    @Override
    public Page<CmsArticleVO> selectArticlePage(Page<CmsArticleVO> page, CmsArticleQuery query) {
        // 1. 转换查询参数
        ArticleQuery articleQuery = BeanUtil.copyProperties(query, ArticleQuery.class);

        // 2. 创建实体分页对象
        Page<PortalArticle> entityPage = new Page<>(page.getCurrent(), page.getSize());

        // 3. 执行分页查询（MP 自动拦截，自动查询 total）
        IPage<PortalArticle> result = portalArticleMapper.selectPortalArticlePage(entityPage, articleQuery);

        // 4. 转换 VO
        List<CmsArticleVO> voList = BeanUtil.copyToList(result.getRecords(), CmsArticleVO.class);

        // 5. 构造返回结果
        Page<CmsArticleVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(voList);
        return voPage;
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
    public int insertArticle(PortalArticle article) {
        if (article == null) {
            return 0;
        }
        processArticleImages(article);
        fillAdminAuthorInfo(article);
        if (!StringUtils.hasText(article.getStatus())) {
            article.setStatus("published");
        }
        if (article.getPublishedAt() == null) {
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
        // 审核通过设置发布时间；拒绝记录原因到 remark
        if ("published".equals(newStatus)) {
            wrapper.set(PortalArticle::getPublishedAt, LocalDateTime.now());
        } else if (article.getRemark() != null && !article.getRemark().isEmpty()) {
            wrapper.set(PortalArticle::getRemark, article.getRemark());
        }
        int rows = portalArticleMapper.update(null, wrapper);
        if (rows == 0) {
            throw new com.moyun.common.exception.system.ServiceException("审核失败：文章状态已变更，请刷新后重试");
        }
        return rows;
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

    private void fillAdminAuthorInfo(PortalArticle article) {
        // authorId 必须由前端选择 portal_user，不再自动填充 sys_user 的 ID
        // 如果前端未传 authorId，则留空（文章将显示为"匿名作者"）
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