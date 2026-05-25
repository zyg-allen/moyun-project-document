package com.moyun.ext.generator.service.impl;

import com.moyun.ext.generator.domain.entity.GenTableColumn;
import com.moyun.ext.generator.mapper.GenTableColumnMapper;
import com.moyun.ext.generator.service.IGenTableColumnService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * 代码生成业务字段 服务实现
 *
 * @author ruoyi
 */
@Service
public class GenTableColumnServiceImpl implements IGenTableColumnService {

    @Autowired
    private GenTableColumnMapper genTableColumnMapper;

    @Override
    public List<GenTableColumn> selectGenTableColumnListByTableId(Long tableId) {
        return genTableColumnMapper.selectGenTableColumnListByTableId(tableId);
    }

    @Override
    public GenTableColumn selectGenTableColumnById(Long id) {
        return genTableColumnMapper.selectGenTableColumnById(id);
    }

    @Override
    public int deleteGenTableColumnByIds(Long[] ids) {
        return genTableColumnMapper.deleteGenTableColumnByIds(ids);
    }

    @Override
    public void updateGenTableColumn(GenTableColumn genTableColumn) {
        genTableColumnMapper.updateGenTableColumn(genTableColumn);
    }

    @Override
    public void insertGenTableColumn(GenTableColumn genTableColumn) {
        genTableColumnMapper.insertGenTableColumn(genTableColumn);
    }

    @Override
    public void updateGenTableColumnByTable(GenTableColumn[] genTableColumns) {
        Arrays.stream(genTableColumns).forEach(column -> {
            updateGenTableColumn(column);
        });
    }
}
