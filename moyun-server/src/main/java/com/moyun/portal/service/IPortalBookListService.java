package com.moyun.portal.service;

import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import com.moyun.portal.domain.entity.PortalBook;
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

    /**
     * 查询书单内书籍列表（带书籍详情）
     * 用于后台"管理书籍"弹窗展示
     */
    List<Map<String, Object>> selectBookListItemsWithDetail(Long bookListId);

    /**
     * 批量添加书籍到书单
     * @param bookListId 书单ID
     * @param bookIds    书籍ID列表
     * @param note       添加说明（可选，应用到所有书）
     * @return 新增条数
     */
    int addBooksToBookList(Long bookListId, List<Long> bookIds, String note);

    /**
     * 批量从书单移除书籍
     * @param bookListId 书单ID
     * @param bookIds    书籍ID列表
     * @return 移除条数
     */
    int removeBooksFromBookList(Long bookListId, List<Long> bookIds);

    /**
     * 批量更新书单内书籍排序
     * @param bookListId 书单ID（用于校验归属）
     * @param sortItems  排序项列表，每项含 {id, sort}
     */
    int updateBookListSort(Long bookListId, List<PortalBookListItem> sortItems);

    /**
     * 查询可添加到书单的书籍（排除已在书单内的）
     * 支持书名/作者/分类筛选
     */
    List<PortalBook> selectAvailableBooks(Long bookListId, String title, String author, Long categoryId);

    /**
     * 切换书单点赞（toggle，已点赞则取消，未点赞则新增）
     * @return 包含 liked(Boolean) 与 likeCount(Long)
     */
    Map<String, Object> toggleBookListLike(Long bookListId, Long userId);

    /**
     * 查询当前用户是否已点赞该书单
     */
    boolean isBookListLiked(Long bookListId, Long userId);

    /**
     * 新增单本书单项（原子更新书单 bookCount +1）
     * @param bookListId 书单ID
     * @param bookId     书籍ID
     * @param sortOrder  排序（可为空）
     * @param remark     添加说明（可为空）
     * @return 新增条数（0 表示已存在或失败）
     */
    int addBookListItem(Long bookListId, Long bookId, Integer sortOrder, String remark);

    /**
     * 修改书单项（按 item 主键）
     */
    int updateBookListItem(Long id, Integer sortOrder, String remark);

    /**
     * 删除书单项（按 item 主键，原子更新书单 bookCount -1）
     */
    int deleteBookListItem(Long id);

    /**
     * 分页查询可添加到书单的书籍（排除已在书单内的）
     */
    Page<PortalBook> selectAvailableBooksPage(Page<PortalBook> page, Long bookListId, String title, String author, Long categoryId);
}
