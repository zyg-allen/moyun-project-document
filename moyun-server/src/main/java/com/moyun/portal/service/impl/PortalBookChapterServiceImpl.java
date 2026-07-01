package com.moyun.portal.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.moyun.common.exception.system.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moyun.portal.domain.entity.PortalBook;
import com.moyun.portal.domain.entity.PortalBookChapter;
import com.moyun.portal.domain.query.BookChapterQuery;
import com.moyun.portal.mapper.PortalBookChapterMapper;
import com.moyun.portal.mapper.PortalBookMapper;
import com.moyun.portal.service.IPortalBookChapterService;
import com.moyun.util.security.SecurityUtils;

/**
 * 书籍章节 业务层实现
 *
 * @author moyun
 */
@Service
public class PortalBookChapterServiceImpl
        extends ServiceImpl<PortalBookChapterMapper, PortalBookChapter>
        implements IPortalBookChapterService {

    @Autowired
    private PortalBookChapterMapper chapterMapper;

    @Autowired
    private PortalBookMapper bookMapper;

    @Override
    public Page<PortalBookChapter> selectChapterPage(Page<PortalBookChapter> page, BookChapterQuery query) {
        return chapterMapper.selectChapterPage(page, query);
    }

    @Override
    public List<PortalBookChapter> selectChapterList(BookChapterQuery query) {
        return chapterMapper.selectChapterList(query);
    }

    @Override
    public PortalBookChapter selectChapterById(Long id) {
        return chapterMapper.selectChapterById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertChapter(PortalBookChapter chapter) {
        // 校验书籍存在
        PortalBook book = bookMapper.selectPortalBookById(chapter.getBookId());
        if (book == null) {
            throw new ServiceException("书籍不存在");
        }
        // 自动分配章节序号（若未指定）
        if (chapter.getChapterNo() == null || chapter.getChapterNo() <= 0) {
            Integer maxNo = chapterMapper.selectMaxChapterNo(chapter.getBookId());
            chapter.setChapterNo((maxNo == null ? 0 : maxNo) + 1);
        }
        // 默认值
        if (chapter.getEditorMode() == null || chapter.getEditorMode().isEmpty()) {
            chapter.setEditorMode("richtext");
        }
        if (chapter.getIsFree() == null) {
            chapter.setIsFree(true);
        }
        if (chapter.getIsPublished() == null) {
            chapter.setIsPublished(false);
        }
        if (chapter.getWordCount() == null) {
            chapter.setWordCount(calcWordCount(chapter.getContent(), chapter.getContentMarkdown()));
        }
        if (chapter.getCreateTime() == null) {
            chapter.setCreateTime(LocalDateTime.now());
        }
        // 填充创建者
        try {
            String username = SecurityUtils.getUsername();
            if (username != null && !username.isEmpty()) {
                chapter.setCreateBy(username);
            }
        } catch (Exception ignored) {
            // 后台调用时获取，非后台调用忽略
        }

        int rows = chapterMapper.insertChapter(chapter);

        // 若直接发布，同步书籍冗余字段
        if (Boolean.TRUE.equals(chapter.getIsPublished())) {
            if (chapter.getPublishTime() == null) {
                chapter.setPublishTime(new Date());
                chapterMapper.updateChapter(chapter);
            }
            updateBookStats(chapter.getBookId());
        }
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateChapter(PortalBookChapter chapter) {
        if (chapter.getUpdateTime() == null) {
            chapter.setUpdateTime(LocalDateTime.now());
        }
        try {
            String username = SecurityUtils.getUsername();
            if (username != null && !username.isEmpty()) {
                chapter.setUpdateBy(username);
            }
        } catch (Exception ignored) {
        }
        // 若内容有变，重新统计字数
        if (chapter.getContent() != null || chapter.getContentMarkdown() != null) {
            PortalBookChapter existing = chapterMapper.selectChapterById(chapter.getId());
            if (existing != null) {
                String content = chapter.getContent() != null ? chapter.getContent() : existing.getContent();
                String markdown = chapter.getContentMarkdown() != null ? chapter.getContentMarkdown() : existing.getContentMarkdown();
                chapter.setWordCount(calcWordCount(content, markdown));
            }
        }
        int rows = chapterMapper.updateChapter(chapter);
        // 若发布状态可能变化，同步书籍统计
        updateBookStats(getBookIdByChapter(chapter));
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteChapterById(Long id) {
        PortalBookChapter chapter = chapterMapper.selectChapterById(id);
        if (chapter == null) {
            return 0;
        }
        Long bookId = chapter.getBookId();
        int rows = chapterMapper.deleteChapterById(id);
        // 同步书籍统计
        if (Boolean.TRUE.equals(chapter.getIsPublished())) {
            updateBookStats(bookId);
        }
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteChapterByIds(Long[] ids) {
        if (ids == null || ids.length == 0) {
            return 0;
        }
        // 收集涉及的 bookId
        java.util.Set<Long> bookIds = new java.util.HashSet<>();
        for (Long id : ids) {
            PortalBookChapter chapter = chapterMapper.selectChapterById(id);
            if (chapter != null && Boolean.TRUE.equals(chapter.getIsPublished())) {
                bookIds.add(chapter.getBookId());
            }
        }
        int rows = chapterMapper.deleteChapterByIds(ids);
        // 同步书籍统计
        for (Long bookId : bookIds) {
            updateBookStats(bookId);
        }
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int publishChapter(Long id) {
        PortalBookChapter chapter = chapterMapper.selectChapterById(id);
        if (chapter == null) {
            throw new ServiceException("章节不存在");
        }
        if (Boolean.TRUE.equals(chapter.getIsPublished())) {
            return 1; // 已发布，幂等
        }
        chapter.setIsPublished(true);
        chapter.setPublishTime(new Date());
        chapter.setUpdateTime(LocalDateTime.now());
        int rows = chapterMapper.updateChapter(chapter);
        // 同步书籍冗余字段
        updateBookStats(chapter.getBookId());
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int unpublishChapter(Long id) {
        PortalBookChapter chapter = chapterMapper.selectChapterById(id);
        if (chapter == null) {
            throw new ServiceException("章节不存在");
        }
        if (!Boolean.TRUE.equals(chapter.getIsPublished())) {
            return 1; // 已撤回，幂等
        }
        chapter.setIsPublished(false);
        chapter.setUpdateTime(LocalDateTime.now());
        int rows = chapterMapper.updateChapter(chapter);
        updateBookStats(chapter.getBookId());
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchImportChapters(Long bookId, List<PortalBookChapter> chapters, Boolean autoPublish) {
        if (bookId == null) {
            throw new ServiceException("书籍ID不能为空");
        }
        if (chapters == null || chapters.isEmpty()) {
            throw new ServiceException("章节数据不能为空");
        }
        if (chapters.size() > 500) {
            throw new ServiceException("单次导入不超过 500 章");
        }

        PortalBook book = bookMapper.selectPortalBookById(bookId);
        if (book == null) {
            throw new ServiceException("书籍不存在");
        }

        // 当前最大章节序号（支持追加导入）
        Integer startNo = chapterMapper.selectMaxChapterNo(bookId);
        if (startNo == null) startNo = 0;

        String createBy = null;
        try {
            createBy = SecurityUtils.getUsername();
        } catch (Exception ignored) {
        }
        Date now = new Date();
        boolean shouldPublish = Boolean.TRUE.equals(autoPublish);

        List<PortalBookChapter> toInsert = new ArrayList<>();
        int totalWordCount = 0;
        int chapterNo = startNo;
        for (PortalBookChapter pc : chapters) {
            chapterNo++;
            if (pc.getTitle() == null || pc.getTitle().trim().isEmpty()) {
                pc.setTitle("第" + chapterNo + "章");
            }
            pc.setBookId(bookId);
            pc.setChapterNo(chapterNo);
            pc.setEditorMode(pc.getEditorMode() != null ? pc.getEditorMode() : "markdown");
            pc.setIsFree(pc.getIsFree() != null ? pc.getIsFree() : true);
            pc.setIsPublished(shouldPublish);
            pc.setPublishTime(shouldPublish ? now : null);
            pc.setViewCount(0L);
            pc.setWordCount(calcWordCount(pc.getContent(), pc.getContentMarkdown()));
            pc.setCreateBy(createBy);
            pc.setCreateTime(LocalDateTime.now());
            toInsert.add(pc);
            totalWordCount += pc.getWordCount() == null ? 0 : pc.getWordCount();
        }

        // 批量插入
        int rows = chapterMapper.batchInsertChapter(toInsert);

        // 同步书籍冗余字段
        if (shouldPublish) {
            updateBookStats(bookId);
        } else {
            // 即使未发布，也更新章节数（含草稿）和最后更新时间
            PortalBook update = new PortalBook();
            update.setId(bookId);
            update.setLastUpdateTime(now);
            bookMapper.updatePortalBook(update);
        }
        return rows;
    }

    @Override
    public PortalBookChapter selectPrevChapter(Long bookId, Integer chapterNo) {
        return chapterMapper.selectPrevChapter(bookId, chapterNo);
    }

    @Override
    public PortalBookChapter selectNextChapter(Long bookId, Integer chapterNo) {
        return chapterMapper.selectNextChapter(bookId, chapterNo);
    }

    @Override
    public int incrementViewCount(Long id) {
        return chapterMapper.incrementViewCount(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recountBookStats(Long bookId) {
        updateBookStats(bookId);
    }

    // -------------------------------------------------------
    // 私有辅助方法
    // -------------------------------------------------------

    /**
     * 同步书籍冗余字段：word_count / chapter_count / latest_chapter_* / last_update_time
     */
    private void updateBookStats(Long bookId) {
        if (bookId == null) return;
        PortalBook book = bookMapper.selectPortalBookById(bookId);
        if (book == null) return;

        Integer publishedCount = chapterMapper.selectPublishedChapterCount(bookId);
        Long publishedWords = chapterMapper.selectPublishedWordCount(bookId);
        PortalBookChapter latest = chapterMapper.selectLatestPublishedChapter(bookId);

        PortalBook update = new PortalBook();
        update.setId(bookId);
        update.setChapterCount(publishedCount == null ? 0 : publishedCount);
        update.setWordCount(publishedWords == null ? 0 : publishedWords);
        update.setLastUpdateTime(new Date());
        if (latest != null) {
            update.setLatestChapterId(latest.getId());
            update.setLatestChapterTitle(latest.getTitle());
        } else {
            // 没有已发布章节
            update.setLatestChapterId(null);
            update.setLatestChapterTitle(null);
        }
        bookMapper.updatePortalBook(update);
    }

    private Long getBookIdByChapter(PortalBookChapter chapter) {
        if (chapter.getBookId() != null) return chapter.getBookId();
        if (chapter.getId() != null) {
            PortalBookChapter existing = chapterMapper.selectChapterById(chapter.getId());
            return existing != null ? existing.getBookId() : null;
        }
        return null;
    }

    /**
     * 字数统计：优先用 Markdown 内容，否则用 HTML 内容（去标签）
     */
    private Integer calcWordCount(String content, String markdown) {
        String text = markdown;
        if (text == null || text.isEmpty()) {
            text = content;
        }
        if (text == null || text.isEmpty()) {
            return 0;
        }
        // 去除 HTML 标签
        text = text.replaceAll("<[^>]+>", "");
        // 去除空白
        text = text.replaceAll("\\s+", "");
        return text.length();
    }
}
