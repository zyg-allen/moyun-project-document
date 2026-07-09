package com.moyun.portal.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moyun.portal.domain.entity.PortalBook;
import com.moyun.portal.domain.entity.PortalBookList;
import com.moyun.portal.domain.entity.PortalBookListItem;
import com.moyun.portal.domain.entity.PortalBookListLike;
import com.moyun.portal.domain.query.BookListQuery;
import com.moyun.portal.mapper.PortalBookListItemMapper;
import com.moyun.portal.mapper.PortalBookListLikeMapper;
import com.moyun.portal.mapper.PortalBookListMapper;
import com.moyun.portal.service.IPortalBookListService;
import com.moyun.portal.service.IPortalGrowthService;
import com.moyun.ext.cms.service.IFeedService;

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

    @Autowired
    private PortalBookListLikeMapper portalBookListLikeMapper;

    @Autowired
    private IPortalGrowthService portalGrowthService;

    @Autowired
    private IFeedService feedService;

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
        int rows = portalBookListMapper.insertPortalBookList(portalBookList);

        // 记录创建书单成长事件
        if (rows > 0 && portalBookList.getUserId() != null) {
            portalGrowthService.recordEvent("reading", "create_booklist",
                    portalBookList.getUserId(), "booklist", portalBookList.getId());

            // 发布动态事件（Feed 流）
            try {
                feedService.publishEvent(portalBookList.getUserId(), "create_booklist", "booklist",
                        portalBookList.getId(), portalBookList.getTitle(),
                        portalBookList.getDescription(), portalBookList.getCover());
            } catch (Exception e) {
                org.slf4j.LoggerFactory.getLogger(PortalBookListServiceImpl.class)
                        .error("[Feed] 书单创建动态事件失败：booklistId={}", portalBookList.getId(), e);
            }
        }

        return rows;
    }

    @Override
    public int updatePortalBookList(PortalBookList portalBookList) {
        portalBookList.setUpdateTime(LocalDateTime.now());
        return portalBookListMapper.updatePortalBookList(portalBookList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deletePortalBookListById(Long id) {
        // 级联删除关联记录，避免孤儿数据
        portalBookListItemMapper.deleteByBookListId(id);
        return portalBookListMapper.deletePortalBookListById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deletePortalBookListByIds(Long[] ids) {
        if (ids != null && ids.length > 0) {
            for (Long id : ids) {
                portalBookListItemMapper.deleteByBookListId(id);
            }
        }
        return portalBookListMapper.deletePortalBookListByIds(ids);
    }

    @Override
    public List<PortalBookListItem> selectBookListItems(Long bookListId) {
        return portalBookListItemMapper.selectByBookListId(bookListId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int addBookToBookList(Long bookListId, Long bookId, Integer sort, String note) {
        // 排除重复：同一本书不能重复加入同一书单
        List<Long> existIds = portalBookListItemMapper.selectExistBookIds(bookListId);
        if (existIds != null && existIds.contains(bookId)) {
            return 0;
        }
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
        // 使用数据库原子更新，避免并发竞态条件
        portalBookListMapper.incrementViewCount(id);
    }

    @Override
    public void incrementLikeCount(Long id) {
        // 使用数据库原子更新，避免并发竞态条件
        int affected = portalBookListMapper.incrementLikeCount(id);
        if (affected > 0) {
            // 为书单创建者记录被赞成长事件
            PortalBookList bl = portalBookListMapper.selectPortalBookListById(id);
            if (bl != null && bl.getUserId() != null) {
                portalGrowthService.recordEvent("reading", "booklist_liked",
                        bl.getUserId(), "booklist", bl.getId());
            }
        }
    }

    // ===== 第三阶段补齐：书单-书籍关系维护 =====

    @Override
    public List<Map<String, Object>> selectBookListItemsWithDetail(Long bookListId) {
        return portalBookListItemMapper.selectItemsWithBookDetail(bookListId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int addBooksToBookList(Long bookListId, List<Long> bookIds, String note) {
        if (bookIds == null || bookIds.isEmpty()) {
            return 0;
        }
        // 排除已在书单内的 bookId，避免重复插入
        List<Long> existIds = portalBookListItemMapper.selectExistBookIds(bookListId);
        List<PortalBookListItem> toInsert = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (Long bookId : bookIds) {
            if (bookId == null || existIds.contains(bookId)) {
                continue;
            }
            PortalBookListItem item = new PortalBookListItem();
            item.setBookListId(bookListId);
            item.setBookId(bookId);
            item.setSort(0);
            item.setNote(note);
            item.setCreateTime(now);
            toInsert.add(item);
        }
        if (toInsert.isEmpty()) {
            return 0;
        }
        int rows = portalBookListItemMapper.batchInsertItems(toInsert);
        // 同步 bookCount
        if (rows > 0) {
            PortalBookList bl = portalBookListMapper.selectPortalBookListById(bookListId);
            if (bl != null) {
                int newCount = (bl.getBookCount() == null ? 0 : bl.getBookCount()) + toInsert.size();
                bl.setBookCount(newCount);
                portalBookListMapper.updatePortalBookList(bl);
            }
        }
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int removeBooksFromBookList(Long bookListId, List<Long> bookIds) {
        if (bookIds == null || bookIds.isEmpty()) {
            return 0;
        }
        // 批量删除，避免循环逐条操作
        int rows = portalBookListItemMapper.batchDeleteByBookListIdAndBookIds(bookListId, bookIds);
        // 同步 bookCount
        if (rows > 0) {
            PortalBookList bl = portalBookListMapper.selectPortalBookListById(bookListId);
            if (bl != null && bl.getBookCount() != null) {
                int newCount = Math.max(0, bl.getBookCount() - rows);
                bl.setBookCount(newCount);
                portalBookListMapper.updatePortalBookList(bl);
            }
        }
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateBookListSort(Long bookListId, List<PortalBookListItem> sortItems) {
        if (sortItems == null || sortItems.isEmpty()) {
            return 0;
        }
        int rows = 0;
        for (PortalBookListItem item : sortItems) {
            if (item.getId() == null || item.getSort() == null) {
                continue;
            }
            // 传入 bookListId 进行归属校验，防止越权修改其他书单的排序
            rows += portalBookListItemMapper.updateSort(item.getId(), bookListId, item.getSort());
        }
        return rows;
    }

    @Override
    public List<PortalBook> selectAvailableBooks(Long bookListId, String title, String author, Long categoryId) {
        return portalBookListItemMapper.selectAvailableBooks(bookListId, title, author, categoryId);
    }

    // ===== 点赞功能 =====

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> toggleBookListLike(Long bookListId, Long userId) {
        PortalBookListLike existing = portalBookListLikeMapper.selectOne(
                new LambdaQueryWrapper<PortalBookListLike>()
                        .eq(PortalBookListLike::getBookListId, bookListId)
                        .eq(PortalBookListLike::getUserId, userId));
        Map<String, Object> result = new HashMap<>();
        if (existing != null) {
            // 已点赞 → 取消点赞
            portalBookListLikeMapper.deleteById(existing.getId());
            portalBookListMapper.decrementLikeCount(bookListId);
            result.put("liked", false);
        } else {
            // 未点赞 → 新增点赞（并发兜底：唯一键冲突视为已点赞）
            PortalBookListLike like = new PortalBookListLike();
            like.setBookListId(bookListId);
            like.setUserId(userId);
            like.setCreateTime(LocalDateTime.now());
            try {
                portalBookListLikeMapper.insert(like);
                portalBookListMapper.incrementLikeCount(bookListId);
                result.put("liked", true);
            } catch (DuplicateKeyException e) {
                // 并发下另一事务已点赞，视为已点赞（不再重复计数）
                result.put("liked", true);
            }
        }
        // 返回最新点赞数
        PortalBookList bl = portalBookListMapper.selectPortalBookListById(bookListId);
        result.put("likeCount", bl != null && bl.getLikeCount() != null ? bl.getLikeCount() : 0L);
        return result;
    }

    @Override
    public boolean isBookListLiked(Long bookListId, Long userId) {
        Long count = portalBookListLikeMapper.selectCount(
                new LambdaQueryWrapper<PortalBookListLike>()
                        .eq(PortalBookListLike::getBookListId, bookListId)
                        .eq(PortalBookListLike::getUserId, userId));
        return count != null && count > 0;
    }

    // ===== 书单项单本 CRUD（管理后台）=====

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int addBookListItem(Long bookListId, Long bookId, Integer sortOrder, String remark) {
        if (bookId == null) {
            return 0;
        }
        // 排除重复：同一本书不能重复加入同一书单
        List<Long> existIds = portalBookListItemMapper.selectExistBookIds(bookListId);
        if (existIds != null && existIds.contains(bookId)) {
            return 0;
        }
        PortalBookListItem item = new PortalBookListItem();
        item.setBookListId(bookListId);
        item.setBookId(bookId);
        item.setSort(sortOrder != null ? sortOrder : 0);
        item.setNote(remark);
        item.setCreateTime(LocalDateTime.now());
        int result = portalBookListItemMapper.insertItem(item);
        if (result > 0) {
            // 原子更新书单书籍数量 +1
            portalBookListMapper.incrementBookCount(bookListId);
        }
        return result;
    }

    @Override
    public int updateBookListItem(Long id, Integer sortOrder, String remark) {
        PortalBookListItem item = new PortalBookListItem();
        item.setId(id);
        if (sortOrder != null) {
            item.setSort(sortOrder);
        }
        if (remark != null) {
            item.setNote(remark);
        }
        return portalBookListItemMapper.updateById(item);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteBookListItem(Long id) {
        PortalBookListItem item = portalBookListItemMapper.selectById(id);
        if (item == null) {
            return 0;
        }
        int result = portalBookListItemMapper.deleteById(id);
        if (result > 0 && item.getBookListId() != null) {
            // 原子更新书单书籍数量 -1
            portalBookListMapper.decrementBookCount(item.getBookListId());
        }
        return result;
    }

    @Override
    public Page<PortalBook> selectAvailableBooksPage(Page<PortalBook> page, Long bookListId, String title, String author, Long categoryId) {
        return portalBookListItemMapper.selectAvailableBooksPage(page, bookListId, title, author, categoryId);
    }
}
