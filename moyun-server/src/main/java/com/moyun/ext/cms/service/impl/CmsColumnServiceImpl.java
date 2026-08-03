package com.moyun.ext.cms.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.common.exception.system.ServiceException;
import com.moyun.ext.cms.domain.query.ColumnQuery;
import com.moyun.ext.cms.domain.vo.ColumnListItemVO;
import com.moyun.ext.cms.service.ICmsColumnService;
import com.moyun.ext.cms.service.IFeedService;
import com.moyun.portal.domain.entity.PortalColumn;
import com.moyun.portal.mapper.PortalColumnMapper;
import com.moyun.system.domain.entity.SysNotification;
import com.moyun.system.service.ISysNotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;

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

    @Autowired(required = false)
    private IFeedService feedService;

    @Autowired
    private ISysNotificationService notificationService;

    @Override
    public Page<ColumnListItemVO> selectColumnPage(Page<ColumnListItemVO> page, ColumnQuery query) {
        return columnMapper.selectAdminListPage(page, query);
    }

    @Override
    public PortalColumn selectColumnById(Long id) {
        return columnMapper.selectById(id);
    }

    @Override
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
        return columnMapper.insert(column);
    }

    @Override
    public int updateColumn(PortalColumn column) {
        PortalColumn existing = columnMapper.selectById(column.getId());
        if (existing == null) {
            throw new ServiceException("专栏不存在");
        }
        return columnMapper.updateById(column);
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
}
