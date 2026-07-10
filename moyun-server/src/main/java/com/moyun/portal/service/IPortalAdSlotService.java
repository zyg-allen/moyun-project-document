package com.moyun.portal.service;

import java.util.List;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.moyun.portal.domain.entity.PortalAdSlot;
import com.moyun.portal.domain.query.AdSlotQuery;

/**
 * 门户广告位 业务层
 *
 * @author moyun
 */
public interface IPortalAdSlotService {

    /**
     * 根据条件分页查询广告位列表
     *
     * @param page 分页参数
     * @param query 查询条件
     * @return 分页结果
     */
    Page<PortalAdSlot> selectPortalAdSlotPage(Page<PortalAdSlot> page, AdSlotQuery query);

    /**
     * 根据条件查询广告位列表（不分页，用于导出等场景）
     *
     * @param query 查询条件
     * @return 广告位信息集合
     */
    List<PortalAdSlot> selectPortalAdSlotList(AdSlotQuery query);

    /**
     * 通过广告位ID查询广告位
     *
     * @param id 广告位ID
     * @return 广告位对象信息
     */
    public PortalAdSlot selectPortalAdSlotById(Long id);

    /**
     * 新增广告位信息
     *
     * @param portalAdSlot 广告位信息
     * @return 结果
     */
    public int insertPortalAdSlot(PortalAdSlot portalAdSlot);

    /**
     * 修改广告位信息
     *
     * @param portalAdSlot 广告位信息
     * @return 结果
     */
    public int updatePortalAdSlot(PortalAdSlot portalAdSlot);

    /**
     * 通过广告位ID删除广告位
     *
     * @param id 广告位ID
     * @return 结果
     */
    public int deletePortalAdSlotById(Long id);

    /**
     * 批量删除广告位信息
     *
     * @param ids 需要删除的广告位ID
     * @return 结果
     */
    public int deletePortalAdSlotByIds(Long[] ids);
}
