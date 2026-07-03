package com.moyun.ext.cms.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.ext.cms.domain.query.ColumnQuery;
import com.moyun.ext.cms.domain.vo.ColumnListItemVO;
import com.moyun.portal.domain.entity.PortalColumn;

/**
 * CMS 专栏后台管理 Service 接口
 *
 * <p>提供专栏列表/详情/创建/更新/删除/审核（状态流转 draft→published→archived）。</p>
 *
 * @author moyun
 */
public interface ICmsColumnService {

    /**
     * 后台分页查询专栏（含所有状态、作者信息）
     */
    Page<ColumnListItemVO> selectColumnPage(Page<ColumnListItemVO> page, ColumnQuery query);

    /**
     * 根据ID获取专栏详情
     */
    PortalColumn selectColumnById(Long id);

    /**
     * 新增专栏
     */
    int insertColumn(PortalColumn column);

    /**
     * 修改专栏
     */
    int updateColumn(PortalColumn column);

    /**
     * 更新专栏状态（审核/状态流转：draft→published→archived）
     */
    int updateColumnStatus(Long id, String status);

    /**
     * 批量删除专栏
     */
    int deleteColumnByIds(Long[] ids);
}
