package com.moyun.ledger.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.moyun.common.annotation.Anonymous;
import com.moyun.common.exception.system.ServiceException;
import com.moyun.core.base.AjaxResult;
import com.moyun.ledger.domain.entity.LedgerInvite;
import com.moyun.ledger.domain.entity.LedgerWarehouse;
import com.moyun.ledger.domain.entity.LedgerWarehouseMember;
import com.moyun.ledger.mapper.LedgerInviteMapper;
import com.moyun.ledger.mapper.LedgerWarehouseMapper;
import com.moyun.ledger.mapper.LedgerWarehouseMemberMapper;
import com.moyun.portal.domain.entity.PortalUser;
import com.moyun.portal.mapper.PortalUserMapper;
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

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 邀请码：生成 / 校验（免登录）/ 接受。
 *
 * <p>校验接口必须免登录 —— 否则未注册用户看不到「谁邀请我去哪」。</p>
 */
@Tag(name = "门户-邀请码")
@RestController
public class PortalInviteController {

    private static final int CODE_LEN = 6;
    private static final int DEFAULT_MAX_USE = 10;
    private static final int EXPIRE_DAYS = 7;

    @Autowired
    private LedgerInviteMapper inviteMapper;

    @Autowired
    private LedgerWarehouseMapper warehouseMapper;

    @Autowired
    private LedgerWarehouseMemberMapper memberMapper;

    @Autowired
    private PortalUserMapper portalUserMapper;

    @Operation(summary = "生成邀请码（仅仓库 owner）")
    @PostMapping("/portal/ledger/invites")
    public AjaxResult create(@RequestBody Map<String, Object> body) {
        Long userId = PortalSecurityUtils.getUserId();
        Long warehouseId = longVal(body.get("warehouseId"));
        if (warehouseId == null) {
            throw new ServiceException("缺少 warehouseId");
        }
        LedgerWarehouse w = warehouseMapper.selectById(warehouseId);
        if (w == null || LedgerWarehouse.STATUS_DELETED == w.getStatus()) {
            throw new ServiceException("仓库不存在");
        }
        if (!userId.equals(w.getUserId())) {
            throw new ServiceException("只有创建者能生成邀请码");
        }

        LedgerInvite inv = new LedgerInvite();
        inv.setCode(uniqueCode());
        inv.setType(LedgerInvite.TYPE_WAREHOUSE);
        inv.setWarehouseId(warehouseId);
        inv.setRole(LedgerInvite.ROLE_MEMBER);
        inv.setCreatedBy(userId);
        inv.setExpireAt(LocalDateTime.now().plusDays(EXPIRE_DAYS));
        inv.setMaxUse(DEFAULT_MAX_USE);
        inv.setUsedCount(0);
        inv.setStatus(LedgerInvite.STATUS_NORMAL);
        inviteMapper.insert(inv);

        Map<String, Object> out = new HashMap<>();
        out.put("code", inv.getCode());
        out.put("expireAt", inv.getExpireAt());
        out.put("maxUse", inv.getMaxUse());
        out.put("warehouseName", w.getName());
        return AjaxResult.success(out);
    }

    @Operation(summary = "校验邀请码（免登录：返回仓库名与邀请人）")
    @Anonymous
    @GetMapping("/portal/invite/{code}")
    public AjaxResult preview(@PathVariable("code") String code) {
        LedgerInvite inv = findByCode(code);
        LedgerWarehouse w = warehouseMapper.selectById(inv.getWarehouseId());
        PortalUser inviter = portalUserMapper.selectById(inv.getCreatedBy());

        Map<String, Object> out = new HashMap<>();
        out.put("code", inv.getCode());
        out.put("warehouseId", inv.getWarehouseId());
        out.put("warehouseName", w == null ? "（仓库已删除）" : w.getName());
        out.put("inviterName", inviter == null ? null
                : (inviter.getNickname() != null && !inviter.getNickname().isEmpty()
                    ? inviter.getNickname() : inviter.getUsername()));
        out.put("remain", Math.max(0, (inv.getMaxUse() == null ? 0 : inv.getMaxUse())
                - (inv.getUsedCount() == null ? 0 : inv.getUsedCount())));
        return AjaxResult.success(out);
    }

