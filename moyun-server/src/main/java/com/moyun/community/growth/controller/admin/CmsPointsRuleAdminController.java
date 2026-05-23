package com.moyun.community.growth.controller.admin;

import com.moyun.common.annotation.Log;
import com.moyun.common.core.controller.BaseController;
import com.moyun.common.core.domain.AjaxResult;
import com.moyun.common.core.domain.TableDataInfo;
import com.moyun.common.enums.BusinessType;
import com.moyun.community.growth.entity.CmsPointsRule;
import com.moyun.community.growth.service.ICmsPointsRuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/growth/points/rule")
public class CmsPointsRuleAdminController extends BaseController {

    @Autowired
    private ICmsPointsRuleService pointsRuleService;

    @PreAuthorize("@ss.hasPermi('growth:points:rule:list')")
    @GetMapping("/list")
    public TableDataInfo list() {
        startPage();
        List<CmsPointsRule> list = pointsRuleService.list();
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('growth:points:rule:query')")
    @GetMapping(value = "/{ruleId}")
    public AjaxResult getInfo(@PathVariable("ruleId") Long ruleId) {
        return success(pointsRuleService.getById(ruleId));
    }

    @PreAuthorize("@ss.hasPermi('growth:points:rule:add')")
    @Log(title = "积分规则管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody CmsPointsRule rule) {
        return toAjax(pointsRuleService.save(rule));
    }

    @PreAuthorize("@ss.hasPermi('growth:points:rule:edit')")
    @Log(title = "积分规则管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody CmsPointsRule rule) {
        return toAjax(pointsRuleService.updateById(rule));
    }

    @PreAuthorize("@ss.hasPermi('growth:points:rule:remove')")
    @Log(title = "积分规则管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ruleIds}")
    public AjaxResult remove(@PathVariable Long[] ruleIds) {
        return toAjax(pointsRuleService.removeByIds(List.of(ruleIds)));
    }
}