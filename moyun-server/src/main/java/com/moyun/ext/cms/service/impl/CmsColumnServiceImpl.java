package com.moyun.ext.cms.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.common.exception.system.ServiceException;
import com.moyun.ext.cms.domain.query.ColumnQuery;
import com.moyun.ext.cms.domain.vo.ColumnListItemVO;
import com.moyun.ext.cms.service.ICmsColumnService;
import com.moyun.portal.domain.entity.PortalColumn;
import com.moyun.portal.mapper.PortalColumnMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;

/**
 * CMS 专栏后台管理 Service 实现
 *
 * <p>复用 {@link PortalColumnMapper}，列表查询走 selectAdminListPage（含作者信息），
 * 其余走 BaseMapper 通用方法。</p>
 *
 * @author moyun
 */
@Service
public class CmsColumnServiceImpl implements ICmsColumnService {

    @Autowired
    private PortalColumnMapper columnMapper;

    @Override
    public Page<ColumnListItemVO> selectColumnPage(Page<ColumnListItemVO> page, ColumnQuery query) {
        return columnMapper.selectAdminListPage(page, query);
    }

    @Override
    public PortalColumn selectColumnById(Long id) {
        return columnMapper.selectById(id);
    }

    @Override
    public int insertColumn(PortalColumn column) {
        if (column.getStatus() == null || column.getStatus().isEmpty()) {
            column.setStatus("draft");
        }
        if (column.getArticleCount() == null) {
            column.setArticleCount(0);
        }
        if (column.getSubscribeCount() == null) {
            column.setSubscribeCount(0);
        }
        if (column.getViewCount() == null) {
            column.setViewCount(0);
        }
        if (column.getIsFinished() == null) {
            column.setIsFinished(0);
        }
        if (column.getPrice() == null) {
            column.setPrice(java.math.BigDecimal.ZERO);
        }
        return columnMapper.insert(column);
    }

    @Override
    public int updateColumn(PortalColumn column) {
        PortalColumn existing = columnMapper.selectById(column.getId());
        if (existing == null) {
            throw new ServiceException("专栏不存在");
        }
        return columnMapper.updateById(column);
    }

    @Override
    public int updateColumnStatus(Long id, String status) {
        PortalColumn existing = columnMapper.selectById(id);
        if (existing == null) {
            throw new ServiceException("专栏不存在");
        }
        LambdaUpdateWrapper<PortalColumn> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(PortalColumn::getId, id)
                .set(PortalColumn::getStatus, status);
        return columnMapper.update(null, wrapper);
    }

    @Override
    public int deleteColumnByIds(Long[] ids) {
        return columnMapper.deleteBatchIds(Arrays.asList(ids));
    }
}
