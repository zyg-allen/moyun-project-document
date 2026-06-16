package com.moyun.portal.service;

import java.util.List;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import com.moyun.portal.domain.entity.PortalBookList;
import com.moyun.portal.domain.entity.PortalBookListItem;
import com.moyun.portal.domain.query.BookListQuery;

/**
 * 书单 业务层接口
 *
 * @author moyun
 */
public interface IPortalBookListService extends IService<PortalBookList> {

    Page<PortalBookList> selectPortalBookListPage(Page<PortalBookList> page, BookListQuery query);

    List<PortalBookList> selectPortalBookList(BookListQuery query);

    PortalBookList selectPortalBookListById(Long id);

    int insertPortalBookList(PortalBookList portalBookList);

    int updatePortalBookList(PortalBookList portalBookList);

    int deletePortalBookListById(Long id);

    int deletePortalBookListByIds(Long[] ids);

    /**
     * 查询书单包含的书籍列表
     */
    List<PortalBookListItem> selectBookListItems(Long bookListId);

    /**
     * 添加书籍到书单
     */
    int addBookToBookList(Long bookListId, Long bookId, Integer sort, String note);

    /**
     * 从书单移除书籍
     */
    int removeBookFromBookList(Long bookListId, Long bookId);

    /**
     * 查询精选书单
     */
    List<PortalBookList> selectFeaturedBookLists(int limit);

    /**
     * 增加阅读数
     */
    void incrementViewCount(Long id);

    /**
     * 点赞书单
     */
    void incrementLikeCount(Long id);
}
