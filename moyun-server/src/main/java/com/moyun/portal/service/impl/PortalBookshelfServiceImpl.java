package com.moyun.portal.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.moyun.portal.domain.entity.PortalBookshelf;
import com.moyun.portal.domain.query.BookshelfQuery;
import com.moyun.portal.mapper.PortalBookshelfMapper;
import com.moyun.portal.service.IPortalBookshelfService;

/**
 * 用户书架 业务层实现
 *
 * @author moyun
 */
@Service
public class PortalBookshelfServiceImpl
        extends ServiceImpl<PortalBookshelfMapper, PortalBookshelf>
        implements IPortalBookshelfService {

    @Autowired
    private PortalBookshelfMapper bookshelfMapper;

    @Override
    public Page<PortalBookshelf> selectBookshelfPage(Page<PortalBookshelf> page, BookshelfQuery query) {
        return bookshelfMapper.selectBookshelfPage(page, query);
    }

    @Override
    public List<PortalBookshelf> selectBookshelfList(BookshelfQuery query) {
        return bookshelfMapper.selectBookshelfList(query);
    }

    @Override
    public PortalBookshelf selectBookshelfById(Long id) {
        return bookshelfMapper.selectBookshelfById(id);
    }

    @Override
    public PortalBookshelf selectByUserAndBook(Long userId, Long bookId) {
        return bookshelfMapper.selectByUserAndBook(userId, bookId);
    }

    @Override
    public int insertBookshelf(PortalBookshelf bookshelf) {
        if (bookshelf.getCreateTime() == null) {
            bookshelf.setCreateTime(LocalDateTime.now());
        }
        if (bookshelf.getSort() == null) {
            bookshelf.setSort(0);
        }
        return bookshelfMapper.insertBookshelf(bookshelf);
    }

    @Override
    public int updateBookshelf(PortalBookshelf bookshelf) {
        bookshelf.setUpdateTime(LocalDateTime.now());
        return bookshelfMapper.updateBookshelf(bookshelf);
    }

    @Override
    public int deleteBookshelfById(Long id) {
        return bookshelfMapper.deleteBookshelfById(id);
    }

    @Override
    public int deleteBookshelfByIds(Long[] ids) {
        return bookshelfMapper.deleteBookshelfByIds(ids);
    }

    @Override
    public boolean toggleBookshelf(Long userId, Long bookId) {
        PortalBookshelf existing = bookshelfMapper.selectByUserAndBook(userId, bookId);
        if (existing != null) {
            // 已收藏，取消收藏（delete 天然幂等）
            bookshelfMapper.deleteByUserAndBook(userId, bookId);
            return false;
        }
        // 未收藏，加入书架
        PortalBookshelf item = new PortalBookshelf();
        item.setUserId(userId);
        item.setBookId(bookId);
        item.setSort(0);
        item.setLastChapterNo(0);
        item.setCreateTime(LocalDateTime.now());
        try {
            bookshelfMapper.insertBookshelf(item);
            return true;
        } catch (org.springframework.dao.DuplicateKeyException e) {
            // 并发下另一事务已插入（UK user_id+book_id 冲突），回退为 delete
            // 符合 toggle 语义：二次调用应取消收藏
            bookshelfMapper.deleteByUserAndBook(userId, bookId);
            return false;
        }
    }

    @Override
    public int updateLastChapter(Long userId, Long bookId, Long lastChapterId, Integer lastChapterNo) {
        return bookshelfMapper.updateLastChapter(userId, bookId, lastChapterId, lastChapterNo);
    }
}
