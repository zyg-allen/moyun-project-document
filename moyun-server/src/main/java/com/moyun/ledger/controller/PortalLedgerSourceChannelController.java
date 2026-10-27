package com.moyun.ledger.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.moyun.common.exception.system.ServiceException;
import com.moyun.core.base.AjaxResult;
import com.moyun.ledger.domain.entity.LedgerSourceChannel;
import com.moyun.ledger.mapper.LedgerSourceChannelMapper;
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

/**
 * 进货渠道：系统预设 + 用户自建。
 * 与「售卖渠道」对称，用于追溯货物从哪个平台买的。
 */
@Tag(name = "门户-进货渠道")
@RestController
@RequestMapping("/portal/ledger/source-channels")
public class PortalLedgerSourceChannelController {

    @Autowired
    private LedgerSourceChannelMapper channelMapper;

    @Operation(summary = "进货渠道列表（系统预设 + 本人自建）")
    @GetMapping
    public AjaxResult list() {
        Long userId = PortalSecurityUtils.getUserId();
        return AjaxResult.success(channelMapper.selectList(new LambdaQueryWrapper<LedgerSourceChannel>()
                .in(LedgerSourceChannel::getUserId, LedgerSourceChannel.SYSTEM_USER_ID, userId)
                .eq(LedgerSourceChannel::getStatus, LedgerSourceChannel.STATUS_ENABLED)
                .orderByAsc(LedgerSourceChannel::getSortOrder)
                .orderByAsc(LedgerSourceChannel::getId)));
    }

    @Operation(summary = "新增进货渠道")
    @com.moyun.common.annotation.RepeatSubmit(interval = 2000)
    @com.moyun.common.annotation.RateLimiter(key = "portalledgersourcechannel:", time = 60, count = 120)
    @PostMapping
    public AjaxResult create(@RequestBody LedgerSourceChannel bo) {
        Long userId = PortalSecurityUtils.getUserId();
        String name = bo.getName() == null ? null : bo.getName().trim();
        if (name == null || name.isEmpty()) {
            throw new ServiceException("请填写渠道名称");
        }
        if (name.length() > 50) {
            throw new ServiceException("渠道名称最多 50 个字");
        }
        Long dup = channelMapper.selectCount(new LambdaQueryWrapper<LedgerSourceChannel>()
                .eq(LedgerSourceChannel::getUserId, userId)
                .eq(LedgerSourceChannel::getName, name));
        if (dup != null && dup > 0) {
            throw new ServiceException("渠道「" + name + "」已存在");
        }
        LedgerSourceChannel c = new LedgerSourceChannel();
        c.setUserId(userId);
        c.setName(name);
        c.setIcon(bo.getIcon());
        c.setSortOrder(bo.getSortOrder() == null ? 99 : bo.getSortOrder());
        c.setIsSystem(0);
        c.setStatus(LedgerSourceChannel.STATUS_ENABLED);
        channelMapper.insert(c);
        return AjaxResult.success(c);
    }

    @Operation(summary = "删除进货渠道（系统预设不可删）")
    @com.moyun.common.annotation.RepeatSubmit(interval = 2000)
    @com.moyun.common.annotation.RateLimiter(key = "portalledgersourcechannel:", time = 60, count = 120)
    @DeleteMapping("/{id}")
    public AjaxResult delete(@PathVariable Long id) {
        Long userId = PortalSecurityUtils.getUserId();
        LedgerSourceChannel c = channelMapper.selectById(id);
        if (c == null) {
            throw new ServiceException("渠道不存在");
        }
        if (c.getIsSystem() != null && c.getIsSystem() == 1) {
            throw new ServiceException("系统预设渠道不可删除");
        }
        if (!userId.equals(c.getUserId())) {
            throw new ServiceException("只能删除自己创建的渠道");
        }
        c.setStatus(LedgerSourceChannel.STATUS_DISABLED);
        channelMapper.updateById(c);
        return AjaxResult.success("删除成功");
    }
}
