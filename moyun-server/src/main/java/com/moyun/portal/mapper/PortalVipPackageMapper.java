package com.moyun.portal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.portal.domain.entity.PortalVipPackage;
import com.moyun.portal.domain.query.VipPackageQuery;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 门户VIP套餐表 数据层
 *
 * @author moyun
 */
@Mapper
public interface PortalVipPackageMapper extends BaseMapper<PortalVipPackage> {

    /**
     * 根据条件分页查询VIP套餐列表
     *
     * @param page 分页参数
     * @param query 查询条件
     * @return VIP套餐信息集合信息
     */
    Page<PortalVipPackage> selectPortalVipPackagePage(Page<PortalVipPackage> page, @Param("params") VipPackageQuery query);

    /**
     * 根据条件查询VIP套餐列表（不分页，用于导出等场景）
     *
     * @param query 查询条件
     * @return VIP套餐信息集合信息
     */
    List<PortalVipPackage> selectPortalVipPackageList(@Param("params") VipPackageQuery query);

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
