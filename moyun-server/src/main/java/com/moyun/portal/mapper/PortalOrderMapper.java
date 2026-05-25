package com.moyun.portal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyun.portal.domain.entity.PortalOrder;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 门户订单表 数据层
 *
 * @author moyun
 */
@Mapper
public interface PortalOrderMapper extends BaseMapper<PortalOrder> {

    /**
     * 根据条件分页查询订单列表
     *
     * @param portalOrder 订单信息
     * @return 订单信息集合信息
     */
    public List<PortalOrder> selectPortalOrderList(PortalOrder portalOrder);

    /**
     * 通过订单ID查询订单
     *
     * @param id 订单ID
     * @return 订单对象信息
     */
    public PortalOrder selectPortalOrderById(Long id);

    /**
     * 通过订单号查询订单
     *
     * @param orderNo 订单号
     * @return 订单对象信息
     */
    public PortalOrder selectPortalOrderByOrderNo(String orderNo);

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
