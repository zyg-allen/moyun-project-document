package com.moyun.ledger.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.moyun.common.exception.system.ServiceException;
import com.moyun.core.base.AjaxResult;
import com.moyun.ledger.domain.entity.LedgerSaleChannel;
import com.moyun.ledger.mapper.LedgerSaleChannelMapper;
import com.moyun.portal.util.PortalSecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 售卖渠道：系统预设 + 用户自建。 */
@Tag(name = "门户-售卖渠道")
@RestController
@RequestMapping("/portal/ledger/sale-channels")
public class PortalLedgerSaleChannelController {

    @Autowired
    private LedgerSaleChannelMapper channelMapper;

    @Operation(summary = "渠道列表（系统预设 + 本人自建）")
    @GetMapping
    public AjaxResult list() {
        Long userId = PortalSecurityUtils.getUserId();
        return AjaxResult.success(channelMapper.selectList(new LambdaQueryWrapper<LedgerSaleChannel>()
                .in(LedgerSaleChannel::getUserId, LedgerSaleChannel.SYSTEM_USER_ID, userId)
                .eq(LedgerSaleChannel::getStatus, LedgerSaleChannel.STATUS_ENABLED)
                .orderByAsc(LedgerSaleChannel::getSortOrder)
                .orderByAsc(LedgerSaleChannel::getId)));
    }

    @Operation(summary = "新增渠道")
    @PostMapping
    public AjaxResult create(@RequestBody LedgerSaleChannel bo) {
        Long userId = PortalSecurityUtils.getUserId();
        String name = bo.getName() == null ? null : bo.getName().trim();
        if (name == null || name.isEmpty()) {
            throw new ServiceException("请填写渠道名称");
        }
        Long dup = channelMapper.selectCount(new LambdaQueryWrapper<LedgerSaleChannel>()
                .eq(LedgerSaleChannel::getUserId, userId)
                .eq(LedgerSaleChannel::getName, name));
        if (dup != null && dup > 0) {
            throw new ServiceException("渠道「" + name + "」已存在");
        }
        LedgerSaleChannel c = new LedgerSaleChannel();
        c.setUserId(userId);
        c.setName(name);
        c.setIcon(bo.getIcon());
        c.setSortOrder(bo.getSortOrder() == null ? 0 : bo.getSortOrder());
        c.setIsSystem(0);
        c.setStatus(LedgerSaleChannel.STATUS_ENABLED);
        channelMapper.insert(c);
        return AjaxResult.success(c.getId());
    }

    @Operation(summary = "删除渠道（仅本人自建）")
    @DeleteMapping("/{id}")
    public AjaxResult remove(@PathVariable("id") Long id) {
        Long userId = PortalSecurityUtils.getUserId();
        LedgerSaleChannel c = channelMapper.selectById(id);
        if (c == null) {
            throw new ServiceException("渠道不存在");
        }
        if (c.getUserId() == null || c.getUserId() == LedgerSaleChannel.SYSTEM_USER_ID) {
            throw new ServiceException("系统预设渠道不可删除");
        }
        if (!userId.equals(c.getUserId())) {
            throw new ServiceException("无权删除他人渠道");
        }
        c.setStatus(LedgerSaleChannel.STATUS_DISABLED);
        channelMapper.updateById(c);
        return AjaxResult.success();
    }
}