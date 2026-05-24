package com.moyun.community.content.controller.app;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.moyun.community.common.domain.ApiResponse;
import com.moyun.community.content.entity.CmsCategory;
import com.moyun.community.content.mapper.CmsCategoryMapper;
import com.moyun.community.content.model.vo.PortalCategoryVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 前台分类Controller
 *
 * @author moyun
 */
@Tag(name = "分类模块", description = "分类列表、详情等接口")
@RestController
@RequestMapping("/api/category")
@RequiredArgsConstructor
public class PortalCategoryController {

    private final CmsCategoryMapper categoryMapper;

    /**
     * 获取分类列表
     */
    @Operation(summary = "获取分类列表", description = "获取所有分类列表")
    @GetMapping("/list")
    public ApiResponse<List<PortalCategoryVo>> getCategoryList() {
        LambdaQueryWrapper<CmsCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(CmsCategory::getSortOrder);
        List<CmsCategory> categories = categoryMapper.selectList(wrapper);
        List<PortalCategoryVo> voList = categories.stream()
                .map(this::convertToPortalCategoryVo)
                .collect(Collectors.toList());
        return ApiResponse.success(voList);
    }

    /**
     * 获取分类详情
     */
    @Operation(summary = "获取分类详情", description = "根据ID获取分类详情")
    @GetMapping("/{categoryId}")
    public ApiResponse<PortalCategoryVo> getCategoryDetail(
            @Parameter(description = "分类ID", required = true)
            @PathVariable String categoryId) {
        try {
            Long id = Long.parseLong(categoryId);
            CmsCategory category = categoryMapper.selectById(id);
            if (category == null) {
                return ApiResponse.error(404, "分类不存在");
            }
            PortalCategoryVo vo = convertToPortalCategoryVo(category);
            return ApiResponse.success(vo);
        } catch (NumberFormatException e) {
            return ApiResponse.error("无效的分类ID");
        }
    }

    /**
     * 转换为前台分类VO
     */
    private PortalCategoryVo convertToPortalCategoryVo(CmsCategory category) {
        PortalCategoryVo vo = new PortalCategoryVo();
        vo.setId(String.valueOf(category.getCategoryId()));
        vo.setName(category.getCategoryName());
        vo.setDescription(category.getDescription());
        vo.setSortOrder(category.getSortOrder());
        if (category.getCreateTime() != null) {
            vo.setCreatedAt(category.getCreateTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }
        return vo;
    }
}
