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
        // 安全风险：当前 CodeExecutorService 基于 ProcessBuilder 直接执行用户代码，
        // 未引入 Docker / cgroups 等强隔离沙箱，存在 RCE（任意命令执行）风险。
        // 在独立安全沙箱就绪之前，临时禁用此入口，仅保留 Controller 与路由占位，
        // Service / Mapper / 历史查询接口保持不动，待沙箱方案落地后重新启用。
        return AjaxResult.error(503, "代码执行功能正在升级安全沙箱，暂时不可用");
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
