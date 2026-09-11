package com.moyun.ext.cms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.common.exception.system.ServiceException;
import com.moyun.core.base.AjaxResult;
import com.moyun.util.security.SecurityUtils;
import com.moyun.ext.cms.domain.query.ColumnQuery;
import com.moyun.ext.cms.domain.vo.ArticleSimpleVO;
import com.moyun.ext.cms.domain.vo.ColumnListItemVO;
import com.moyun.ext.cms.service.ICmsColumnService;
import com.moyun.ext.cms.service.IFeedService;
import com.moyun.portal.domain.entity.PortalColumn;
import com.moyun.portal.domain.entity.PortalColumnArticle;
import com.moyun.portal.mapper.PortalColumnArticleMapper;
import com.moyun.portal.mapper.PortalColumnMapper;
import com.moyun.portal.mapper.PortalUserMapper;
import com.moyun.portal.domain.entity.PortalUser;
import com.moyun.system.domain.dto.AuditTaskSubmitDTO;
import com.moyun.system.domain.entity.SysNotification;
import com.moyun.system.service.ISysNotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * CMS 专栏后台管理 Service 实现
 *
 * <p>复用 {@link PortalColumnMapper}，列表查询走 selectAdminListPage（含作者信息），
 * 其余走 BaseMapper 通用方法。</p>
 *
 * @author moyun
 */
@Slf4j
@Service
public class CmsColumnServiceImpl implements ICmsColumnService {

    @Autowired
    private PortalColumnMapper columnMapper;

    @Autowired
    private PortalColumnArticleMapper columnArticleMapper;

    @Autowired(required = false)
    private IFeedService feedService;

    @Autowired
    private ISysNotificationService notificationService;

    @Autowired
    private PortalUserMapper portalUserMapper;

    @Autowired
    @org.springframework.context.annotation.Lazy
    private com.moyun.system.service.IAuditTaskService auditTaskService;

    @Override
    public Page<ColumnListItemVO> selectColumnPage(Page<ColumnListItemVO> page, ColumnQuery query) {
        return columnMapper.selectAdminListPage(page, query);
    }

