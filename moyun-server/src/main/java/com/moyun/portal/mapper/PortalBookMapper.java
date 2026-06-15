package com.moyun.portal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.portal.domain.entity.PortalBook;
import com.moyun.portal.domain.query.BookQuery;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 书籍表 数据层
 *
 * @author moyun
 */
@Mapper
public interface PortalBookMapper extends BaseMapper<PortalBook>
{
    /**
     * 分页查询书籍列表
     */
    Page<PortalBook> selectPortalBookPage(Page<PortalBook> page, @Param("query") BookQuery query);

    /**
     * 根据条件查询书籍列表
     */
    List<PortalBook> selectPortalBookList(@Param("query") BookQuery query);

    /**
     * 通过书籍ID查询书籍
     */
    PortalBook selectPortalBookById(@Param("id") Long id);

    /**
     * 新增书籍
     */
    int insertPortalBook(PortalBook portalBook);

    /**
     * 修改书籍
     */
    int updatePortalBook(PortalBook portalBook);

    /**
     * 通过书籍ID删除书籍
     */
    int deletePortalBookById(@Param("id") Long id);

    /**
     * 批量删除书籍
     */
    int deletePortalBookByIds(@Param("ids") Long[] ids);
}
