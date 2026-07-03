package com.moyun.ext.cms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.common.exception.system.ServiceException;
import com.moyun.core.base.page.PageDomain;
import com.moyun.ext.cms.domain.query.ColumnQuery;
import com.moyun.ext.cms.domain.vo.ColumnArticleSortItem;
import com.moyun.ext.cms.domain.vo.ColumnListItemVO;
import com.moyun.ext.cms.domain.vo.ColumnVO;
import com.moyun.ext.cms.service.IColumnService;
import com.moyun.ext.cms.service.IFeedService;
import com.moyun.portal.domain.entity.PortalArticle;
import com.moyun.portal.domain.entity.PortalColumn;
import com.moyun.portal.domain.entity.PortalColumnArticle;
import com.moyun.portal.domain.entity.PortalColumnSubscribe;
import com.moyun.portal.mapper.PortalArticleMapper;
import com.moyun.portal.mapper.PortalColumnArticleMapper;
import com.moyun.portal.mapper.PortalColumnMapper;
import com.moyun.portal.mapper.PortalColumnSubscribeMapper;
import com.moyun.util.bean.PageUtils;
import com.moyun.util.string.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 专栏 Service 实现（创作者天堂核心）
 *
 * @author moyun
 */
@Service
public class ColumnServiceImpl implements IColumnService {

    /** 单用户专栏数量上限 */
    private static final int MAX_COLUMN_PER_USER = 10;

    @Autowired private PortalColumnMapper columnMapper;
    @Autowired private PortalColumnArticleMapper columnArticleMapper;
    @Autowired private PortalColumnSubscribeMapper columnSubscribeMapper;
    @Autowired private PortalArticleMapper articleMapper;
    @Autowired(required = false) private IFeedService feedService;

    // ========================================================================
    // 列表 / 详情
    // ========================================================================
    @Override
    public Page<ColumnListItemVO> listColumns(ColumnQuery query) {
        Page<ColumnListItemVO> page = PageUtils.buildPage(query);
        return columnMapper.selectListPage(page, query);
    }

    @Override
    public ColumnVO getColumnDetail(Long id, Long currentUserId) {
        ColumnVO vo = columnMapper.selectDetailById(id);
        if (vo == null) {
            return null;
        }
        // 当前用户是否已订阅（未登录视为未订阅）
        boolean subscribed = currentUserId != null
                && columnSubscribeMapper.selectByColumnAndUser(id, currentUserId) != null;
        vo.setIsSubscribed(subscribed);
        // 文章目录
        vo.setArticles(columnArticleMapper.selectArticlesByColumn(id));
        // 浏览数：作者本人访问不计入，避免自我刷量
        if (currentUserId == null || !currentUserId.equals(vo.getUserId())) {
            columnMapper.updateViewCount(id, 1);
        }
        return vo;
    }

    // ========================================================================
    // 创建 / 修改
    // ========================================================================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveColumn(ColumnVO vo, Long userId) {
        if (userId == null) {
            throw new ServiceException("请登录后操作");
        }
        if (vo == null || StringUtils.isEmpty(vo.getTitle())) {
            throw new ServiceException("专栏名不能为空");
        }

        boolean isNew = vo.getId() == null || vo.getId() <= 0;
        PortalColumn entity;
        if (isNew) {
            // 创建：校验同用户专栏数量上限
            int existCount = columnMapper.countByUserId(userId);
            if (existCount >= MAX_COLUMN_PER_USER) {
                throw new ServiceException("专栏数量已达上限（" + MAX_COLUMN_PER_USER + " 个）");
            }
            entity = new PortalColumn();
            entity.setUserId(userId);
            entity.setStatus(StringUtils.isNotEmpty(vo.getStatus()) ? vo.getStatus() : "draft");
            entity.setArticleCount(0);
            entity.setSubscribeCount(0);
            entity.setViewCount(0);
            entity.setIsFinished(vo.getIsFinished() != null ? vo.getIsFinished() : 0);
            entity.setPrice(vo.getPrice() != null ? vo.getPrice() : BigDecimal.ZERO);
            entity.setCreatedTime(LocalDateTime.now());
        } else {
            // 修改：校验归属
            entity = columnMapper.selectById(vo.getId());
            if (entity == null) {
                throw new ServiceException("专栏不存在");
            }
            if (!entity.getUserId().equals(userId)) {
                throw new ServiceException("无权修改该专栏");
            }
            if (vo.getStatus() != null) {
                entity.setStatus(vo.getStatus());
            }
            if (vo.getIsFinished() != null) {
                entity.setIsFinished(vo.getIsFinished());
            }
            if (vo.getPrice() != null) {
                entity.setPrice(vo.getPrice());
            }
        }

