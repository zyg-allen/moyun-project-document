package com.moyun.portal.controller;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.portal.domain.entity.PortalAdSlot;
import com.moyun.portal.domain.query.AdSlotQuery;
import com.moyun.portal.service.IPortalAdSlotService;

/**
 * 门户广告位 Controller
 *
 * 说明：自研广告位 MVP，用于详情页底部展示广告卡片。
 * 仅提供前台公开 list 接口，按广告位标识返回启用中的广告列表（status='0'），按 sort 升序。
 * 流量起来后可接广告联盟，本接口形态保持不变。
 */
@Tag(name = "门户广告位", description = "门户广告位前台展示接口")
@RestController
@RequestMapping("/portal/ad")
public class PortalAdSlotController extends BaseController {

    @Autowired
    private IPortalAdSlotService portalAdSlotService;

    @Operation(summary = "获取广告位列表", description = "根据广告位标识获取启用中的广告列表，按sort升序")
    @GetMapping("/list")
    public AjaxResult list(AdSlotQuery query) {
        // 前台只返回启用中的广告（status='0'）
        query.setStatus("0");
        List<PortalAdSlot> list = portalAdSlotService.selectPortalAdSlotList(query);
        return success(list);
    }
}
