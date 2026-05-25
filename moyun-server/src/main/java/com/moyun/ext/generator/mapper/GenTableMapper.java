package com.moyun.ext.generator.mapper;

import com.moyun.ext.generator.domain.entity.GenTable;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 代码生成Mapper接口
 *
 * @author ruoyi
 */
@Mapper
public interface GenTableMapper {

    List<GenTable> selectGenTableList(GenTable genTable);

    GenTable selectGenTableById(Long id);

    GenTable selectGenTableByName(String tableName);

    List<GenTable> selectGenTableListByIds(Long[] ids);

    int deleteGenTableByIds(Long[] ids);

    int deleteGenTableById(Long id);

    int updateGenTable(GenTable genTable);

    int insertGenTable(GenTable genTable);

    List<String> selectDatatableList();
}
