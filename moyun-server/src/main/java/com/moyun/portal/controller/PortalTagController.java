package com.moyun.portal.controller;

import jakarta.servlet.http.HttpServletResponse;
import java.util.List;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.moyun.common.annotation.Log;
import com.moyun.common.enums.BusinessType;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.portal.domain.dto.PortalTagBindDTO;
import com.moyun.portal.domain.entity.PortalTag;
import com.moyun.portal.domain.query.TagQuery;
import com.moyun.portal.service.IPortalTagService;
import com.moyun.util.bean.PageUtils;
import com.moyun.util.file.ExcelUtil;

@Tag(name = "门户标签", description = "门户标签的增删改查操作接口")
@RestController
@RequestMapping("/portal/tag")
public class PortalTagController extends BaseController {

    @Autowired
    private IPortalTagService portalTagService;

    @Operation(summary = "获取标签列表", description = "根据条件分页查询标签列表")
    @GetMapping("/list")
    public AjaxResult list(TagQuery query) {
        Page<PortalTag> page = PageUtils.buildPage(query);
        Page<PortalTag> resultPage = portalTagService.selectPortalTagPage(page, query);
        return success(resultPage);
    }

    @Operation(summary = "获取热门标签", description = "按引用次数排行获取热门标签，支持按 module 过滤")
    @GetMapping("/hot")
    public AjaxResult getHotTags(
            @RequestParam(required = false) String module,
            @RequestParam(defaultValue = "20") Integer limit) {
        return success(portalTagService.getHotTags(module, limit));
    }

    @Operation(summary = "搜索标签", description = "根据关键词搜索标签")
    @GetMapping("/search")
    public AjaxResult searchTags(@RequestParam String keyword) {
        TagQuery query = new TagQuery();
        query.setName(keyword);
        List<PortalTag> list = portalTagService.selectPortalTagList(query);
        return success(list);
    }

    @Operation(summary = "获取推荐标签", description = "根据标题和分类推荐标签")
    @GetMapping("/recommend")
    public AjaxResult getRecommendTags(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String category) {
        TagQuery query = new TagQuery();
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

    @Operation(summary = "绑定标签到实体", description = "根据 entityType/entityId 绑定标签，支持按 id 或名称自动创建")
    @Log(title = "门户标签-绑定", businessType = BusinessType.UPDATE)
    @PostMapping("/bind")
    public AjaxResult bindTags(@Validated @RequestBody PortalTagBindDTO dto) {
        portalTagService.bindTags(
            dto.getEntityType(),
            dto.getEntityId(),
            dto.getTagIds() == null ? java.util.Collections.emptyList() : dto.getTagIds(),
            dto.getTagNames() == null ? java.util.Collections.emptyList() : dto.getTagNames(),
            dto.getModule()
        );
        return success();
    }

    @Operation(summary = "解绑实体标签", description = "删除 entityType/entityId 关联的所有标签")
    @Log(title = "门户标签-解绑", businessType = BusinessType.DELETE)
    @DeleteMapping("/unbind/{entityType}/{entityId}")
    public AjaxResult unbindTags(@PathVariable String entityType, @PathVariable Long entityId) {
        portalTagService.unbindTags(entityType, entityId);
        return success();
    }

    @Operation(summary = "获取实体关联标签", description = "按 entityType/entityId 查询标签列表")
    @GetMapping("/entity/{entityType}/{entityId}")
    public AjaxResult getTagsByEntity(@PathVariable String entityType, @PathVariable Long entityId) {
        return success(portalTagService.getTagsByEntity(entityType, entityId));
    }

    @Operation(summary = "导出标签", description = "导出标签数据到Excel文件")
    @Log(title = "门户标签", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, TagQuery query) {
        List<PortalTag> list = portalTagService.selectPortalTagList(query);
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