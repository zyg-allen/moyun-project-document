package com.moyun.portal.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.moyun.portal.domain.entity.PortalBook;
import com.moyun.portal.domain.query.BookQuery;

import java.util.List;

/**
 * 书籍 业务层接口
 *
 * @author moyun
 */
public interface IPortalBookService extends IService<PortalBook> {

    /**
     * 根据条件分页查询书籍列表
     */
    Page<PortalBook> selectPortalBookPage(Page<PortalBook> page, BookQuery query);

    /**
     * 根据条件查询书籍列表（不分页）
     */
    List<PortalBook> selectPortalBookList(BookQuery query);

    /**
     * 通过书籍ID查询书籍
     */
    PortalBook selectPortalBookById(Long id);

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
    int deletePortalBookById(Long id);

    /**
     * 批量删除书籍
     */
    int deletePortalBookByIds(Long[] ids);

    /**
     * 增加阅读数
     */
    void incrementReadingCount(Long id);

    /**
     * 查询精选书籍
     */
    List<PortalBook> selectFeaturedBooks(int limit);

    /**
     * 按分类查询热门书籍
     */
    List<PortalBook> selectBooksByCategory(Long categoryId, int limit);

    /**
     * 搜索书籍（按标题/作者/ISBN模糊匹配）
     */
    Page<PortalBook> searchBooks(String keyword, int pageNum, int pageSize);
}
