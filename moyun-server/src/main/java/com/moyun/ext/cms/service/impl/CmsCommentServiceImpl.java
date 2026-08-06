package com.moyun.ext.cms.service.impl;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moyun.common.exception.system.ServiceException;
import com.moyun.ext.cms.domain.query.CmsCommentQuery;
import com.moyun.ext.cms.domain.vo.CmsCommentVO;
import com.moyun.ext.cms.service.ICmsCommentService;
import com.moyun.portal.domain.entity.PortalArticle;
import com.moyun.portal.domain.entity.PortalComment;
import com.moyun.portal.mapper.PortalArticleMapper;
import com.moyun.portal.mapper.PortalCommentMapper;
import com.moyun.util.security.SecurityUtils;

/**
 * CMS评论服务实现类
 *
 * @author moyun
 */
@Slf4j
@Service
public class CmsCommentServiceImpl implements ICmsCommentService
{
    @Autowired
    private PortalCommentMapper portalCommentMapper;

    @Autowired
    private PortalArticleMapper portalArticleMapper;

    @Override
    public Page<CmsCommentVO> selectCommentPage(Page<CmsCommentVO> page, CmsCommentQuery query)
    {
        Page<PortalComment> entityPage = new Page<>(page.getCurrent(), page.getSize());
        entityPage = portalCommentMapper.selectPage(entityPage, buildQueryWrapper(query));

        Page<CmsCommentVO> voPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        List<CmsCommentVO> voList = BeanUtil.copyToList(entityPage.getRecords(), CmsCommentVO.class);
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    public List<PortalComment> selectCommentList(CmsCommentQuery query)
    {
        return portalCommentMapper.selectList(buildQueryWrapper(query));
    }

    @Override
    public PortalComment selectCommentById(Long id)
    {
        return portalCommentMapper.selectById(id);
    }

    /**
     * 审核评论（对齐文章审核模式）
     * <p>状态白名单：仅允许审核为 "1"(已发布) / "2"(驳回)
     * 乐观锁：仅 status="0"(待审核) 可被审核，避免重复审核已发布评论
     * 写入：auditor_id / audit_time / audit_remark
     * 文章评论计数同步：仅一级评论（parent_id=0）会影响文章 comments 计数
     *   - 驳回（0->2）：若原已计入评论数则扣减；当前实现 0 状态评论未计入，故无需扣减
     *   - 通过（0->1）：一级评论计入文章评论数（与 PortalCommentServiceImpl.insertPortalComment 行为对齐）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int auditComment(PortalComment comment)
    {
        if (comment == null || comment.getId() == null) {
            return 0;
        }
        String newStatus = comment.getStatus();
        // 状态白名单：仅允许审核为已发布 / 驳回
        if (!"1".equals(newStatus) && !"2".equals(newStatus)) {
            throw new ServiceException("评论审核状态仅支持 1(已发布) / 2(驳回)");
        }
        // 仅待审核（status=0）的评论可被审核，防止重复审核
        PortalComment existing = portalCommentMapper.selectById(comment.getId());
        if (existing == null) {
            throw new ServiceException("评论不存在");
        }
        if (!"0".equals(existing.getStatus())) {
            throw new ServiceException("仅待审核（status=0）状态的评论可审核，当前状态：" + existing.getStatus());
        }

        // 乐观锁：仅 status=0 可被审核
        LambdaUpdateWrapper<PortalComment> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(PortalComment::getId, comment.getId())
                .eq(PortalComment::getStatus, "0")
                .set(PortalComment::getStatus, newStatus)
                .set(PortalComment::getAuditorId, SecurityUtils.getUserId())
                .set(PortalComment::getAuditTime, LocalDateTime.now());
        // 审核意见
        String auditRemark = comment.getAuditRemark() != null ? comment.getAuditRemark() : comment.getRemark();
        if (auditRemark != null && !auditRemark.isEmpty()) {
            wrapper.set(PortalComment::getAuditRemark, auditRemark);
        }
        int rows = portalCommentMapper.update(null, wrapper);
        if (rows == 0) {
            throw new ServiceException("审核失败：评论状态已变更，请刷新后重试");
        }

        // 审核通过且为一级评论：计入文章评论数（对齐 insertPortalComment 中"一级评论 +1"逻辑）
        if ("1".equals(newStatus) && existing.getArticleId() != null
                && (existing.getParentId() == null || existing.getParentId() == 0)) {
            portalArticleMapper.incrementComments(existing.getArticleId(), 1);
        }
        log.info("评论审核完成：commentId={}, newStatus={}, auditorId={}",
                comment.getId(), newStatus, SecurityUtils.getUserId());
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteCommentByIds(Long[] ids)
    {
        if (ids == null || ids.length == 0) {
            return 0;
        }
        // 先批量查询待删除评论（删除后逻辑删除字段过滤导致 selectById 拿不到）
        // 同步扣减文章评论数：仅一级评论（parent_id=0）+ 已发布（status=1）此前计入文章评论数
        List<PortalComment> toDelete = portalCommentMapper.selectList(
                new LambdaQueryWrapper<PortalComment>().in(PortalComment::getId, Arrays.asList(ids))
        );
        int rows = portalCommentMapper.deleteBatchIds(Arrays.asList(ids));
        if (rows > 0) {
            for (PortalComment comment : toDelete) {
                if (comment.getArticleId() == null) continue;
                // 仅一级评论影响文章评论数
                if (comment.getParentId() != null && comment.getParentId() != 0) continue;
                // 仅已发布评论此前计入文章评论数
                if (!"1".equals(comment.getStatus())) continue;
                PortalArticle article = portalArticleMapper.selectById(comment.getArticleId());
                if (article != null && article.getComments() != null && article.getComments() > 0) {
                    portalArticleMapper.incrementComments(comment.getArticleId(), -1);
                }
            }
        }
        return rows;
    }

    /**
     * 构建查询条件
     */
    private LambdaQueryWrapper<PortalComment> buildQueryWrapper(CmsCommentQuery query)
    {
        LambdaQueryWrapper<PortalComment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ObjectUtil.isNotNull(query.getArticleId()), PortalComment::getArticleId, query.getArticleId());
        wrapper.eq(ObjectUtil.isNotNull(query.getAuthorId()), PortalComment::getAuthorId, query.getAuthorId());
        wrapper.eq(ObjectUtil.isNotEmpty(query.getStatus()), PortalComment::getStatus, query.getStatus());
        wrapper.orderByDesc(PortalComment::getCreateTime);
        return wrapper;
    }
}