    @Operation(summary = "接受邀请（需登录）")
    @PostMapping("/portal/invite/{code}/accept")
    public AjaxResult accept(@PathVariable("code") String code) {
        Long userId = PortalSecurityUtils.getUserId();
        LedgerInvite inv = findByCode(code);

        if (userId.equals(inv.getCreatedBy())) {
            throw new ServiceException("这是你自己生成的邀请码");
        }

        // 已在仓库：幂等返回成功
        Long exists = memberMapper.selectCount(new LambdaQueryWrapper<LedgerWarehouseMember>()
                .eq(LedgerWarehouseMember::getWarehouseId, inv.getWarehouseId())
                .eq(LedgerWarehouseMember::getUserId, userId)
                .eq(LedgerWarehouseMember::getIsActive, 1)
                .eq(LedgerWarehouseMember::getStatus, LedgerWarehouseMember.STATUS_NORMAL));
        if (exists != null && exists > 0) {
            return AjaxResult.success(inv.getWarehouseId());
        }

        // 次数用尽
        int used = inv.getUsedCount() == null ? 0 : inv.getUsedCount();
        int max = inv.getMaxUse() == null ? 0 : inv.getMaxUse();
        if (used >= max) {
            throw new ServiceException("邀请码已用完");
        }

        LedgerWarehouseMember m = new LedgerWarehouseMember();
        m.setWarehouseId(inv.getWarehouseId());
        m.setUserId(userId);
        m.setRole(inv.getRole() == null ? LedgerInvite.ROLE_MEMBER : inv.getRole());
        m.setInvitedBy(inv.getCreatedBy());
        m.setIsActive(1);
        m.setStatus(LedgerWarehouseMember.STATUS_NORMAL);
        memberMapper.insert(m);

        inv.setUsedCount(used + 1);
        inviteMapper.updateById(inv);
        return AjaxResult.success(inv.getWarehouseId());
    }

    @Operation(summary = "撤销邀请码")
    @DeleteMapping("/portal/ledger/invites/{id}")
    public AjaxResult revoke(@PathVariable("id") Long id) {
        Long userId = PortalSecurityUtils.getUserId();
        LedgerInvite inv = inviteMapper.selectById(id);
        if (inv == null) {
            throw new ServiceException("邀请码不存在");
        }
        if (!userId.equals(inv.getCreatedBy())) {
            throw new ServiceException("无权撤销");
        }
        inv.setStatus(LedgerInvite.STATUS_REVOKED);
        inviteMapper.updateById(inv);
        return AjaxResult.success();
    }

    @Operation(summary = "该仓库的有效邀请码列表")
    @GetMapping("/portal/ledger/invites")
    public AjaxResult list(Long warehouseId) {
        Long userId = PortalSecurityUtils.getUserId();
        if (warehouseId == null) {
            throw new ServiceException("缺少 warehouseId");
        }
        LedgerWarehouse w = warehouseMapper.selectById(warehouseId);
        if (w == null || !userId.equals(w.getUserId())) {
            throw new ServiceException("只有创建者能查看邀请码");
        }
        List<LedgerInvite> list = inviteMapper.selectList(new LambdaQueryWrapper<LedgerInvite>()
                .eq(LedgerInvite::getWarehouseId, warehouseId)
                .eq(LedgerInvite::getStatus, LedgerInvite.STATUS_NORMAL)
                .orderByDesc(LedgerInvite::getId));
        return AjaxResult.success(list);
    }

    // ---------------- 内部 ----------------

    private LedgerInvite findByCode(String code) {
        String c = code == null ? "" : code.trim().toUpperCase();
        LedgerInvite inv = inviteMapper.selectOne(new LambdaQueryWrapper<LedgerInvite>()
                .eq(LedgerInvite::getCode, c).last("LIMIT 1"));
        if (inv == null || LedgerInvite.STATUS_REVOKED == inv.getStatus()) {
            throw new ServiceException("邀请码无效");
        }
        if (inv.getExpireAt() != null && inv.getExpireAt().isBefore(LocalDateTime.now())) {
            throw new ServiceException("邀请码已过期");
        }
        return inv;
    }

    private String uniqueCode() {
        for (int i = 0; i < 20; i++) {
            String c = LedgerInvite.randomCode(CODE_LEN);
            Long n = inviteMapper.selectCount(new LambdaQueryWrapper<LedgerInvite>()
                    .eq(LedgerInvite::getCode, c));
            if (n == null || n == 0) {
                return c;
            }
        }
        throw new ServiceException("生成邀请码失败，请重试");
    }

    private Long longVal(Object o) {
        if (o == null) return null;
        try {
            return Long.valueOf(String.valueOf(o));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