        entity.setTitle(vo.getTitle());
        entity.setSubtitle(vo.getSubtitle());
        entity.setDescription(vo.getDescription());
        entity.setCover(vo.getCover());
        entity.setCategoryId(vo.getCategoryId());
        entity.setUpdatedTime(LocalDateTime.now());

        if (isNew) {
            columnMapper.insert(entity);
            // 仅新建时推送 Feed；修改不推送
            if (feedService != null && "published".equals(entity.getStatus())) {
                feedService.publishEvent(userId, "new_column", "column", entity.getId(),
                        entity.getTitle(), entity.getDescription(), entity.getCover());
            }
        } else {
            columnMapper.updateById(entity);
        }
        return entity.getId();
    }

    // ========================================================================
    // 完结 / 恢复连载
    // ========================================================================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int toggleFinish(Long id, Long userId) {
        PortalColumn entity = mustOwnColumn(id, userId);
        int target = entity.getIsFinished() != null && entity.getIsFinished() == 1 ? 0 : 1;
        entity.setIsFinished(target);
        entity.setUpdatedTime(LocalDateTime.now());
        return columnMapper.updateById(entity);
    }

    // ========================================================================
    // 删除（级联）
    // ========================================================================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteColumn(Long id, Long userId) {
        PortalColumn entity = mustOwnColumn(id, userId);
        // 级联删除文章关联与订阅记录，避免孤儿数据
        LambdaQueryWrapper<PortalColumnArticle> articleQw = Wrappers.<PortalColumnArticle>lambdaQuery()
                .eq(PortalColumnArticle::getColumnId, id);
        columnArticleMapper.delete(articleQw);

        LambdaQueryWrapper<PortalColumnSubscribe> subQw = Wrappers.<PortalColumnSubscribe>lambdaQuery()
                .eq(PortalColumnSubscribe::getColumnId, id);
        columnSubscribeMapper.delete(subQw);

        return columnMapper.deleteById(entity.getId());
    }

    // ========================================================================
    // 订阅 / 取消订阅
    // ========================================================================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean toggleSubscribe(Long columnId, Long userId) {
        if (userId == null) {
            throw new ServiceException("请登录后操作");
        }
        PortalColumn column = columnMapper.selectById(columnId);
        if (column == null) {
            throw new ServiceException("专栏不存在");
        }
        PortalColumnSubscribe existing = columnSubscribeMapper.selectByColumnAndUser(columnId, userId);
        if (existing != null) {
            // 已订阅 → 取消
            columnSubscribeMapper.deleteById(existing.getId());
            columnMapper.updateSubscribeCount(columnId, -1);
            return false;
        }
        // 未订阅 → 订阅
        PortalColumnSubscribe sub = new PortalColumnSubscribe();
        sub.setColumnId(columnId);
        sub.setUserId(userId);
        sub.setCreatedTime(LocalDateTime.now());
        try {
            columnSubscribeMapper.insert(sub);
        } catch (DuplicateKeyException e) {
            // 并发兜底：uk_column_user 唯一索引触发，说明已被订阅，按已订阅处理
            columnMapper.updateSubscribeCount(columnId, 1);
            return true;
        }
        columnMapper.updateSubscribeCount(columnId, 1);
        return true;
    }

    // ========================================================================
    // 我创建的 / 我订阅的
    // ========================================================================
    @Override
    public Page<ColumnListItemVO> listMyColumns(Long userId, PageDomain query) {
        Page<ColumnListItemVO> page = PageUtils.buildPage(query);
        return columnMapper.selectMyColumnsPage(page, userId);
    }

    @Override
    public Page<ColumnListItemVO> listSubscribedColumns(Long userId, PageDomain query) {
        Page<ColumnListItemVO> page = PageUtils.buildPage(query);
        return columnMapper.selectSubscribedColumnsPage(page, userId);
    }

    // ========================================================================
    // 文章加入 / 移出专栏
    // ========================================================================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int addArticle(Long columnId, Long articleId, Long userId) {
        // 校验专栏归属
        PortalColumn column = mustOwnColumn(columnId, userId);
        // 校验文章归属
        PortalArticle article = articleMapper.selectById(articleId);
        if (article == null) {
            throw new ServiceException("文章不存在");
        }
        if (article.getAuthorId() == null || !article.getAuthorId().equals(userId)) {
            throw new ServiceException("只能加入自己的文章");
        }
        // 校验是否已加入（uk_column_article 兜底，这里提前给出友好提示）
        LambdaQueryWrapper<PortalColumnArticle> existsQw = Wrappers.<PortalColumnArticle>lambdaQuery()
                .eq(PortalColumnArticle::getColumnId, columnId)
                .eq(PortalColumnArticle::getArticleId, articleId);
        Long exists = columnArticleMapper.selectCount(existsQw);
        if (exists != null && exists > 0) {
            throw new ServiceException("该文章已在专栏中");
        }
        // 追加到末尾：sort_order 取当前最大值 + 1
        int nextSort = nextSortOrder(columnId);
        PortalColumnArticle rel = new PortalColumnArticle();
        rel.setColumnId(columnId);
        rel.setArticleId(articleId);
        rel.setSortOrder(nextSort);
        rel.setCreatedTime(LocalDateTime.now());
        int rows;
        try {
            rows = columnArticleMapper.insert(rel);
        } catch (DuplicateKeyException e) {
            // 并发兜底：uk_column_article 唯一索引触发，说明已被加入
            throw new ServiceException("该文章已在专栏中");
        }
        if (rows > 0) {
            columnMapper.updateArticleCount(columnId, 1);
            // 专栏新增文章 → 推送 Feed
            if (feedService != null) {
                PortalColumn col = columnMapper.selectById(columnId);
                PortalArticle art = articleMapper.selectById(articleId);
                if (col != null && art != null && "published".equals(col.getStatus())) {
                    feedService.publishEvent(col.getUserId(), "column_new_article", "article", art.getId(),
                            art.getTitle(), col.getTitle() + " 更新了新文章", art.getCover());
                }
            }
        }
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int removeArticle(Long columnId, Long articleId, Long userId) {
        mustOwnColumn(columnId, userId);
        LambdaQueryWrapper<PortalColumnArticle> qw = Wrappers.<PortalColumnArticle>lambdaQuery()
                .eq(PortalColumnArticle::getColumnId, columnId)
                .eq(PortalColumnArticle::getArticleId, articleId);
        int rows = columnArticleMapper.delete(qw);
        if (rows > 0) {
            columnMapper.updateArticleCount(columnId, -1);
        }
        return rows;
    }

    // ========================================================================
    // 批量排序
    // ========================================================================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int sortArticles(Long columnId, List<ColumnArticleSortItem> list, Long userId) {
        mustOwnColumn(columnId, userId);
        if (list == null || list.isEmpty()) {
            return 0;
        }
        return columnMapper.updateArticleSort(columnId, list);
    }

    // ========================================================================
    // 内部工具
    // ========================================================================

    /**
     * 校验专栏存在且归属当前用户，返回实体
     */
    private PortalColumn mustOwnColumn(Long columnId, Long userId) {
        if (userId == null) {
            throw new ServiceException("请登录后操作");
        }
        PortalColumn entity = columnMapper.selectById(columnId);
        if (entity == null) {
            throw new ServiceException("专栏不存在");
        }
        if (!entity.getUserId().equals(userId)) {
            throw new ServiceException("无权操作该专栏");
        }
        return entity;
    }

    /**
     * 计算专栏下一篇文章的 sort_order（当前最大值 + 1，空专栏返回 0）
     */
    private int nextSortOrder(Long columnId) {
        LambdaQueryWrapper<PortalColumnArticle> qw = Wrappers.<PortalColumnArticle>lambdaQuery()
                .eq(PortalColumnArticle::getColumnId, columnId)
                .orderByDesc(PortalColumnArticle::getSortOrder)
                .last("LIMIT 1");
        PortalColumnArticle last = columnArticleMapper.selectOne(qw);
        if (last == null || last.getSortOrder() == null) {
            return 0;
        }
        return last.getSortOrder() + 1;
    }
}
