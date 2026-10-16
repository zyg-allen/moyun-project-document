package com.moyun.system.controller;

import com.moyun.core.base.AjaxResult;
import com.moyun.system.domain.entity.SysPlatform;
import com.moyun.system.service.ISysPlatformService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 平台端管理
 *
 * @author allen-zyg
 */
@Tag(name = "平台端管理", description = "端定义查询接口")
@RestController
@RequestMapping("/system/platform")
public class SysPlatformController {

    @Autowired
    private ISysPlatformService platformService;

    /**
     * 获取端下拉选项（启用状态）
     */
    @Operation(summary = "获取端下拉选项", description = "获取启用状态的端列表用于下拉选择")
    @GetMapping("/optionselect")
    public AjaxResult optionselect() {
        List<SysPlatform> list = platformService.selectPlatformOptionselect();
        return AjaxResult.success(list);
    }
}
