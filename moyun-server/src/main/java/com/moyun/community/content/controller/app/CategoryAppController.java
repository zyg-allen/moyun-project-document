package com.moyun.community.content.controller.app;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.moyun.common.core.controller.BaseController;
import com.moyun.common.core.domain.AjaxResult;
import com.moyun.community.content.entity.CmsCategory;
import com.moyun.community.content.entity.CmsTag;
import com.moyun.community.content.mapper.CmsCategoryMapper;
import com.moyun.community.content.mapper.CmsTagMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 前台分类管理Controller
 *
 * @author moyun
 * @date 2026-04-21
 */
@Tag(name = "前台分类管理", description = "文章分类和标签查询接口")
@RestController
@RequestMapping("/app/category")
public class CategoryAppController extends BaseController {

    @Autowired
    private CmsCategoryMapper categoryMapper;

    @Autowired
    private CmsTagMapper tagMapper;

    /**
     * 获取分类列表
     */
    @Operation(summary = "获取分类列表", description = "查询所有启用的文章分类")
    @GetMapping("/list")
    public AjaxResult list() {
        LambdaQueryWrapper<CmsCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CmsCategory::getStatus, "0")
               .eq(CmsCategory::getIsShow, 1)
               .orderByAsc(CmsCategory::getSort);
        List<CmsCategory> list = categoryMapper.selectList(wrapper);
        return success(list);
    }

    /**
     * 获取分类树形结构
     */
    @Operation(summary = "获取分类树", description = "查询分类的树形层级结构")
    @GetMapping("/tree")
    public AjaxResult tree() {
        List<CmsCategory> all = categoryMapper.selectList(null);
        return success(buildTree(all));
    }

    /**
     * 获取标签列表
     */
    @Operation(summary = "获取标签列表", description = "查询所有可用的文章标签")
    @GetMapping("/tags")
    public AjaxResult tags() {
        LambdaQueryWrapper<CmsTag> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CmsTag::getStatus, "0")
               .orderByAsc(CmsTag::getTagId);
        List<CmsTag> list = tagMapper.selectList(wrapper);
        return success(list);
    }

    private List<CmsCategory> buildTree(List<CmsCategory> categories) {
        return categories;
    }
}