package com.moyun.portal.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.common.annotation.Anonymous;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.ext.cms.domain.query.JobQuery;
import com.moyun.ext.cms.domain.vo.JobListItemVO;
import com.moyun.ext.cms.domain.vo.JobVO;
import com.moyun.ext.cms.service.IPortalJobService;
import com.moyun.util.bean.PageUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 职位 Controller（门户端）
 * <p>
 * 公开接口：职位列表、详情。
 *
 * @author moyun
 */
@Tag(name = "职位", description = "职位列表、详情相关接口")
@RestController
@RequestMapping("/portal/job")
public class PortalJobController extends BaseController {

    @Autowired
    private IPortalJobService portalJobService;

    // ==================== 公开接口 ====================

    @Operation(summary = "职位列表", description = "公开分页查询在招职位（含公司简要信息），支持按公司/城市/经验/学历筛选")
    @GetMapping("/list")
    @Anonymous
    public AjaxResult list(JobQuery query) {
        Page<JobListItemVO> page = PageUtils.buildPage(query);
        return AjaxResult.success(portalJobService.listJobs(query));
    }

    @Operation(summary = "职位详情", description = "公开查询职位详情（含公司信息）")
    @GetMapping("/{id}")
    @Anonymous
    public AjaxResult detail(@PathVariable("id") Long id) {
        JobVO vo = portalJobService.getJobDetail(id);
        if (vo == null) {
            return AjaxResult.error("职位不存在");
        }
        return AjaxResult.success(vo);
    }
}
