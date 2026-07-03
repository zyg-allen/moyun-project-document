package com.moyun.portal.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.moyun.portal.domain.entity.PortalCreatorCertification;

/**
 * 创作者认证 业务层
 *
 * <p>提供认证申请、状态查询、审核与后台分页查询能力。</p>
 *
 * @author moyun
 */
public interface IPortalCreatorCertificationService extends IService<PortalCreatorCertification> {

    /**
     * 提交认证申请（同用户已有 pending 申请时拒绝重复提交）
     *
     * @param userId 当前登录用户ID
     * @param dto    认证申请表单
     * @return 新建的认证申请记录（含 id）
     */
    PortalCreatorCertification apply(Long userId, PortalCreatorCertification dto);

    /**
     * 我的认证状态（返回最近一条申请记录，无则返回 null）
     *
     * @param userId 当前登录用户ID
     */
    PortalCreatorCertification getMy(Long userId);

    /**
     * 审核（管理员）
     *
     * @param id         认证申请ID
     * @param auditorId  审核人ID
     * @param status     审核结果：approved / rejected
     * @param remark     审核备注
     * @return 更新后的认证申请记录
     */
    PortalCreatorCertification audit(Long id, Long auditorId, String status, String remark);

    /**
     * 后台分页查询
     *
     * @param query 查询条件（可含 status、certType、userId 等）
     * @param page  分页参数
     */
    Page<PortalCreatorCertification> list(PortalCreatorCertification query, Page<PortalCreatorCertification> page);
}
