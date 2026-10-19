package com.moyun.ledger.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.moyun.common.exception.system.ServiceException;
import com.moyun.core.base.AjaxResult;
import com.moyun.ledger.domain.entity.LedgerWarehouse;
import com.moyun.ledger.domain.entity.LedgerWarehouseMember;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 仓库成员：多人协作共享库存。
 *
 * <p>只有 owner 能管成员。成员加入后即可看到该仓库的货架 / 商品 / 库存，
 * 并能出入库 —— 这是「合作卖同一批货」的基础。</p>
 *
 * <p>权限：改仓库设置、删仓库、管成员一律仅 owner（见 getOwnerOnlyWarehouse）。</p>
 */
@Tag(name = "门户-仓库成员")
@RestController
@RequestMapping("/portal/ledger/inventory/warehouses/{warehouseId}/members")
public class PortalLedgerWarehouseMemberController {

    public static final String ROLE_OWNER = "owner";
    public static final String ROLE_MEMBER = "member";

    @Autowired
    private LedgerWarehouseMapper warehouseMapper;

    @Autowired
    private LedgerWarehouseMemberMapper memberMapper;

    @Autowired
    private PortalUserMapper portalUserMapper;

    @Operation(summary = "成员列表（含 owner）")
    @GetMapping
    public AjaxResult list(@PathVariable("warehouseId") Long warehouseId) {
        Long userId = PortalSecurityUtils.getUserId();
        LedgerWarehouse w = requireOwner(userId, warehouseId);

        List<Map<String, Object>> out = new ArrayList<>();
        // 创建者放第一位
        PortalUser owner = portalUserMapper.selectById(w.getUserId());
        out.add(build(owner, ROLE_OWNER, true));

        List<LedgerWarehouseMember> members = memberMapper.selectList(
                new LambdaQueryWrapper<LedgerWarehouseMember>()
                        .eq(LedgerWarehouseMember::getWarehouseId, warehouseId)
                        .eq(LedgerWarehouseMember::getStatus, LedgerWarehouseMember.STATUS_NORMAL)
                        .eq(LedgerWarehouseMember::getIsActive, 1));
        for (LedgerWarehouseMember m : members) {
            if (m.getUserId() != null && m.getUserId().equals(w.getUserId())) {
                continue; // owner 已单独放
            }
            out.add(build(portalUserMapper.selectById(m.getUserId()), m.getRole(), false));
        }
        return AjaxResult.success(out);
    }

    @Operation(summary = "添加成员（按手机号或用户名）")
    @PostMapping
    public AjaxResult add(@PathVariable("warehouseId") Long warehouseId, @RequestBody Map<String, Object> body) {
        Long userId = PortalSecurityUtils.getUserId();
        LedgerWarehouse w = requireOwner(userId, warehouseId);

        String account = str(body.get("account"));
        if (account == null || account.isEmpty()) {
            throw new ServiceException("请填写对方手机号或用户名");
        }
        PortalUser target = portalUserMapper.selectOne(new LambdaQueryWrapper<PortalUser>()
                .eq(PortalUser::getPhone, account).last("LIMIT 1"));
        if (target == null) {
            target = portalUserMapper.selectOne(new LambdaQueryWrapper<PortalUser>()
                    .eq(PortalUser::getUsername, account).last("LIMIT 1"));
        }
        if (target == null) {
            throw new ServiceException("没找到这个用户，请确认手机号或用户名");
        }
        if (target.getId().equals(w.getUserId())) {
            throw new ServiceException("这是你自己的仓库");
        }

        LedgerWarehouseMember exists = memberMapper.selectOne(new LambdaQueryWrapper<LedgerWarehouseMember>()
                .eq(LedgerWarehouseMember::getWarehouseId, warehouseId)
                .eq(LedgerWarehouseMember::getUserId, target.getId())
                .last("LIMIT 1"));
        if (exists != null) {
            if (Integer.valueOf(1).equals(exists.getIsActive())
                    && LedgerWarehouseMember.STATUS_NORMAL == exists.getStatus()) {
                throw new ServiceException("该用户已在仓库里");
            }
            // 之前删过：恢复
            exists.setIsActive(1);
            exists.setStatus(LedgerWarehouseMember.STATUS_NORMAL);
            exists.setInvitedBy(userId);
            memberMapper.updateById(exists);
            return AjaxResult.success();
        }

        LedgerWarehouseMember m = new LedgerWarehouseMember();
        m.setWarehouseId(warehouseId);
        m.setUserId(target.getId());
        m.setRole(ROLE_MEMBER);
        m.setInvitedBy(userId);
        m.setIsActive(1);
        m.setStatus(LedgerWarehouseMember.STATUS_NORMAL);
        memberMapper.insert(m);
        return AjaxResult.success();
    }

    @Operation(summary = "移除成员")
    @DeleteMapping("/{memberUserId}")
    public AjaxResult remove(@PathVariable("warehouseId") Long warehouseId,
                             @PathVariable("memberUserId") Long memberUserId) {
        Long userId = PortalSecurityUtils.getUserId();
        LedgerWarehouse w = requireOwner(userId, warehouseId);
        if (memberUserId != null && memberUserId.equals(w.getUserId())) {
            throw new ServiceException("不能移除创建者");
        }
        memberMapper.delete(new LambdaQueryWrapper<LedgerWarehouseMember>()
                .eq(LedgerWarehouseMember::getWarehouseId, warehouseId)
                .eq(LedgerWarehouseMember::getUserId, memberUserId));
        return AjaxResult.success();
    }

    // ---------------- 内部 ----------------

    private LedgerWarehouse requireOwner(Long userId, Long warehouseId) {
        LedgerWarehouse w = warehouseMapper.selectById(warehouseId);
        if (w == null || LedgerWarehouse.STATUS_DELETED == w.getStatus()) {
            throw new ServiceException("仓库不存在");
        }
        if (!userId.equals(w.getUserId())) {
            throw new ServiceException("只有创建者能管理成员");
        }
        return w;
    }

    private Map<String, Object> build(PortalUser u, String role, boolean isOwner) {
        Map<String, Object> m = new HashMap<>();
        m.put("userId", u == null ? null : u.getId());
        m.put("name", u == null ? "（已注销）"
                : (u.getNickname() != null && !u.getNickname().isEmpty() ? u.getNickname() : u.getUsername()));
        m.put("phone", u == null ? null : u.getPhone());
        m.put("role", role);
        m.put("isOwner", isOwner);
        return m;
    }

    private String str(Object o) {
        return o == null ? null : String.valueOf(o).trim();
    }
}
