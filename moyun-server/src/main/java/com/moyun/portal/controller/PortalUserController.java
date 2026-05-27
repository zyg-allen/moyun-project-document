package com.moyun.portal.controller;

import com.moyun.common.annotation.Log;
import com.moyun.core.base.BaseController;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.TableDataInfo;
import com.moyun.common.enums.BusinessType;
import com.moyun.util.file.ExcelUtil;
import com.moyun.portal.domain.entity.PortalUser;
import com.moyun.portal.service.IPortalUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "门户用户", description = "门户用户的增删改查操作接口")
@RestController
@RequestMapping("/portal/user")
public class PortalUserController extends BaseController {

    @Autowired
    private IPortalUserService portalUserService;

    @Operation(summary = "获取用户列表", description = "根据条件分页查询用户列表")
    @GetMapping("/list")
    public TableDataInfo list(PortalUser portalUser) {
        startPage();
        List<PortalUser> list = portalUserService.selectPortalUserList(portalUser);
        return getDataTable(list);
    }

    @Operation(summary = "导出用户", description = "导出用户数据到Excel文件")
    @Log(title = "门户用户", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, PortalUser portalUser) {
        List<PortalUser> list = portalUserService.selectPortalUserList(portalUser);
        ExcelUtil<PortalUser> util = new ExcelUtil<PortalUser>(PortalUser.class);
        util.exportExcel(response, list, "门户用户数据");
    }

    @Operation(summary = "获取用户详情", description = "根据用户ID获取用户详细信息")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@Parameter(description = "用户ID") @PathVariable Long id) {
        return success(portalUserService.selectPortalUserById(id));
    }

    @Operation(summary = "新增用户", description = "创建新用户")
    @Log(title = "门户用户", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody PortalUser portalUser) {
        if (!portalUserService.checkPortalUserNameUnique(portalUser)) {
            return error("新增用户'" + portalUser.getUsername() + "'失败，登录账号已存在");
        }
        if (!portalUserService.checkPortalEmailUnique(portalUser)) {
            return error("新增用户'" + portalUser.getUsername() + "'失败，邮箱已存在");
        }
        return toAjax(portalUserService.insertPortalUser(portalUser));
    }

    @Operation(summary = "修改用户", description = "更新用户信息")
    @Log(title = "门户用户", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody PortalUser portalUser) {
        if (!portalUserService.checkPortalUserNameUnique(portalUser)) {
            return error("修改用户'" + portalUser.getUsername() + "'失败，登录账号已存在");
        }
        if (!portalUserService.checkPortalEmailUnique(portalUser)) {
            return error("修改用户'" + portalUser.getUsername() + "'失败，邮箱已存在");
        }
        return toAjax(portalUserService.updatePortalUser(portalUser));
    }

    @Operation(summary = "删除用户", description = "批量删除用户")
    @Log(title = "门户用户", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@Parameter(description = "用户ID数组") @PathVariable Long[] ids) {
        return toAjax(portalUserService.deletePortalUserByIds(ids));
    }
}