package com.moyun.portal.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.moyun.portal.domain.entity.PortalBookList;
import com.moyun.portal.domain.query.BookListQuery;

/**
 * 书单表 数据层
 *
 * @author moyun
 */
@Mapper
public interface PortalBookListMapper extends BaseMapper<PortalBookList>
{
    Page<PortalBookList> selectPortalBookListPage(Page<PortalBookList> page, @Param("query") BookListQuery query);

    List<PortalBookList> selectPortalBookList(@Param("query") BookListQuery query);

    PortalBookList selectPortalBookListById(@Param("id") Long id);

    int insertPortalBookList(PortalBookList portalBookList);

    int updatePortalBookList(PortalBookList portalBookList);

    int deletePortalBookListById(@Param("id") Long id);

    int deletePortalBookListByIds(@Param("ids") Long[] ids);
}
