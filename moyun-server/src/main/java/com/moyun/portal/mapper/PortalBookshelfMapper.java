package com.moyun.portal.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.moyun.portal.domain.entity.PortalBookshelf;
import com.moyun.portal.domain.query.BookshelfQuery;

/**
 * 用户书架 数据层
 *
 * @author moyun
 */
@Mapper
public interface PortalBookshelfMapper extends BaseMapper<PortalBookshelf> {

    /**
     * 分页查询书架（含书籍信息，关联 portal_book）
     */
    Page<PortalBookshelf> selectBookshelfPage(Page<PortalBookshelf> page, @Param("query") BookshelfQuery query);

    /**
     * 列表查询书架（含书籍信息）
     */
    List<PortalBookshelf> selectBookshelfList(@Param("query") BookshelfQuery query);

    /**
     * 按主键查询（含书籍信息）
     */
    PortalBookshelf selectBookshelfById(@Param("id") Long id);

    /**
     * 按 user_id + book_id 查询（判断是否已收藏）
     */
    PortalBookshelf selectByUserAndBook(@Param("userId") Long userId, @Param("bookId") Long bookId);

    int insertBookshelf(PortalBookshelf bookshelf);

    int updateBookshelf(PortalBookshelf bookshelf);

    int deleteBookshelfById(@Param("id") Long id);

    int deleteBookshelfByIds(@Param("ids") Long[] ids);

    /**
     * 按 user_id + book_id 删除（用户取消收藏）
     */
    int deleteByUserAndBook(@Param("userId") Long userId, @Param("bookId") Long bookId);

    /**
     * 更新书架项的最后阅读章节（续读恢复用）
     */
    int updateLastChapter(@Param("userId") Long userId, @Param("bookId") Long bookId,
                          @Param("lastChapterId") Long lastChapterId, @Param("lastChapterNo") Integer lastChapterNo);
}
