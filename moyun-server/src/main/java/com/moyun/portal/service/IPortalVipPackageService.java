package com.moyun.portal.service;

import java.util.List;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.moyun.portal.domain.entity.PortalVipPackage;
import com.moyun.portal.domain.query.VipPackageQuery;

/**
 * 门户VIP套餐 业务层
 *
 * @author moyun
 */
public interface IPortalVipPackageService {

    /**
     * 根据条件分页查询VIP套餐列表
     *
     * @param page 分页参数
     * @param query 查询条件
     * @return 分页结果
     */
    Page<PortalVipPackage> selectPortalVipPackagePage(Page<PortalVipPackage> page, VipPackageQuery query);

    /**
     * 根据条件查询VIP套餐列表（不分页，用于导出等场景）
     *
     * @param query 查询条件
     * @return VIP套餐信息集合
     */
    List<PortalVipPackage> selectPortalVipPackageList(VipPackageQuery query);

    /**
     * 通过VIP套餐ID查询VIP套餐
     *
     * @param id VIP套餐ID
     * @return VIP套餐对象信息
     */
    public PortalVipPackage selectPortalVipPackageById(Long id);

    /**
     * 新增VIP套餐信息
     *
     * @param portalVipPackage VIP套餐信息
     * @return 结果
     */
    public int insertPortalVipPackage(PortalVipPackage portalVipPackage);

    /**
     * 修改VIP套餐信息
     *
     * @param portalVipPackage VIP套餐信息
     * @return 结果
     */
    public int updatePortalVipPackage(PortalVipPackage portalVipPackage);

    /**
     * 通过VIP套餐ID删除VIP套餐
     *
     * @param id VIP套餐ID
     * @return 结果
     */
    public int deletePortalVipPackageById(Long id);

    /**
     * 批量删除VIP套餐信息
     *
     * @param ids 需要删除的VIP套餐ID
     * @return 结果
     */
    public int deletePortalVipPackageByIds(Long[] ids);
}
