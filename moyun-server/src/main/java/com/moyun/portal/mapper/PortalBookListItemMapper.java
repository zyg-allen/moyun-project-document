package com.moyun.portal.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.moyun.portal.domain.entity.PortalBookListItem;

/**
 * 书单-书籍关联表 数据层
 *
 * @author moyun
 */
@Mapper
public interface PortalBookListItemMapper extends BaseMapper<PortalBookListItem>
{
    List<PortalBookListItem> selectByBookListId(@Param("bookListId") Long bookListId);

    int insertItem(PortalBookListItem item);

    int deleteByBookListIdAndBookId(@Param("bookListId") Long bookListId, @Param("bookId") Long bookId);

    int deleteByBookListId(@Param("bookListId") Long bookListId);
}
