package com.moyun.system.controller;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.common.annotation.Log;
import com.moyun.common.enums.BusinessType;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.core.base.TableDataInfo;
import com.moyun.system.domain.entity.SysSensitiveWord;
import com.moyun.system.service.ISensitiveWordService;
import com.moyun.util.bean.PageUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * CMS 敏感词管理 Controller
 * <p>路径前缀 /system/sensitiveWord。提供敏感词库的 CRUD 与词树刷新入口，
 * 供 CMS 后台维护词库。</p>
 *
 * @author moyun
 */
@Tag(name = "敏感词管理", description = "敏感词库的维护与刷新")
@RestController
@RequestMapping("/system/sensitiveWord")
public class SysSensitiveWordController extends BaseController {

    @Autowired
    private ISensitiveWordService sensitiveWordService;

    /**
     * 分页查询敏感词列表
     */
    @Operation(summary = "敏感词列表", description = "分页查询敏感词库，支持按词/分类/状态筛选")
    @PreAuthorize("@ss.hasPermi('system:sensitiveWord:list')")
    @GetMapping("/list")
    public TableDataInfo list(SysSensitiveWord query) {
        Page<SysSensitiveWord> page = PageUtils.startPage();
        Page<SysSensitiveWord> result = sensitiveWordService.selectWordPage(page, query);
        TableDataInfo rsp = new TableDataInfo();
        rsp.setCode(200);
        rsp.setMsg("查询成功");
        rsp.setRows(result.getRecords());
        rsp.setTotal(result.getTotal());
        return rsp;
    }

    /**
     * 获取敏感词详情
     */
    @Operation(summary = "敏感词详情", description = "根据ID获取敏感词")
    @PreAuthorize("@ss.hasPermi('system:sensitiveWord:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        return success(sensitiveWordService.getById(id));
    }

    /**
     * 新增敏感词
     */
    @Operation(summary = "新增敏感词", description = "新增敏感词，自动去空白、小写归一、触发词树刷新")
    @PreAuthorize("@ss.hasPermi('system:sensitiveWord:add')")
    @Log(title = "敏感词管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody SysSensitiveWord word) {
        return toAjax(sensitiveWordService.insertWord(word));
    }

    /**
     * 修改敏感词
     */
    @Operation(summary = "修改敏感词", description = "修改敏感词，自动触发词树刷新")
    @PreAuthorize("@ss.hasPermi('system:sensitiveWord:edit')")
    @Log(title = "敏感词管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody SysSensitiveWord word) {
        return toAjax(sensitiveWordService.updateWord(word));
    }

    /**
     * 批量删除敏感词
     */
    @Operation(summary = "删除敏感词", description = "批量删除敏感词，自动触发词树刷新")
    @PreAuthorize("@ss.hasPermi('system:sensitiveWord:remove')")
    @Log(title = "敏感词管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(sensitiveWordService.deleteWordByIds(ids));
    }

    /**
     * 手动刷新词树（运维/排障用）
     */
    @Operation(summary = "刷新词树", description = "手动重新加载敏感词词库到内存")
    @PreAuthorize("@ss.hasPermi('system:sensitiveWord:edit')")
    @PutMapping("/reload")
    public AjaxResult reload() {
        sensitiveWordService.reload();
        return success("词库已刷新");
    }
}
