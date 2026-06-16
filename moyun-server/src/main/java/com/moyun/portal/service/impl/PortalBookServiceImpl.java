package com.moyun.portal.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.moyun.portal.domain.entity.PortalBook;
import com.moyun.portal.domain.query.BookQuery;
import com.moyun.portal.mapper.PortalBookMapper;
import com.moyun.portal.service.IPortalBookService;

/**
 * 书籍 业务层实现
 *
 * @author moyun
 */
@Service
public class PortalBookServiceImpl extends ServiceImpl<PortalBookMapper, PortalBook> implements IPortalBookService {

    @Autowired
    private PortalBookMapper portalBookMapper;

    @Override
    public Page<PortalBook> selectPortalBookPage(Page<PortalBook> page, BookQuery query) {
        return portalBookMapper.selectPortalBookPage(page, query);
    }

    @Override
    public List<PortalBook> selectPortalBookList(BookQuery query) {
        return portalBookMapper.selectPortalBookList(query);
    }

    @Override
    public PortalBook selectPortalBookById(Long id) {
        return portalBookMapper.selectPortalBookById(id);
    }

    @Override
    public int insertPortalBook(PortalBook portalBook) {
        if (portalBook.getCreateTime() == null) {
            portalBook.setCreateTime(LocalDateTime.now());
        }
        if (portalBook.getStatus() == null || portalBook.getStatus().isEmpty()) {
            portalBook.setStatus("active");
        }
        if (portalBook.getAccessLevel() == null || portalBook.getAccessLevel().isEmpty()) {
            portalBook.setAccessLevel("free");
        }
        if (portalBook.getReadingCount() == null) {
            portalBook.setReadingCount(0L);
        }
        return portalBookMapper.insertPortalBook(portalBook);
    }

    @Override
    public int updatePortalBook(PortalBook portalBook) {
        portalBook.setUpdateTime(LocalDateTime.now());
        return portalBookMapper.updatePortalBook(portalBook);
    }

    @Override
    public int deletePortalBookById(Long id) {
        return portalBookMapper.deletePortalBookById(id);
    }

    @Override
    public int deletePortalBookByIds(Long[] ids) {
        return portalBookMapper.deletePortalBookByIds(ids);
    }

    @Override
    public void incrementReadingCount(Long id) {
        PortalBook book = portalBookMapper.selectPortalBookById(id);
        if (book != null) {
            book.setReadingCount((book.getReadingCount() == null ? 0L : book.getReadingCount()) + 1L);
            portalBookMapper.updatePortalBook(book);
        }
    }

    @Override
    public List<PortalBook> selectFeaturedBooks(int limit) {
        BookQuery query = new BookQuery();
        query.setIsFeatured(true);
        query.setStatus("active");
        Page<PortalBook> page = new Page<>(1, limit);
        Page<PortalBook> result = portalBookMapper.selectPortalBookPage(page, query);
        return result.getRecords();
    }

    @Override
    public List<PortalBook> selectBooksByCategory(Long categoryId, int limit) {
        BookQuery query = new BookQuery();
        query.setCategoryId(categoryId);
        query.setStatus("active");
        Page<PortalBook> page = new Page<>(1, limit);
        Page<PortalBook> result = portalBookMapper.selectPortalBookPage(page, query);
        return result.getRecords();
    }

    @Override
    public Page<PortalBook> searchBooks(String keyword, int pageNum, int pageSize) {
        BookQuery query = new BookQuery();
        query.setTitle(keyword);
        query.setStatus("active");
        Page<PortalBook> page = new Page<>(pageNum, pageSize);
        return portalBookMapper.selectPortalBookPage(page, query);
    }
}
