package com.moyun.portal.service.impl;

import java.util.List;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.moyun.portal.domain.entity.PortalVipPackage;
import com.moyun.portal.domain.query.VipPackageQuery;
import com.moyun.portal.mapper.PortalVipPackageMapper;
import com.moyun.portal.service.IPortalVipPackageService;

/**
 * 门户VIP套餐 业务层处理
 *
 * @author moyun
 */
@Service
public class PortalVipPackageServiceImpl extends ServiceImpl<PortalVipPackageMapper, PortalVipPackage> implements IPortalVipPackageService {

    @Autowired
    private PortalVipPackageMapper portalVipPackageMapper;

    /**
     * 根据条件分页查询VIP套餐列表
     *
     * @param page 分页参数
     * @param query 查询条件
     * @return 分页结果
     */
    @Override
    public Page<PortalVipPackage> selectPortalVipPackagePage(Page<PortalVipPackage> page, VipPackageQuery query) {
        return baseMapper.selectPortalVipPackagePage(page, query);
    }

    /**
     * 根据条件查询VIP套餐列表（不分页，用于导出等场景）
     *
     * @param query 查询条件
     * @return VIP套餐信息集合
     */
    @Override
    public List<PortalVipPackage> selectPortalVipPackageList(VipPackageQuery query) {
        return baseMapper.selectPortalVipPackageList(query);
    }

    /**
     * 通过VIP套餐ID查询VIP套餐
     *
     * @param id VIP套餐ID
     * @return VIP套餐对象信息
     */
    @Override
    public PortalVipPackage selectPortalVipPackageById(Long id) {
        return portalVipPackageMapper.selectPortalVipPackageById(id);
    }

    /**
     * 新增VIP套餐信息
     *
     * @param portalVipPackage VIP套餐信息
     * @return 结果
     */
    @Override
    public int insertPortalVipPackage(PortalVipPackage portalVipPackage) {
        return portalVipPackageMapper.insertPortalVipPackage(portalVipPackage);
    }

    /**
     * 修改VIP套餐信息
     *
     * @param portalVipPackage VIP套餐信息
     * @return 结果
     */
    @Override
    public int updatePortalVipPackage(PortalVipPackage portalVipPackage) {
        return portalVipPackageMapper.updatePortalVipPackage(portalVipPackage);
    }

    /**
     * 通过VIP套餐ID删除VIP套餐
     *
     * @param id VIP套餐ID
     * @return 结果
     */
    @Override
    public int deletePortalVipPackageById(Long id) {
        return portalVipPackageMapper.deletePortalVipPackageById(id);
    }

    /**
     * 批量删除VIP套餐信息
     *
     * @param ids 需要删除的VIP套餐ID
     * @return 结果
     */
    @Override
    public int deletePortalVipPackageByIds(Long[] ids) {
        return portalVipPackageMapper.deletePortalVipPackageByIds(ids);
    }
}
