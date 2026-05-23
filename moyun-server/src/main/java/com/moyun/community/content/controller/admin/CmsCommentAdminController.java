package com.moyun.community.content.controller.admin;

import com.moyun.common.annotation.Log;
import com.moyun.common.core.controller.BaseController;
import com.moyun.common.core.domain.AjaxResult;
import com.moyun.common.core.domain.TableDataInfo;
import com.moyun.common.enums.BusinessType;
import com.moyun.community.content.entity.CmsComment;
import com.moyun.community.content.service.ICmsCommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/comment")
public class CmsCommentAdminController extends BaseController {

    @Autowired
    private ICmsCommentService commentService;

    @PreAuthorize("@ss.hasPermi('content:comment:list')")
    @GetMapping("/list")
    public TableDataInfo list(CmsComment comment) {
        startPage();
        List<CmsComment> list = commentService.list();
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('content:comment:query')")
    @GetMapping(value = "/{commentId}")
    public AjaxResult getInfo(@PathVariable("commentId") Long commentId) {
        return success(commentService.getById(commentId));
    }

    @PreAuthorize("@ss.hasPermi('content:comment:edit')")
    @Log(title = "评论管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody CmsComment comment) {
        return toAjax(commentService.updateById(comment));
    }

    @PreAuthorize("@ss.hasPermi('content:comment:remove')")
    @Log(title = "评论管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{commentIds}")
    public AjaxResult remove(@PathVariable Long[] commentIds) {
        return toAjax(commentService.removeByIds(List.of(commentIds)));
    }

    @PreAuthorize("@ss.hasPermi('content:comment:hide')")
    @Log(title = "评论管理", businessType = BusinessType.UPDATE)
    @PutMapping("/hide/{commentId}")
    public AjaxResult hide(@PathVariable Long commentId) {
        CmsComment comment = new CmsComment();
        comment.setCommentId(commentId);
        comment.setStatus("1");
        return toAjax(commentService.updateById(comment));
    }

    @PreAuthorize("@ss.hasPermi('content:comment:show')")
    @Log(title = "评论管理", businessType = BusinessType.UPDATE)
    @PutMapping("/show/{commentId}")
    public AjaxResult show(@PathVariable Long commentId) {
        CmsComment comment = new CmsComment();
        comment.setCommentId(commentId);
        comment.setStatus("0");
        return toAjax(commentService.updateById(comment));
    }
}