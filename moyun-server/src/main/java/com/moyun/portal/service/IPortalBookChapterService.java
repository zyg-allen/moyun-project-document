package com.moyun.portal.service;

import java.util.List;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import com.moyun.portal.domain.entity.PortalBookChapter;
import com.moyun.portal.domain.query.BookChapterQuery;

/**
 * 书籍章节 业务层接口
 *
 * @author moyun
 */
public interface IPortalBookChapterService extends IService<PortalBookChapter> {

    /**
     * 分页查询章节列表
     */
    Page<PortalBookChapter> selectChapterPage(Page<PortalBookChapter> page, BookChapterQuery query);

    /**
     * 查询章节列表（不分页）
     */
    List<PortalBookChapter> selectChapterList(BookChapterQuery query);

    /**
     * 通过章节ID查询章节详情
     */
    PortalBookChapter selectChapterById(Long id);

    /**
     * 新增章节（自动维护 chapter_no）
     */
    int insertChapter(PortalBookChapter chapter);

    /**
     * 修改章节
     */
    int updateChapter(PortalBookChapter chapter);

    /**
     * 通过章节ID删除章节
     */
    int deleteChapterById(Long id);

    /**
     * 批量删除章节
     */
    int deleteChapterByIds(Long[] ids);

    /**
     * 发布章节（同步更新书籍冗余字段）
     */
    int publishChapter(Long id);

    /**
     * 撤回发布（同步更新书籍冗余字段）
     */
    int unpublishChapter(Long id);

    /**
     * 批量导入章节（事务保障，同步更新书籍冗余字段）
     *
     * @param bookId       所属书籍ID
     * @param chapters     章节列表
     * @param autoPublish  是否自动发布
     */
    int batchImportChapters(Long bookId, List<PortalBookChapter> chapters, Boolean autoPublish);

    /**
     * 查询上一章（已发布）
     */
    PortalBookChapter selectPrevChapter(Long bookId, Integer chapterNo);

    /**
     * 查询下一章（已发布）
     */
    PortalBookChapter selectNextChapter(Long bookId, Integer chapterNo);

    /**
     * 浏览量 +1
     */
    int incrementViewCount(Long id);

    /**
     * 重新统计书籍的字数/章节数/最新章节（数据修复用）
     */
    void recountBookStats(Long bookId);
}
