package com.moyun.ext.cms.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.moyun.common.annotation.Log;
import com.moyun.common.enums.BusinessType;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.ext.cms.domain.query.CmsPortalUserQuery;
import com.moyun.ext.cms.domain.vo.CmsPortalUserVO;
import com.moyun.ext.cms.service.ICmsPortalUserService;
import com.moyun.portal.domain.entity.PortalUser;
import com.moyun.util.bean.PageUtils;

/**
 * CMS门户用户管理Controller
 *
 * @author moyun
 */
@Tag(name = "CMS门户用户管理", description = "CMS门户用户管理接口")
@RestController
@RequestMapping("/cms/user")
public class CmsPortalUserController extends BaseController {
    @Autowired
    private ICmsPortalUserService cmsPortalUserService;

    /**
     * 获取门户用户列表
     */
    @Operation(summary = "获取门户用户列表", description = "根据条件分页查询门户用户列表")
    @PreAuthorize("@ss.hasPermi('cms:user:list')")
    @GetMapping("/list")
    public AjaxResult list(CmsPortalUserQuery query) {
        Page<CmsPortalUserVO> page = PageUtils.buildPage(query);
        page = cmsPortalUserService.selectUserPage(page, query);
        return success(page);
    }

    /**
     * 根据门户用户编号获取详细信息
     */
    @Operation(summary = "获取门户用户详情", description = "根据用户ID获取门户用户详细信息")
    @PreAuthorize("@ss.hasPermi('cms:user:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@Parameter(description = "用户ID") @PathVariable("id") Long id) {
        return success(cmsPortalUserService.selectUserById(id));
    }

    /**
     * 获取门户用户画像（含完整画像 + 业务统计 + 快速跳转入口）
     *
     * <p>用于后台"用户画像"抽屉展示，让管理员快速掌握客户状态：
     * <ul>
     *   <li>完整画像：学校、公司、地点、网站、GitHub、认证、VIP、验证状态等</li>
     *   <li>业务统计：文章/评论/收藏/书架/反馈/举报/粉丝/关注/获赞等</li>
     *   <li>快速跳转：跳转到文章管理、评论管理、反馈管理、举报管理（带用户筛选参数）</li>
     * </ul>
     */
    @Operation(summary = "获取门户用户画像", description = "聚合用户完整画像、业务统计、快速跳转入口，用于后台画像抽屉")
    @PreAuthorize("@ss.hasPermi('cms:user:query')")
    @GetMapping(value = "/{id}/profile")
    public AjaxResult getProfile(@Parameter(description = "用户ID") @PathVariable("id") Long id) {
        return success(cmsPortalUserService.selectUserProfile(id));
    }

    /**
     * 新增门户用户
     */
    @Operation(summary = "新增门户用户", description = "创建新门户用户")
    @PreAuthorize("@ss.hasPermi('cms:user:add')")
    @Log(title = "门户用户管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody PortalUser user) {
        return toAjax(cmsPortalUserService.insertUser(user));
    }

    /**
     * 修改门户用户
     */
    @Operation(summary = "修改门户用户", description = "更新门户用户信息")
    @PreAuthorize("@ss.hasPermi('cms:user:edit')")
    @Log(title = "门户用户管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody PortalUser user) {
        return toAjax(cmsPortalUserService.updateUser(user));
    }

    /**
     * 状态修改
     */
    @Operation(summary = "修改门户用户状态", description = "启用或禁用门户用户")
    @PreAuthorize("@ss.hasPermi('cms:user:status')")
    @Log(title = "门户用户管理", businessType = BusinessType.UPDATE)
    @PutMapping("/changeStatus")
    public AjaxResult changeStatus(@RequestBody PortalUser user) {
        return toAjax(cmsPortalUserService.changeStatus(user));
    }

    /**
     * 重置用户密码
     */
    @Operation(summary = "重置门户用户密码", description = "重置用户密码")
    @PreAuthorize("@ss.hasPermi('cms:user:resetPwd')")
    @Log(title = "门户用户管理", businessType = BusinessType.UPDATE)
    @PutMapping("/resetPwd")
    public AjaxResult resetPwd(@RequestBody PortalUser user) {
        return toAjax(cmsPortalUserService.resetUserPwd(user));
    }

    /**
     * 删除门户用户
     */
    @Operation(summary = "删除门户用户", description = "批量删除门户用户")
    @PreAuthorize("@ss.hasPermi('cms:user:remove')")
    @Log(title = "门户用户管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@Parameter(description = "用户ID数组") @PathVariable("ids") Long[] ids) {
        return toAjax(cmsPortalUserService.deleteUserByIds(ids));
    }
}
