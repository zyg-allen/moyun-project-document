package com.moyun.portal.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.moyun.common.annotation.Log;
import com.moyun.common.enums.BusinessType;

import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.portal.domain.entity.PortalBookChapter;
import com.moyun.portal.domain.query.BookChapterQuery;
import com.moyun.portal.service.IPortalBookChapterService;
import com.moyun.util.bean.PageUtils;

import java.util.List;

/**
 * 读书空间-章节管理Controller（后台）
 *
 * <p>路径前缀 /portal/admin/ 由核心 SecurityConfig 处理 admin token，
 * 方法级权限通过 @PreAuthorize("@ss.hasPermi(...)") 校验。</p>
 *
 * @author moyun
 */
@Tag(name = "读书空间-章节管理", description = "书籍章节管理相关接口")
@RestController
@RequestMapping("/portal/admin/book-chapters")
public class PortalBookChapterAdminController extends BaseController {

    @Autowired
    private IPortalBookChapterService chapterService;

    @Operation(summary = "查询章节列表")
    @PreAuthorize("@ss.hasPermi('portal:bookChapter:list')")
    @GetMapping("/list")
    public AjaxResult list(BookChapterQuery query) {
        if (query.getBookId() == null) {
            return error("书籍ID不能为空");
        }
        Page<PortalBookChapter> page = PageUtils.buildPage(query);
        Page<PortalBookChapter> result = chapterService.selectChapterPage(page, query);
        return success(result);
    }

    @Operation(summary = "获取章节详情")
    @PreAuthorize("@ss.hasPermi('portal:bookChapter:query')")
    @GetMapping("/{id}")
    public AjaxResult getById(@PathVariable Long id) {
        return success(chapterService.selectChapterById(id));
    }

    @Operation(summary = "新增章节")
    @PreAuthorize("@ss.hasPermi('portal:bookChapter:add')")
    @Log(title = "读书空间-章节", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody PortalBookChapter chapter) {
        int result = chapterService.insertChapter(chapter);
        return result > 0 ? success("新增成功") : error("新增失败");
    }

    @Operation(summary = "修改章节")
    @PreAuthorize("@ss.hasPermi('portal:bookChapter:edit')")
    @Log(title = "读书空间-章节", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult update(@RequestBody PortalBookChapter chapter) {
        int result = chapterService.updateChapter(chapter);
        return result > 0 ? success("修改成功") : error("修改失败");
    }

    @Operation(summary = "删除章节")
    @PreAuthorize("@ss.hasPermi('portal:bookChapter:remove')")
    @Log(title = "读书空间-章节", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public AjaxResult delete(@PathVariable Long id) {
        int result = chapterService.deleteChapterById(id);
        return result > 0 ? success("删除成功") : error("删除失败");
    }

    @Operation(summary = "批量删除章节")
    @PreAuthorize("@ss.hasPermi('portal:bookChapter:remove')")
    @Log(title = "读书空间-章节", businessType = BusinessType.DELETE)
    @DeleteMapping("/ids/{ids}")
    public AjaxResult deleteBatch(@PathVariable Long[] ids) {
        int result = chapterService.deleteChapterByIds(ids);
        return result > 0 ? success("删除成功") : error("删除失败");
    }

    @Operation(summary = "发布章节")
    @PreAuthorize("@ss.hasPermi('portal:bookChapter:publish')")
    @Log(title = "读书空间-章节", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}/publish")
    public AjaxResult publish(@PathVariable Long id) {
        int result = chapterService.publishChapter(id);
        return result > 0 ? success("发布成功") : error("发布失败");
    }

    @Operation(summary = "撤回发布")
    @PreAuthorize("@ss.hasPermi('portal:bookChapter:publish')")
    @Log(title = "读书空间-章节", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}/unpublish")
    public AjaxResult unpublish(@PathVariable Long id) {
        int result = chapterService.unpublishChapter(id);
        return result > 0 ? success("撤回成功") : error("撤回失败");
    }

    @Operation(summary = "批量导入章节（确认入库）")
    @PreAuthorize("@ss.hasPermi('portal:bookChapter:add')")
    @Log(title = "读书空间-章节", businessType = BusinessType.IMPORT)
    @PostMapping("/batch-import")
    public AjaxResult batchImport(@RequestBody ChapterImportDTO dto) {
        if (dto.getBookId() == null) {
            return error("书籍ID不能为空");
        }
        if (dto.getChapters() == null || dto.getChapters().isEmpty()) {
            return error("章节数据不能为空");
        }
        if (dto.getChapters().size() > 500) {
            return error("单次导入不超过 500 章");
        }
        int rows = chapterService.batchImportChapters(dto.getBookId(), dto.getChapters(), dto.getAutoPublish());
        return success("成功导入 " + rows + " 章");
    }

    @Operation(summary = "重新统计书籍字数与章节数（数据修复）")
    @PreAuthorize("@ss.hasPermi('portal:bookChapter:edit')")
    @PostMapping("/recount/{bookId}")
    public AjaxResult recount(@PathVariable Long bookId) {
        chapterService.recountBookStats(bookId);
        return success("统计完成");
    }

    /**
     * 批量导入请求体
     */
    @lombok.Data
    public static class ChapterImportDTO {
        /** 所属书籍ID */
        private Long bookId;
        /** 章节列表（用户可能已编辑过标题/内容） */
        private List<PortalBookChapter> chapters;
        /** 是否自动发布（false=草稿） */
        private Boolean autoPublish = false;
    }
}
