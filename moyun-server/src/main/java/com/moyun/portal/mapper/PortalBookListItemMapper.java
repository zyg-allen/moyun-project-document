package com.moyun.portal.mapper;

import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.moyun.portal.domain.entity.PortalBook;
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

    /**
     * 查询书单内书籍列表（带书籍详情，用于后台"管理书籍"弹窗展示）
     */
    List<Map<String, Object>> selectItemsWithBookDetail(@Param("bookListId") Long bookListId);

    /**
     * 查询书单内已包含的 book_id 列表（用于"可添加书籍"查询时排除）
     */
    List<Long> selectExistBookIds(@Param("bookListId") Long bookListId);

    /**
     * 批量插入书单-书籍关联
     */
    int batchInsertItems(@Param("items") List<PortalBookListItem> items);

    /**
     * 更新单条排序（校验归属：只更新属于该书单的记录）
     */
    int updateSort(@Param("id") Long id, @Param("bookListId") Long bookListId, @Param("sort") Integer sort);

    /**
     * 批量删除书单-书籍关联（按 bookListId + bookIds）
     */
    int batchDeleteByBookListIdAndBookIds(@Param("bookListId") Long bookListId, @Param("bookIds") List<Long> bookIds);

    /**
     * 查询可添加到书单的书籍（排除已在书单内的），支持书名/作者/分类筛选
     */
    List<PortalBook> selectAvailableBooks(@Param("bookListId") Long bookListId,
                                          @Param("title") String title,
                                          @Param("author") String author,
                                          @Param("categoryId") Long categoryId);

    /**
     * 分页查询可添加到书单的书籍（排除已在书单内的），支持书名/作者/分类筛选
     */
    Page<PortalBook> selectAvailableBooksPage(Page<PortalBook> page,
                                              @Param("bookListId") Long bookListId,
                                              @Param("title") String title,
                                              @Param("author") String author,
                                              @Param("categoryId") Long categoryId);
}
