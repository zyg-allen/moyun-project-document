package com.moyun.ledger.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.moyun.core.base.BaseController;
import com.moyun.core.base.TableDataInfo;
import com.moyun.ledger.domain.entity.PortalDataAuth;
import com.moyun.ledger.domain.entity.SysDataAccessLog;
import com.moyun.ledger.mapper.PortalDataAuthMapper;
import com.moyun.ledger.mapper.SysDataAccessLogMapper;
import com.moyun.ledger.service.ILedgerDataAuthService;
import com.moyun.portal.domain.entity.PortalUser;
import com.moyun.portal.mapper.PortalUserMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 后台-用户数据授权与访问审计（只读）
 *
 * <p><b>后台不提供写入授权的能力</b>：授权只能由用户在 App 内主动发起/撤回，
 * 后台若可代授权，"用户授权"就失去合规意义。本控制器只做两件事：
 * 查看授权记录、查看访问日志。</p>
 *
 * @author moyun
 */
@Tag(name = "CMS记账-授权与审计", description = "用户数据授权记录 + 敏感数据访问日志（只读）")
@RestController
@RequestMapping("/cms/ledger")
public class CmsLedgerDataAuthController extends BaseController {

    @Autowired
    private PortalDataAuthMapper authMapper;

    @Autowired
    private SysDataAccessLogMapper accessLogMapper;

    @Autowired
    private PortalUserMapper portalUserMapper;

    @Autowired
    private ILedgerDataAuthService dataAuthService;

    @Operation(summary = "授权记录列表（可按用户/状态筛选；后台只读）")
    @PreAuthorize("@ss.hasPermi('cms:ledgerAuth:list')")
    @GetMapping("/auth/list")
    public TableDataInfo authList(@RequestParam(required = false) Long userId,
                                  @RequestParam(required = false) String status,
                                  @RequestParam(defaultValue = "1") Integer pageNum,
                                  @RequestParam(defaultValue = "10") Integer pageSize) {
        startPage();
        QueryWrapper<PortalDataAuth> qw = new QueryWrapper<>();
        qw.eq("biz_domain", PortalDataAuth.DOMAIN_LEDGER);
        if (userId != null) {
            qw.eq("user_id", userId);
        }
        LocalDateTime now = LocalDateTime.now();
        if ("valid".equals(status)) {
            qw.isNull("revoked_at").and(w -> w.isNull("expire_at").or().gt("expire_at", now));
        } else if ("expired".equals(status)) {
            qw.isNull("revoked_at").isNotNull("expire_at").le("expire_at", now);
        } else if ("revoked".equals(status)) {
            qw.isNotNull("revoked_at");
        }
        qw.orderByDesc("granted_at");

        List<PortalDataAuth> list = authMapper.selectList(qw);
        Map<Long, String> nicknames = maskNicknames(list.stream().map(PortalDataAuth::getUserId).toList());

        List<Map<String, Object>> rows = list.stream().map(a -> {
            Map<String, Object> row = new HashMap<>();
            row.put("id", a.getId());
            row.put("userId", a.getUserId());
            row.put("nickname", nicknames.getOrDefault(a.getUserId(), "用户#" + a.getUserId()));
            row.put("scopes", a.getScopes());
            row.put("purpose", a.getPurpose());
            row.put("channel", a.getChannel());
            row.put("grantType", a.getGrantType());
            row.put("grantedAt", format(a.getGrantedAt()));
            row.put("expireAt", format(a.getExpireAt()));
            row.put("revokedAt", format(a.getRevokedAt()));
            row.put("status", dataAuthService.statusOf(a));
            return row;
        }).toList();
        return getDataTable(rows);
    }

    @Operation(summary = "敏感数据访问日志（谁在何时看了谁的什么数据）")
    @PreAuthorize("@ss.hasPermi('cms:ledgerAccessLog:list')")
    @GetMapping("/access-log/list")
    public TableDataInfo accessLogList(@RequestParam(required = false) String operatorName,
                                       @RequestParam(required = false) Long targetUserId,
                                       @RequestParam(required = false) String result,
                                       @RequestParam(defaultValue = "1") Integer pageNum,
                                       @RequestParam(defaultValue = "10") Integer pageSize) {
        startPage();
        QueryWrapper<SysDataAccessLog> qw = new QueryWrapper<>();
        if (operatorName != null && !operatorName.isBlank()) {
            qw.like("operator_name", operatorName.trim());
        }
        if (targetUserId != null) {
            qw.eq("target_user_id", targetUserId);
        }
        if ("allowed".equals(result) || "denied".equals(result)) {
            qw.eq("result", result);
        }
        qw.orderByDesc("access_time");
        return getDataTable(accessLogMapper.selectList(qw));
    }

    /** 昵称脱敏：首尾保留 1 字符 */
    private Map<Long, String> maskNicknames(List<Long> userIds) {
        List<Long> ids = userIds.stream().filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return portalUserMapper.selectList(new QueryWrapper<PortalUser>()
                        .select("id", "nickname", "username").in("id", ids))
                .stream()
                .collect(Collectors.toMap(PortalUser::getId,
                        u -> mask(u.getNickname() != null && !u.getNickname().isBlank()
                                ? u.getNickname() : u.getUsername()),
                        (a, b) -> a));
    }

    private String mask(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        if (text.length() <= 2) {
            return "**";
        }
        return text.charAt(0) + "*".repeat(text.length() - 2) + text.charAt(text.length() - 1);
    }

    private String format(LocalDateTime time) {
        return time == null ? null : time.toString().replace('T', ' ');
    }
}
