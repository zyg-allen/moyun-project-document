package com.moyun.ext.generator.service.impl;

import com.moyun.ext.generator.domain.entity.GenTable;
import com.moyun.ext.generator.mapper.GenTableMapper;
import com.moyun.ext.generator.service.IGenTableService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * 代码生成 服务实现
 *
 * @author ruoyi
 */
@Service
public class GenTableServiceImpl implements IGenTableService {

    @Autowired
    private GenTableMapper genTableMapper;

    @Override
    public GenTable selectGenTableById(Long id) {
        return genTableMapper.selectGenTableById(id);
    }

    @Override
    public GenTable selectGenTableByName(String tableName) {
        return genTableMapper.selectGenTableByName(tableName);
    }

    @Override
    public List<GenTable> selectGenTableList(GenTable genTable) {
        return genTableMapper.selectGenTableList(genTable);
    }

    @Override
    public List<GenTable> selectGenTableListByIds(Long[] ids) {
        return genTableMapper.selectGenTableListByIds(ids);
    }

    @Override
    public int deleteGenTableByIds(Long[] ids) {
        return genTableMapper.deleteGenTableByIds(ids);
    }

    @Override
    public void deleteGenTable(Long tableId) {
        genTableMapper.deleteGenTableById(tableId);
    }

    @Override
    public void updateGenTable(GenTable genTable) {
        genTableMapper.updateGenTable(genTable);
    }

    @Override
    public void insertGenTable(GenTable genTable) {
        genTableMapper.insertGenTable(genTable);
    }

    @Override
    public List<String> selectDatatableList() {
        return genTableMapper.selectDatatableList();
    }

    @Override
    public void importGenTable(List<GenTable> tableList) {
    }

    @Override
    public void editSave(GenTable genTable) {
    }
}
