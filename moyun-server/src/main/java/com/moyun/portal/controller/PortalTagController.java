package com.moyun.portal.controller;

import com.moyun.common.annotation.Log;
import com.moyun.core.base.BaseController;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.TableDataInfo;
import com.moyun.common.enums.BusinessType;
import com.moyun.util.file.ExcelUtil;
import com.moyun.portal.domain.entity.PortalTag;
import com.moyun.portal.service.IPortalTagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "门户标签", description = "门户标签的增删改查操作接口")
@RestController
@RequestMapping("/portal/tag")
public class PortalTagController extends BaseController {

    @Autowired
    private IPortalTagService portalTagService;

    @Operation(summary = "获取标签列表", description = "根据条件分页查询标签列表")
    @GetMapping("/list")
    public TableDataInfo list(PortalTag portalTag) {
        startPage();
        List<PortalTag> list = portalTagService.selectPortalTagList(portalTag);
        return getDataTable(list);
    }

    @Operation(summary = "获取热门标签", description = "获取使用最多的热门标签")
    @GetMapping("/hot")
    public AjaxResult getHotTags(@RequestParam(defaultValue = "20") Integer limit) {
        PortalTag query = new PortalTag();
        query.setStatus("0");
        List<PortalTag> list = portalTagService.selectPortalTagList(query);
        if (list.size() > limit) {
            list = list.subList(0, limit);
        }
        return success(list);
    }

    @Operation(summary = "搜索标签", description = "根据关键词搜索标签")
    @GetMapping("/search")
    public AjaxResult searchTags(@RequestParam String keyword) {
        PortalTag query = new PortalTag();
        query.setName(keyword);
        List<PortalTag> list = portalTagService.selectPortalTagList(query);
        return success(list);
    }

    @Operation(summary = "获取推荐标签", description = "根据标题和分类推荐标签")
    @GetMapping("/recommend")
    public AjaxResult getRecommendTags(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String category) {
        PortalTag query = new PortalTag();
        query.setStatus("0");
        List<PortalTag> list = portalTagService.selectPortalTagList(query);
        if (list.size() > 10) {
            list = list.subList(0, 10);
        }
        return success(list);
    }

    @Operation(summary = "创建标签", description = "创建新标签")
    @PostMapping("/create")
    public AjaxResult createTag(@RequestBody PortalTag portalTag) {
        return toAjax(portalTagService.insertPortalTag(portalTag));
    }

    @Operation(summary = "导出标签", description = "导出标签数据到Excel文件")
    @Log(title = "门户标签", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, PortalTag portalTag) {
        List<PortalTag> list = portalTagService.selectPortalTagList(portalTag);
        ExcelUtil<PortalTag> util = new ExcelUtil<PortalTag>(PortalTag.class);
        util.exportExcel(response, list, "门户标签数据");
    }

    @Operation(summary = "获取标签详情", description = "根据标签ID获取标签详细信息")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@Parameter(description = "标签ID") @PathVariable Long id) {
        return success(portalTagService.selectPortalTagById(id));
    }

    @Operation(summary = "新增标签", description = "创建新标签")
    @Log(title = "门户标签", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody PortalTag portalTag) {
        return toAjax(portalTagService.insertPortalTag(portalTag));
    }

    @Operation(summary = "修改标签", description = "更新标签信息")
    @Log(title = "门户标签", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody PortalTag portalTag) {
        return toAjax(portalTagService.updatePortalTag(portalTag));
    }

    @Operation(summary = "删除标签", description = "批量删除标签")
    @Log(title = "门户标签", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@Parameter(description = "标签ID数组") @PathVariable Long[] ids) {
        return toAjax(portalTagService.deletePortalTagByIds(ids));
    }
}