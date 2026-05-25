package com.moyun.ext.generator.service;

import com.moyun.ext.generator.domain.entity.GenTable;

import java.util.List;

/**
 * 代码生成 服务层
 *
 * @author ruoyi
 */
public interface IGenTableService {

    GenTable selectGenTableById(Long id);

    GenTable selectGenTableByName(String tableName);

    List<GenTable> selectGenTableList(GenTable genTable);

    List<GenTable> selectGenTableListByIds(Long[] ids);

    int deleteGenTableByIds(Long[] ids);

    void deleteGenTable(Long tableId);

    void updateGenTable(GenTable genTable);

    void insertGenTable(GenTable genTable);

    List<String> selectDatatableList();

    void importGenTable(List<GenTable> tableList);

    void editSave(GenTable genTable);
}
