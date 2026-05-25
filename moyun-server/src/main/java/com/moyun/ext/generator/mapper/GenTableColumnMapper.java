package com.moyun.ext.generator.mapper;

import com.moyun.ext.generator.domain.entity.GenTableColumn;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 代码生成业务字段Mapper接口
 *
 * @author ruoyi
 */
@Mapper
public interface GenTableColumnMapper {

    List<GenTableColumn> selectGenTableColumnList(GenTableColumn genTableColumn);

    List<GenTableColumn> selectGenTableColumnListByTableId(Long tableId);

    GenTableColumn selectGenTableColumnById(Long id);

    int deleteGenTableColumnByIds(Long[] ids);

    int deleteGenTableColumnById(Long id);

    int updateGenTableColumn(GenTableColumn genTableColumn);

    int insertGenTableColumn(GenTableColumn genTableColumn);

    int batchInsert(@Param("list") List<GenTableColumn> list);
}
