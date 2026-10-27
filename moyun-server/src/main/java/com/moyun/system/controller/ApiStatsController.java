package com.moyun.system.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.moyun.core.base.AjaxResult;
import com.moyun.core.config.ApiCatalog;
import com.moyun.core.config.ApiStatsFilter;

/**
 * 接口调用统计（管理端）。
 *
 * <p>用于回答"哪些接口从来没被调用"，配合日志确认后可安全下线。
 * 数据在内存中，重启清零；生产建议同时接 Prometheus。</p>
 */
@RestController
@RequestMapping("/cms/monitor/api-stats")
public class ApiStatsController {

    @org.springframework.beans.factory.annotation.Autowired
    private ApiCatalog catalog;

    @GetMapping
    public AjaxResult stats(@RequestParam(value = "limit", defaultValue = "100") int limit) {
        Map<String, Object> data = ApiStatsFilter.snapshot(limit);
        return AjaxResult.success(data);
    }
}
