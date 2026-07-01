package com.moyun.portal.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.moyun.portal.domain.entity.PortalBookChapter;
import com.moyun.portal.domain.query.BookChapterQuery;

/**
 * 书籍章节表 数据层
 *
 * @author moyun
 */
@Mapper
public interface PortalBookChapterMapper extends BaseMapper<PortalBookChapter> {

    /**
     * 分页查询章节列表（按 book_id 过滤，chapter_no 排序）
     */
    Page<PortalBookChapter> selectChapterPage(Page<PortalBookChapter> page, @Param("query") BookChapterQuery query);

    /**
     * 根据条件查询章节列表
     */
    List<PortalBookChapter> selectChapterList(@Param("query") BookChapterQuery query);

    /**
     * 通过章节ID查询章节详情
     */
    PortalBookChapter selectChapterById(@Param("id") Long id);

    /**
     * 新增章节
     */
    int insertChapter(PortalBookChapter chapter);

    /**
     * 批量新增章节
     */
    int batchInsertChapter(@Param("list") List<PortalBookChapter> list);

    /**
     * 修改章节
     */
    int updateChapter(PortalBookChapter chapter);

    /**
     * 通过章节ID删除章节
     */
    int deleteChapterById(@Param("id") Long id);

    /**
     * 批量删除章节
     */
    int deleteChapterByIds(@Param("ids") Long[] ids);

    /**
     * 查询书籍的最大章节序号（用于追加导入）
     */
    Integer selectMaxChapterNo(@Param("bookId") Long bookId);

    /**
     * 查询上一章（chapter_no 比当前小的最近一章，已发布）
     */
    PortalBookChapter selectPrevChapter(@Param("bookId") Long bookId, @Param("chapterNo") Integer chapterNo);

    /**
     * 查询下一章（chapter_no 比当前大的最近一章，已发布）
     */
    PortalBookChapter selectNextChapter(@Param("bookId") Long bookId, @Param("chapterNo") Integer chapterNo);

    /**
     * 查询书籍的已发布章节数
     */
    Integer selectPublishedChapterCount(@Param("bookId") Long bookId);

    /**
     * 查询书籍的已发布章节总字数
     */
    Long selectPublishedWordCount(@Param("bookId") Long bookId);

    /**
     * 查询书籍最新已发布章节
     */
    PortalBookChapter selectLatestPublishedChapter(@Param("bookId") Long bookId);

    /**
     * 浏览量 +1
     */
    int incrementViewCount(@Param("id") Long id);
}
