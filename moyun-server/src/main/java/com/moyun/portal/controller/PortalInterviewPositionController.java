package com.moyun.portal.controller;

import com.moyun.common.annotation.Anonymous;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.ext.cms.service.IPortalInterviewPositionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 面试岗位字典 Controller（门户端，v5.9 阶段1）
 * <p>
 * 暴露岗位字典给前端：模拟面试岗位选择、用户档案目标岗位选择、题库推荐等场景共用。
 * 全部为只读公开接口。
 *
 * @author moyun
 */
@Tag(name = "面试岗位字典", description = "岗位字典查询接口（驱动画像抽题与目标岗位选择）")
@RestController
@RequestMapping("/portal/interview/position")
public class PortalInterviewPositionController extends BaseController {

    @Autowired
    private IPortalInterviewPositionService positionService;

    @Operation(summary = "获取启用的岗位字典列表", description = "按 sort 升序返回所有 status=active 的岗位，含必备技能与热门公司 JSON")
    @GetMapping("/list")
    @Anonymous
    public AjaxResult listActivePositions() {
        return AjaxResult.success(positionService.listActivePositions());
    }
}
