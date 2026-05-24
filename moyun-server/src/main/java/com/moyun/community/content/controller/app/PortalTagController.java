package com.moyun.community.content.controller.app;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.community.common.domain.ApiResponse;
import com.moyun.community.common.domain.PageResult;
import com.moyun.community.content.entity.CmsTag;
import com.moyun.community.content.mapper.CmsTagMapper;
import com.moyun.community.content.model.vo.PortalTagVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 前台标签Controller
 *
 * @author moyun
 */
@Tag(name = "标签模块", description = "标签列表、详情等接口")
@RestController
@RequestMapping("/api/tag")
@RequiredArgsConstructor
public class PortalTagController {

    private final CmsTagMapper tagMapper;

    /**
     * 获取标签列表
     */
    @Operation(summary = "获取标签列表", description = "获取标签列表")
    @GetMapping("/list")
    public ApiResponse<PageResult<PortalTagVo>> getTagList(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "50") Integer pageSize,
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword) {
        
        Page<CmsTag> pageObj = new Page<>(page, pageSize);
        LambdaQueryWrapper<CmsTag> wrapper = new LambdaQueryWrapper<>();
        
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(CmsTag::getTagName, keyword);
        }
        
        wrapper.orderByDesc(CmsTag::getTagId);
        Page<CmsTag> resultPage = tagMapper.selectPage(pageObj, wrapper);
        List<PortalTagVo> voList = resultPage.getRecords().stream()
                .map(this::convertToPortalTagVo)
                .collect(Collectors.toList());
        
        PageResult<PortalTagVo> pageResult = PageResult.of(
                voList,
                resultPage.getTotal(),
                page,
                pageSize
        );
        
        return ApiResponse.success(pageResult);
    }

    /**
     * 获取热门标签
     */
    @Operation(summary = "获取热门标签", description = "获取热门标签列表")
    @GetMapping("/hot")
    public ApiResponse<List<PortalTagVo>> getHotTags(
            @Parameter(description = "数量") @RequestParam(defaultValue = "10") Integer limit) {
        Page<CmsTag> pageObj = new Page<>(1, limit);
        LambdaQueryWrapper<CmsTag> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(CmsTag::getCreateTime);
        Page<CmsTag> resultPage = tagMapper.selectPage(pageObj, wrapper);
        List<PortalTagVo> voList = resultPage.getRecords().stream()
                .map(this::convertToPortalTagVo)
                .collect(Collectors.toList());
        return ApiResponse.success(voList);
    }

    /**
     * 获取标签详情
     */
    @Operation(summary = "获取标签详情", description = "根据ID获取标签详情")
    @GetMapping("/{tagId}")
    public ApiResponse<PortalTagVo> getTagDetail(
            @Parameter(description = "标签ID", required = true)
            @PathVariable String tagId) {
        try {
            Long id = Long.parseLong(tagId);
            CmsTag tag = tagMapper.selectById(id);
            if (tag == null) {
                return ApiResponse.error(404, "标签不存在");
            }
            PortalTagVo vo = convertToPortalTagVo(tag);
            return ApiResponse.success(vo);
        } catch (NumberFormatException e) {
            return ApiResponse.error("无效的标签ID");
        }
    }

    /**
     * 搜索标签
     */
    @Operation(summary = "搜索标签", description = "根据关键词搜索标签")
    @GetMapping("/search")
    public ApiResponse<List<PortalTagVo>> searchTags(
            @Parameter(description = "搜索关键词", required = true)
            @RequestParam String q) {
        LambdaQueryWrapper<CmsTag> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(CmsTag::getTagName, q)
                .last("LIMIT 10");
        List<CmsTag> tags = tagMapper.selectList(wrapper);
        List<PortalTagVo> voList = tags.stream()
                .map(this::convertToPortalTagVo)
                .collect(Collectors.toList());
        return ApiResponse.success(voList);
    }

    /**
     * 转换为前台标签VO
     */
    private PortalTagVo convertToPortalTagVo(CmsTag tag) {
        PortalTagVo vo = new PortalTagVo();
        vo.setId(String.valueOf(tag.getTagId()));
        vo.setName(tag.getTagName());
        vo.setArticleCount(tag.getArticleCount() != null ? tag.getArticleCount() : 0);
        vo.setUsageCount(tag.getArticleCount() != null ? tag.getArticleCount() : 0);
        if (tag.getCreateTime() != null) {
            vo.setCreatedAt(tag.getCreateTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }
        return vo;
    }
}
