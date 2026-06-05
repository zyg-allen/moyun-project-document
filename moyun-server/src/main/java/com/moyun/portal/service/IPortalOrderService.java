package com.moyun.portal.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.portal.domain.entity.PortalOrder;
import com.moyun.portal.domain.query.OrderQuery;

import java.util.List;

/**
 * 门户订单 业务层
 *
 * @author moyun
 */
public interface IPortalOrderService {

    /**
     * 根据条件分页查询订单列表
     *
     * @param page 分页参数
     * @param query 查询条件
     * @return 分页结果
     */
    Page<PortalOrder> selectPortalOrderPage(Page<PortalOrder> page, OrderQuery query);

    /**
     * 根据条件查询订单列表（不分页，用于导出等场景）
     *
     * @param query 查询条件
     * @return 订单信息集合
     */
    List<PortalOrder> selectPortalOrderList(OrderQuery query);

    /**
     * 通过订单ID查询订单
     *
     * @param id 订单ID
     * @return 订单对象信息
     */
    public PortalOrder selectPortalOrderById(Long id);

    /**
     * 新增订单信息
     *
     * @param portalOrder 订单信息
     * @return 结果
     */
    public int insertPortalOrder(PortalOrder portalOrder);

    /**
     * 修改订单信息
     *
     * @param portalOrder 订单信息
     * @return 结果
     */
    public int updatePortalOrder(PortalOrder portalOrder);

    /**
     * 通过订单ID删除订单
     *
     * @param id 订单ID
     * @return 结果
     */
    public int deletePortalOrderById(Long id);

    /**
     * 批量删除订单信息
     *
     * @param ids 需要删除的订单ID
     * @return 结果
     */
    public int deletePortalOrderByIds(Long[] ids);
}
