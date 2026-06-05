package com.moyun.portal.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.moyun.portal.domain.entity.PortalOrder;
import com.moyun.portal.domain.query.OrderQuery;
import com.moyun.portal.mapper.PortalOrderMapper;
import com.moyun.portal.service.IPortalOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 门户订单 业务层处理
 *
 * @author moyun
 */
@Service
public class PortalOrderServiceImpl extends ServiceImpl<PortalOrderMapper, PortalOrder> implements IPortalOrderService {

    @Autowired
    private PortalOrderMapper portalOrderMapper;

    /**
     * 根据条件分页查询订单列表
     *
     * @param page 分页参数
     * @param query 查询条件
     * @return 分页结果
     */
    @Override
    public Page<PortalOrder> selectPortalOrderPage(Page<PortalOrder> page, OrderQuery query) {
        return baseMapper.selectPortalOrderPage(page, query);
    }

    /**
     * 根据条件查询订单列表（不分页，用于导出等场景）
     *
     * @param query 查询条件
     * @return 订单信息集合
     */
    @Override
    public List<PortalOrder> selectPortalOrderList(OrderQuery query) {
        return baseMapper.selectPortalOrderList(query);
    }

    /**
     * 通过订单ID查询订单
     *
     * @param id 订单ID
     * @return 订单对象信息
     */
    @Override
    public PortalOrder selectPortalOrderById(Long id) {
        return portalOrderMapper.selectPortalOrderById(id);
    }

    /**
     * 新增订单信息
     *
     * @param portalOrder 订单信息
     * @return 结果
     */
    @Override
    public int insertPortalOrder(PortalOrder portalOrder) {
        return portalOrderMapper.insertPortalOrder(portalOrder);
    }

    /**
     * 修改订单信息
     *
     * @param portalOrder 订单信息
     * @return 结果
     */
    @Override
    public int updatePortalOrder(PortalOrder portalOrder) {
        return portalOrderMapper.updatePortalOrder(portalOrder);
    }

    /**
     * 通过订单ID删除订单
     *
     * @param id 订单ID
     * @return 结果
     */
    @Override
    public int deletePortalOrderById(Long id) {
        return portalOrderMapper.deletePortalOrderById(id);
    }

    /**
     * 批量删除订单信息
     *
     * @param ids 需要删除的订单ID
     * @return 结果
     */
    @Override
    public int deletePortalOrderByIds(Long[] ids) {
        return portalOrderMapper.deletePortalOrderByIds(ids);
    }
}
