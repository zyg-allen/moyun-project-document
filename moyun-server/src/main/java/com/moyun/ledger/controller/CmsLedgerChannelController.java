package com.moyun.ledger.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.common.annotation.Log;
import com.moyun.common.annotation.RepeatSubmit;
import com.moyun.common.enums.BusinessType;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.core.base.TableDataInfo;
import com.moyun.ledger.domain.vo.CmsChannelVO;
import com.moyun.ledger.service.ICmsLedgerPresetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台-渠道管理（售卖渠道 / 进货渠道）
 *
 * <p>两种渠道分表存储，后台按 {@code kind} 区分读写；
 * 与门户端 {@code /portal/ledger/sale-channels}、{@code /portal/ledger/source-channels}
 * 完全独立，避免管理端与 App 端口径耦合。</p>
 *
 * @author moyun
 */
@Tag(name = "CMS记账-渠道管理", description = "售卖/进货渠道系统预设维护（用户自建只读）")
@RestController
@RequestMapping("/cms/ledger/channel")
public class CmsLedgerChannelController extends BaseController {

    @Autowired
    private ICmsLedgerPresetService presetService;

    @Operation(summary = "渠道列表（kind=sale 售卖 / source 进货）")
    @PreAuthorize("@ss.hasPermi('cms:ledgerChannel:list')")
    @GetMapping("/list")
    public TableDataInfo list(@RequestParam(defaultValue = "sale") String kind,
                              @RequestParam(required = false) String name,
                              @RequestParam(required = false) String owner,
                              @RequestParam(required = false) Integer status,
                              @RequestParam(defaultValue = "1") Integer pageNum,
                              @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<CmsChannelVO> page = presetService.pageChannel(kind, pageNum, pageSize, name, owner, status);
        return getDataTable(page.getRecords(), page.getTotal());
    }

    @Operation(summary = "新增系统预设渠道")
    @PreAuthorize("@ss.hasPermi('cms:ledgerChannel:add')")
    @RepeatSubmit(interval = 3000)
    @Log(title = "记账-渠道", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody CmsChannelVO body) {
        presetService.createChannel(body.getKind(), body);
        return success();
    }

    @Operation(summary = "修改系统预设渠道")
    @PreAuthorize("@ss.hasPermi('cms:ledgerChannel:edit')")
    @RepeatSubmit(interval = 3000)
    @Log(title = "记账-渠道", businessType = BusinessType.UPDATE)
    @PutMapping("/{kind}/{id}")
    public AjaxResult edit(@PathVariable String kind, @PathVariable Long id, @RequestBody CmsChannelVO body) {
        presetService.updateChannel(kind, id, body);
        return success();
    }

    @Operation(summary = "启用/停用系统预设渠道")
    @PreAuthorize("@ss.hasPermi('cms:ledgerChannel:edit')")
    @Log(title = "记账-渠道", businessType = BusinessType.UPDATE)
    @PutMapping("/{kind}/{id}/status/{status}")
    public AjaxResult changeStatus(@PathVariable String kind, @PathVariable Long id, @PathVariable Integer status) {
        presetService.changeChannelStatus(kind, id, status);
        return success();
    }

    @Operation(summary = "删除系统预设渠道（被引用时拒绝）")
    @PreAuthorize("@ss.hasPermi('cms:ledgerChannel:edit')")
    @Log(title = "记账-渠道", businessType = BusinessType.DELETE)
    @DeleteMapping("/{kind}/{id}")
    public AjaxResult remove(@PathVariable String kind, @PathVariable Long id) {
        presetService.deleteChannel(kind, id);
        return success();
    }
}
