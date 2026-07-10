package com.moyun.portal.service.impl;

import java.util.List;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.moyun.portal.domain.entity.PortalAdSlot;
import com.moyun.portal.domain.query.AdSlotQuery;
import com.moyun.portal.mapper.PortalAdSlotMapper;
import com.moyun.portal.service.IPortalAdSlotService;

/**
 * 门户广告位 业务层处理
 *
 * @author moyun
 */
@Service
public class PortalAdSlotServiceImpl extends ServiceImpl<PortalAdSlotMapper, PortalAdSlot> implements IPortalAdSlotService {

    @Autowired
    private PortalAdSlotMapper portalAdSlotMapper;

    /**
     * 根据条件分页查询广告位列表
     *
     * @param page 分页参数
     * @param query 查询条件
     * @return 分页结果
     */
    @Override
    public Page<PortalAdSlot> selectPortalAdSlotPage(Page<PortalAdSlot> page, AdSlotQuery query) {
        return baseMapper.selectPortalAdSlotPage(page, query);
    }

    /**
     * 根据条件查询广告位列表（不分页，用于导出等场景）
     *
     * @param query 查询条件
     * @return 广告位信息集合
     */
    @Override
    public List<PortalAdSlot> selectPortalAdSlotList(AdSlotQuery query) {
        return baseMapper.selectPortalAdSlotList(query);
    }

    /**
     * 通过广告位ID查询广告位
     *
     * @param id 广告位ID
     * @return 广告位对象信息
     */
    @Override
    public PortalAdSlot selectPortalAdSlotById(Long id) {
        return portalAdSlotMapper.selectPortalAdSlotById(id);
    }

    /**
     * 新增广告位信息
     *
     * @param portalAdSlot 广告位信息
     * @return 结果
     */
    @Override
    public int insertPortalAdSlot(PortalAdSlot portalAdSlot) {
        return portalAdSlotMapper.insertPortalAdSlot(portalAdSlot);
    }

    /**
     * 修改广告位信息
     *
     * @param portalAdSlot 广告位信息
     * @return 结果
     */
    @Override
    public int updatePortalAdSlot(PortalAdSlot portalAdSlot) {
        return portalAdSlotMapper.updatePortalAdSlot(portalAdSlot);
    }

    /**
     * 通过广告位ID删除广告位
     *
     * @param id 广告位ID
     * @return 结果
     */
    @Override
    public int deletePortalAdSlotById(Long id) {
        return portalAdSlotMapper.deletePortalAdSlotById(id);
    }

    /**
     * 批量删除广告位信息
     *
     * @param ids 需要删除的广告位ID
     * @return 结果
     */
    @Override
    public int deletePortalAdSlotByIds(Long[] ids) {
        return portalAdSlotMapper.deletePortalAdSlotByIds(ids);
    }
}
