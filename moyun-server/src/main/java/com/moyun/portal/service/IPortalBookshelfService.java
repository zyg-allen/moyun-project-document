package com.moyun.portal.service;

import java.util.List;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import com.moyun.portal.domain.entity.PortalBookshelf;
import com.moyun.portal.domain.query.BookshelfQuery;

/**
 * 用户书架 业务层接口
 *
 * @author moyun
 */
public interface IPortalBookshelfService extends IService<PortalBookshelf> {

    Page<PortalBookshelf> selectBookshelfPage(Page<PortalBookshelf> page, BookshelfQuery query);

    List<PortalBookshelf> selectBookshelfList(BookshelfQuery query);

    PortalBookshelf selectBookshelfById(Long id);

    PortalBookshelf selectByUserAndBook(Long userId, Long bookId);

    int insertBookshelf(PortalBookshelf bookshelf);

    int updateBookshelf(PortalBookshelf bookshelf);

    int deleteBookshelfById(Long id);

    int deleteBookshelfByIds(Long[] ids);

    /**
     * 加入书架（toggle 语义，已收藏则取消）
     * @return true=已收藏，false=已取消
     */
    boolean toggleBookshelf(Long userId, Long bookId);

    /**
     * 更新书架项的最后阅读章节（阅读时同步）
     */
    int updateLastChapter(Long userId, Long bookId, Long lastChapterId, Integer lastChapterNo);
}