    @Override
    public PortalColumn selectColumnById(Long id) {
        return columnMapper.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertColumn(PortalColumn column) {
        if (column.getStatus() == null || column.getStatus().isEmpty()) {
            column.setStatus("draft");
        }
        if (column.getArticleCount() == null) {
            column.setArticleCount(0);
        }
        if (column.getSubscribeCount() == null) {
            column.setSubscribeCount(0);
        }
        if (column.getViewCount() == null) {
            column.setViewCount(0);
        }
        if (column.getIsFinished() == null) {
            column.setIsFinished(0);
        }
        if (column.getPrice() == null) {
            column.setPrice(java.math.BigDecimal.ZERO);
        }
        int rows = columnMapper.insert(column);
        // v8.1：专栏进入待审核态时，提交统一审核任务（写 sys_audit_task），使首页/审核中心待办可见
        if (rows > 0 && "pending".equals(column.getStatus()) && column.getId() != null) {
            submitColumnAuditTask(column);
        }
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateColumn(PortalColumn column) {
        PortalColumn existing = columnMapper.selectById(column.getId());
        if (existing == null) {
            throw new ServiceException("专栏不存在");
        }
        int rows = columnMapper.updateById(column);
        // v8.1：编辑后若被强制转为 pending，重新提交审核任务
        if (rows > 0 && "pending".equals(column.getStatus()) && column.getId() != null) {
            submitColumnAuditTask(column);
        }
        return rows;
    }

    @Override
    public int updateColumnStatus(Long id, String status) {
        // 状态白名单校验，防止非法值
        if (!"draft".equals(status) && !"pending".equals(status) && !"published".equals(status)
                && !"archived".equals(status) && !"rejected".equals(status)) {
            throw new ServiceException("状态非法，仅支持 draft / pending / published / archived / rejected");
        }
        PortalColumn existing = columnMapper.selectById(id);
        if (existing == null) {
            throw new ServiceException("专栏不存在");
        }
        LambdaUpdateWrapper<PortalColumn> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(PortalColumn::getId, id)
                .set(PortalColumn::getStatus, status)
                .set(PortalColumn::getUpdatedTime, LocalDateTime.now());
        return columnMapper.update(null, wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditColumn(Long id, String status, String auditRemark, Long auditorId) {
        if (!"published".equals(status) && !"rejected".equals(status)) {
            throw new ServiceException("审核状态仅支持 published=通过 / rejected=驳回");
        }
        PortalColumn existing = columnMapper.selectById(id);
        if (existing == null) {
            throw new ServiceException("专栏不存在");
        }
        // 乐观锁：仅 draft 或 pending 状态可审核
        if (!"draft".equals(existing.getStatus()) && !"pending".equals(existing.getStatus())) {
            throw new ServiceException("仅待审核（draft/pending）状态的专栏可审核，当前状态：" + existing.getStatus());
        }

        LambdaUpdateWrapper<PortalColumn> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(PortalColumn::getId, id)
                .in(PortalColumn::getStatus, "draft", "pending") // 乐观锁
                .set(PortalColumn::getStatus, status)
                .set(PortalColumn::getAuditorId, auditorId)
                .set(PortalColumn::getAuditTime, LocalDateTime.now())
                .set(PortalColumn::getUpdatedTime, LocalDateTime.now());
        if (auditRemark != null && !auditRemark.isEmpty()) {
            wrapper.set(PortalColumn::getAuditRemark, auditRemark);
        }
        int rows = columnMapper.update(null, wrapper);
        if (rows == 0) {
            throw new ServiceException("审核失败：专栏状态已变更，请刷新后重试");
        }

        // 审核通过：推送 Feed（new_column），与 saveColumn 新建时的 Feed 推送对齐
        if ("published".equals(status)) {
            if (feedService != null) {
                try {
                    feedService.publishEvent(existing.getUserId(), "new_column", "column", existing.getId(),
                            existing.getTitle(), existing.getDescription(), existing.getCover());
                } catch (Exception e) {
                    log.warn("专栏审核通过 Feed 事件发布失败: columnId={}, err={}", id, e.getMessage());
                }
            }
        }

        // 站内信通知作者（非阻塞）
        sendColumnAuditNotification(existing, status, auditRemark);

        // v8.1：同步统一审核任务状态 + 关闭审核员待办（非阻塞，不影响审核主流程）
        try {
            String taskFinalStatus = "published".equals(status) ? "approved" : "rejected";
            Long auditorIdValue = SecurityUtils.getUserId();
            String auditorName = SecurityUtils.getUsername();
            auditTaskService.syncTaskStatusByBiz("column", existing.getId(), taskFinalStatus, auditorIdValue, auditorName, auditRemark);
            // 关闭下发给审核员的待办通知
            notificationService.completeTodoByBizData("column", existing.getId());
        } catch (Exception e) {
            log.warn("同步专栏审核任务状态失败（不影响审核主流程）：columnId={}, err={}", id, e.getMessage());
        }
    }

    /**
     * v8.1：提交专栏统一审核任务（事务内，异常回滚保证双写一致）。
     */
    private void submitColumnAuditTask(PortalColumn column) {
        AuditTaskSubmitDTO dto = new AuditTaskSubmitDTO();
        dto.setTaskType("column");
        dto.setBizId(column.getId());
        dto.setTitle(column.getTitle());
        dto.setSubmitterId(SecurityUtils.getUserId());
        dto.setSubmitterName(SecurityUtils.getUsername());
        // 补充专栏作者昵称用于展示（查询失败不影响审核任务提交）
        if (column.getUserId() != null) {
            try {
                PortalUser author = portalUserMapper.selectById(column.getUserId());
                if (author != null) {
                    dto.setSubmitterName(author.getNickname() != null ? author.getNickname() : author.getUsername());
                }
            } catch (Exception e) {
                log.warn("查询专栏作者信息失败（不影响审核任务提交）：userId={}, err={}", column.getUserId(), e.getMessage());
            }
        }
        auditTaskService.submit(dto);
    }

    /**
     * 站内信通知专栏作者审核结果
     */
    private void sendColumnAuditNotification(PortalColumn column, String status, String auditRemark) {
        if (column.getUserId() == null) {
            return;
        }
        try {
            SysNotification notification = new SysNotification();
            notification.setType("system");
            notification.setScope("user");
            notification.setUserId(column.getUserId());
            notification.setUserType("portal");
            notification.setNoticeType("1");
            notification.setStatus("0");
            boolean passed = "published".equals(status);
            notification.setTitle(passed ? "专栏审核通过：" + column.getTitle() : "专栏审核未通过：" + column.getTitle());
            StringBuilder content = new StringBuilder();
            content.append("您提交的专栏「").append(column.getTitle()).append("」");
            if (passed) {
                content.append("已审核通过，现已公开发布。");
            } else {
                content.append("审核未通过。");
                if (auditRemark != null && !auditRemark.isEmpty()) {
                    content.append("驳回原因：").append(auditRemark);
                }
            }
            notification.setContent(content.toString());
            notification.setData("{\"bizType\":\"column\",\"id\":" + column.getId() + "}");
            notificationService.insertNotification(notification);
        } catch (Exception e) {
            log.warn("专栏审核通知发送失败: columnId={}, err={}", column.getId(), e.getMessage());
        }
    }


    @Override
    public int deleteColumnByIds(Long[] ids) {
        return columnMapper.deleteBatchIds(Arrays.asList(ids));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult batchBindArticles(Long columnId, List<Long> articleIds) {
        if (columnId == null) {
            throw new ServiceException("专栏ID不能为空");
        }
        if (articleIds == null || articleIds.isEmpty()) {
            return AjaxResult.error("专栏ID不能为空");
        }
        PortalColumn column = columnMapper.selectById(columnId);
        if (column == null) {
            throw new ServiceException("专栏不存在");
        }
        // 查询该专栏已绑定的文章ID，过滤重复
        LambdaQueryWrapper<PortalColumnArticle> existWrapper = new LambdaQueryWrapper<>();
        existWrapper.eq(PortalColumnArticle::getColumnId, columnId)
                .in(PortalColumnArticle::getArticleId, articleIds);
        List<PortalColumnArticle> existList = columnArticleMapper.selectList(existWrapper);
        java.util.Set<Long> existIds = new java.util.HashSet<>();
        for (PortalColumnArticle ca : existList) {
            existIds.add(ca.getArticleId());
        }
        // 计算当前最大 sort_order，新增的往后追加
        LambdaQueryWrapper<PortalColumnArticle> orderWrapper = new LambdaQueryWrapper<>();
        orderWrapper.eq(PortalColumnArticle::getColumnId, columnId)
                .orderByDesc(PortalColumnArticle::getSortOrder)
                .last("LIMIT 1");
        PortalColumnArticle lastOne = columnArticleMapper.selectOne(orderWrapper);
        int startOrder = (lastOne == null || lastOne.getSortOrder() == null) ? 0 : lastOne.getSortOrder() + 1;

        List<PortalColumnArticle> toInsert = new ArrayList<>();
        int idx = 0;

        // 过滤已绑定，并构建新增记录
        for (Long articleId : articleIds) {
            if (articleId == null || existIds.contains(articleId)) {
                continue;
            }
            PortalColumnArticle ca = new PortalColumnArticle();
            ca.setColumnId(columnId);
            ca.setArticleId(articleId);
            ca.setSortOrder(startOrder + idx);
            ca.setCreatedTime(LocalDateTime.now());
            toInsert.add(ca);
            idx++;
        }
        if (toInsert.isEmpty()) {
            return AjaxResult.success("已全部绑定");
        }
        int rows = 0;
        for (PortalColumnArticle ca : toInsert) {
            rows += columnArticleMapper.insert(ca);
        }
        // 同步专栏 article_count（取实际绑定总数，避免偏差）
        int total = columnArticleMapper.countByColumn(columnId);
        LambdaUpdateWrapper<PortalColumn> colWrapper = new LambdaUpdateWrapper<>();
        colWrapper.eq(PortalColumn::getId, columnId)
                .set(PortalColumn::getArticleCount, total)
                .set(PortalColumn::getUpdatedTime, LocalDateTime.now());
        columnMapper.update(null, colWrapper);
        return AjaxResult.success("绑定成功，成功数量：" + rows + ", 已存在：" + existList.size());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int removeColumnArticle(Long columnId, Long articleId) {
        if (columnId == null || articleId == null) {
            throw new ServiceException("专栏ID/文章ID不能为空");
        }
        LambdaQueryWrapper<PortalColumnArticle> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PortalColumnArticle::getColumnId, columnId)
                .eq(PortalColumnArticle::getArticleId, articleId);
        int rows = columnArticleMapper.delete(wrapper);
        if (rows > 0) {
            // 同步专栏 article_count
            int total = columnArticleMapper.countByColumn(columnId);
            LambdaUpdateWrapper<PortalColumn> colWrapper = new LambdaUpdateWrapper<>();
            colWrapper.eq(PortalColumn::getId, columnId)
                    .set(PortalColumn::getArticleCount, total)
                    .set(PortalColumn::getUpdatedTime, LocalDateTime.now());
            columnMapper.update(null, colWrapper);
        }
        return rows;
    }

    @Override
    public Page<ArticleSimpleVO> selectColumnArticlesPage(Page<ArticleSimpleVO> page, Long columnId, String keyword) {
        if (columnId == null) {
            throw new ServiceException("专栏ID不能为空");
        }
        return columnArticleMapper.selectColumnArticlesPage(page, columnId, keyword);
    }
}
