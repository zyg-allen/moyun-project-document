package com.moyun.system.controller;

import com.moyun.common.annotation.Log;
import com.moyun.core.base.BaseController;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.TableDataInfo;
import com.moyun.common.enums.BusinessType;
import com.moyun.system.domain.SysNotice;
import com.moyun.system.service.ISysNoticeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 公告 信息操作处理
 * 
 * @author ruoyi
 */
@Tag(name = "通知公告管理", description = "通知公告的增删改查操作接口")
@RestController
@RequestMapping("/system/notice")
public class SysNoticeController extends BaseController
{
    @Autowired
    private ISysNoticeService noticeService;

    /**
     * 获取通知公告列表
     */
    @Operation(summary = "获取通知公告列表", description = "分页查询通知公告列表")
    @PreAuthorize("@ss.hasPermi('system:notice:list')")
    @GetMapping("/list")
    public TableDataInfo list(SysNotice notice)
    {
        startPage();
        List<SysNotice> list = noticeService.selectNoticeList(notice);
        return getDataTable(list);
    }

    /**
     * 获取首页顶部公告列表
     */
    @Operation(summary = "获取首页顶部公告列表", description = "获取首页顶部展示的公告列表")
    @GetMapping("/listTop")
    public AjaxResult listTop()
    {
        return success(noticeService.selectNoticeTop());
    }

    /**
     * 标记公告已读
     */
    @Operation(summary = "标记公告已读", description = "标记指定公告为已读")
    @PostMapping("/markRead")
    public AjaxResult markRead(@Parameter(description = "公告ID") @RequestParam Long noticeId)
    {
        return toAjax(noticeService.markNoticeRead(noticeId, getUserId()));
    }

    /**
     * 批量标记公告已读
     */
    @Operation(summary = "批量标记公告已读", description = "批量标记公告为已读")
    @PostMapping("/markReadAll")
    public AjaxResult markReadAll(@Parameter(description = "公告ID数组") @RequestParam Long[] ids)
    {
        return toAjax(noticeService.markNoticeReadAll(ids, getUserId()));
    }

    /**
     * 根据通知公告编号获取详细信息
     */
    @Operation(summary = "获取通知公告详情", description = "根据通知公告ID获取详细信息")
    @PreAuthorize("@ss.hasPermi('system:notice:query')")
    @GetMapping(value = "/{noticeId}")
    public AjaxResult getInfo(@Parameter(description = "通知公告ID") @PathVariable Long noticeId)
    {
        return success(noticeService.selectNoticeById(noticeId));
    }

    /**
     * 新增通知公告
     */
    @Operation(summary = "新增通知公告", description = "创建新通知公告")
    @PreAuthorize("@ss.hasPermi('system:notice:add')")
    @Log(title = "通知公告", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody SysNotice notice)
    {
        notice.setCreateBy(getUsername());
        return toAjax(noticeService.insertNotice(notice));
    }

    /**
     * 修改通知公告
     */
    @Operation(summary = "修改通知公告", description = "更新通知公告信息")
    @PreAuthorize("@ss.hasPermi('system:notice:edit')")
    @Log(title = "通知公告", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody SysNotice notice)
    {
        notice.setUpdateBy(getUsername());
        return toAjax(noticeService.updateNotice(notice));
    }

    /**
     * 删除通知公告
     */
    @Operation(summary = "删除通知公告", description = "批量删除通知公告")
    @PreAuthorize("@ss.hasPermi('system:notice:remove')")
    @Log(title = "通知公告", businessType = BusinessType.DELETE)
    @DeleteMapping("/{noticeIds}")
    public AjaxResult remove(@Parameter(description = "通知公告ID数组") @PathVariable Long[] noticeIds)
    {
        return toAjax(noticeService.deleteNoticeByIds(noticeIds));
    }
}
