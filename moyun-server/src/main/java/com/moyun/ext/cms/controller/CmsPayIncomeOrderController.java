package com.moyun.ext.cms.controller;

import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.ext.cms.mapper.CmsIncomeOrderMapper;
import com.moyun.portal.domain.entity.PortalUser;
import com.moyun.portal.mapper.PortalUserMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * CMS 收入订单 Controller（收入管理模块）
 *
 * <p>全平台业务订单统一视图：记账App打赏 + 门户打赏/付费阅读（UNION ALL），
 * 取代原 cms/tip 孤儿页；通道单据（pay_order）另见支付订单页，不重复计入。
 *
 * @author moyun
 */
@Tag(name = "CMS收入订单", description = "全平台业务订单统一视图（平台/渠道/状态筛选）")
@RestController
@RequestMapping("/cms/pay/income-order")
public class CmsPayIncomeOrderController extends BaseController {

    @Autowired
    private CmsIncomeOrderMapper incomeOrderMapper;

    @Autowired
    private PortalUserMapper portalUserMapper;

    @Operation(summary = "收入订单列表", description = "合并 ledger_tip_order + portal_tip_order，统一 platform/channel/status/pay_channel 字段")
    @PreAuthorize("@ss.hasPermi('cms:payIncomeOrder:list')")
    @GetMapping("/list")
    public AjaxResult list(@RequestParam(required = false) String platform,
                           @RequestParam(required = false) String channelCode,
                           @RequestParam(required = false) String status,
                           @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
                           @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
                           @RequestParam(defaultValue = "1") long current,
                           @RequestParam(defaultValue = "10") long size) {
        long offset = (current - 1) * size;
        List<Map<String, Object>> records = incomeOrderMapper.selectIncomeOrders(
                platform, channelCode, status, startTime, endTime, offset, size);
        long total = incomeOrderMapper.countIncomeOrders(platform, channelCode, status, startTime, endTime);

        // 昵称批量回填（一次 IN，避免 N+1）
        if (records != null && !records.isEmpty()) {
            Set<Long> userIds = records.stream()
                    .map(r -> toLong(r.get("user_id")))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            if (!userIds.isEmpty()) {
                Map<Long, String> nicknameMap = portalUserMapper.selectBatchIds(userIds).stream()
                        .filter(Objects::nonNull)
                        .collect(Collectors.toMap(PortalUser::getId,
                                u -> u.getNickname() == null ? "" : u.getNickname(), (a, b) -> a));
                for (Map<String, Object> r : records) {
                    Long uid = toLong(r.get("user_id"));
                    r.put("nickname", uid == null ? "" : nicknameMap.getOrDefault(uid, "用户" + uid));
                }
            }
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("records", records);
        data.put("total", total);
        data.put("current", current);
        data.put("size", size);
        return success(data);
    }

    private Long toLong(Object v) {
        return v == null ? null : Long.valueOf(v.toString());
    }
}
