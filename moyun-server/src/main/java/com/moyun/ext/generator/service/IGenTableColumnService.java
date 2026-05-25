package com.moyun.ext.generator.service;

import com.moyun.ext.generator.domain.entity.GenTableColumn;

import java.util.List;

/**
 * 代码生成 服务层
 *
 * @author ruoyi
 */
public interface IGenTableColumnService {

    List<GenTableColumn> selectGenTableColumnListByTableId(Long tableId);

    GenTableColumn selectGenTableColumnById(Long id);

    int deleteGenTableColumnByIds(Long[] ids);

    void updateGenTableColumn(GenTableColumn genTableColumn);

    void insertGenTableColumn(GenTableColumn genTableColumn);

    void updateGenTableColumnByTable(GenTableColumn[] genTableColumns);
}
