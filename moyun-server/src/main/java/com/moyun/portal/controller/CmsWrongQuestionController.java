package com.moyun.portal.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.ext.cms.domain.query.WrongQuestionQuery;
import com.moyun.ext.cms.domain.vo.WrongQuestionVO;
import com.moyun.portal.mapper.PortalWrongQuestionMapper;
import com.moyun.util.bean.PageUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 错题本 后台 Controller（任务 3.3 后台，只读查看）
 *
 * <p>供后台管理页面调用，提供分页只读查询能力。</p>
 *
 * <p>路径前缀 /cms/portal/wrongQuestion，权限标识 portal:wrongQuestion:list。</p>
 *
 * @author moyun
 */
@Tag(name = "错题本管理", description = "错题本后台只读查看接口")
@RestController
@RequestMapping("/cms/portal/wrongQuestion")
public class CmsWrongQuestionController extends BaseController {

    @Autowired
    private PortalWrongQuestionMapper wrongQuestionMapper;

    @Operation(summary = "错题本分页列表", description = "分页查询全部错题，支持按用户ID、状态、标签、关键词筛选（只读）")
    @PreAuthorize("@ss.hasPermi('portal:wrongQuestion:list')")
    @GetMapping("/list")
    public AjaxResult list(@RequestParam(required = false) Long userId,
                            WrongQuestionQuery query,
                            @RequestParam(defaultValue = "1") Integer pageNum,
                            @RequestParam(defaultValue = "10") Integer pageSize) {
        // query 中的 pageNum/pageSize 由 @RequestParam 注入，这里同步覆盖以保证一致性
        query.setPageNum(pageNum);
        query.setPageSize(pageSize);
        Page<WrongQuestionVO> page = PageUtils.buildPage(query);
        Page<WrongQuestionVO> result = wrongQuestionMapper.selectWrongQuestionPage(page, userId, query);
        return success(result);
    }
}
