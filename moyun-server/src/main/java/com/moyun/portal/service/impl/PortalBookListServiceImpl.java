package com.moyun.portal.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.moyun.portal.domain.entity.PortalBookList;
import com.moyun.portal.domain.entity.PortalBookListItem;
import com.moyun.portal.domain.query.BookListQuery;
import com.moyun.portal.mapper.PortalBookListMapper;
import com.moyun.portal.mapper.PortalBookListItemMapper;
import com.moyun.portal.service.IPortalBookListService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 书单 业务层实现
 *
 * @author moyun
 */
@Service
public class PortalBookListServiceImpl extends ServiceImpl<PortalBookListMapper, PortalBookList> implements IPortalBookListService {

    @Autowired
    private PortalBookListMapper portalBookListMapper;

    @Autowired
    private PortalBookListItemMapper portalBookListItemMapper;

    @Override
    public Page<PortalBookList> selectPortalBookListPage(Page<PortalBookList> page, BookListQuery query) {
        return portalBookListMapper.selectPortalBookListPage(page, query);
    }

    @Override
    public List<PortalBookList> selectPortalBookList(BookListQuery query) {
        return portalBookListMapper.selectPortalBookList(query);
    }

    @Override
    public PortalBookList selectPortalBookListById(Long id) {
        return portalBookListMapper.selectPortalBookListById(id);
    }

    @Override
    public int insertPortalBookList(PortalBookList portalBookList) {
        if (portalBookList.getCreateTime() == null) {
            portalBookList.setCreateTime(LocalDateTime.now());
        }
        if (portalBookList.getStatus() == null || portalBookList.getStatus().isEmpty()) {
            portalBookList.setStatus("active");
        }
        if (portalBookList.getIsPublic() == null) {
            portalBookList.setIsPublic(true);
        }
        if (portalBookList.getBookCount() == null) {
            portalBookList.setBookCount(0);
        }
        if (portalBookList.getViewCount() == null) {
            portalBookList.setViewCount(0L);
        }
        if (portalBookList.getLikeCount() == null) {
            portalBookList.setLikeCount(0L);
        }
        return portalBookListMapper.insertPortalBookList(portalBookList);
    }

    @Override
    public int updatePortalBookList(PortalBookList portalBookList) {
        portalBookList.setUpdateTime(LocalDateTime.now());
        return portalBookListMapper.updatePortalBookList(portalBookList);
    }

    @Override
    public int deletePortalBookListById(Long id) {
        return portalBookListMapper.deletePortalBookListById(id);
    }

    @Override
    public int deletePortalBookListByIds(Long[] ids) {
        return portalBookListMapper.deletePortalBookListByIds(ids);
    }

    @Override
    public List<PortalBookListItem> selectBookListItems(Long bookListId) {
        return portalBookListItemMapper.selectByBookListId(bookListId);
    }

    @Override
    public int addBookToBookList(Long bookListId, Long bookId, Integer sort, String note) {
        PortalBookListItem item = new PortalBookListItem();
        item.setBookListId(bookListId);
        item.setBookId(bookId);
        item.setSort(sort != null ? sort : 0);
        item.setNote(note);
        item.setCreateTime(LocalDateTime.now());
        int result = portalBookListItemMapper.insertItem(item);
        if (result > 0) {
            // 更新书单书籍数量
            PortalBookList bl = portalBookListMapper.selectPortalBookListById(bookListId);
            if (bl != null) {
                bl.setBookCount((bl.getBookCount() == null ? 0 : bl.getBookCount()) + 1);
                portalBookListMapper.updatePortalBookList(bl);
            }
        }
        return result;
    }

    @Override
    public int removeBookFromBookList(Long bookListId, Long bookId) {
        int result = portalBookListItemMapper.deleteByBookListIdAndBookId(bookListId, bookId);
        if (result > 0) {
            PortalBookList bl = portalBookListMapper.selectPortalBookListById(bookListId);
            if (bl != null && bl.getBookCount() != null && bl.getBookCount() > 0) {
                bl.setBookCount(bl.getBookCount() - 1);
                portalBookListMapper.updatePortalBookList(bl);
            }
        }
        return result;
    }

    @Override
    public List<PortalBookList> selectFeaturedBookLists(int limit) {
        BookListQuery query = new BookListQuery();
        query.setIsFeatured(true);
        query.setIsPublic(true);
        query.setStatus("active");
        Page<PortalBookList> page = new Page<>(1, limit);
        Page<PortalBookList> result = portalBookListMapper.selectPortalBookListPage(page, query);
        return result.getRecords();
    }

    @Override
    public void incrementViewCount(Long id) {
        PortalBookList bl = portalBookListMapper.selectPortalBookListById(id);
        if (bl != null) {
            bl.setViewCount((bl.getViewCount() == null ? 0L : bl.getViewCount()) + 1L);
            portalBookListMapper.updatePortalBookList(bl);
        }
    }

    @Override
    public void incrementLikeCount(Long id) {
        PortalBookList bl = portalBookListMapper.selectPortalBookListById(id);
        if (bl != null) {
            bl.setLikeCount((bl.getLikeCount() == null ? 0L : bl.getLikeCount()) + 1L);
            portalBookListMapper.updatePortalBookList(bl);
        }
    }
}
