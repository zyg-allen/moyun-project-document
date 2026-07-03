package com.moyun.portal.controller;

import com.moyun.common.constant.HttpStatus;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.core.base.page.PageDomain;
import com.moyun.ext.cms.service.ICodeRunService;
import com.moyun.portal.util.PortalSecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 在线代码运行 Controller（任务 3.6 学习者成长闭环）
 * <p>
 * 全部接口需登录：执行代码会落库运行历史，并消耗服务器资源。
 *
 * @author moyun
 */
@Tag(name = "在线代码运行", description = "沙箱执行用户代码、运行历史查询")
@RestController
@RequestMapping("/portal/code")
public class PortalCodeRunController extends BaseController {

    @Autowired
    private ICodeRunService codeRunService;

    private Long currentUserId() {
        return PortalSecurityUtils.getUserId();
    }

    @Operation(summary = "执行代码", description = "沙箱执行 java/python/javascript 代码，超时 5s，输出截断 1MB，同步返回结果")
    @PostMapping("/run")
    public AjaxResult run(@RequestBody CodeRunRequest body) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        if (body == null || body.getLanguage() == null || body.getCode() == null) {
            return AjaxResult.error("language 与 code 不能为空");
        }
        return AjaxResult.success(codeRunService.runCode(userId, body.getLanguage(), body.getCode(), body.getStdin()));
    }

    @Operation(summary = "我的运行历史", description = "分页查询当前用户的代码运行历史，按时间倒序")
    @GetMapping("/my/runs")
    public AjaxResult myRuns(PageDomain query) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        return AjaxResult.success(codeRunService.listMyRuns(userId, query));
    }

    @Operation(summary = "运行详情", description = "查询某次运行记录详情（仅本人）")
    @GetMapping("/run/{id}")
    public AjaxResult detail(@PathVariable("id") Long id) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        Object detail = codeRunService.getRunDetail(id, userId);
        if (detail == null) {
            return AjaxResult.error("运行记录不存在");
        }
        return AjaxResult.success(detail);
    }

    /** 运行请求体 */
    @lombok.Data
    public static class CodeRunRequest {
        /** java/python/javascript */
        private String language;
        /** 源代码 */
        private String code;
        /** 标准输入（可空） */
        private String stdin;
    }
}
