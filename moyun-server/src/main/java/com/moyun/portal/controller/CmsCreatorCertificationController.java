package com.moyun.portal.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.common.annotation.Log;
import com.moyun.common.enums.BusinessType;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.portal.domain.entity.PortalCreatorCertification;
import com.moyun.portal.domain.entity.PortalUser;
import com.moyun.portal.mapper.PortalUserMapper;
import com.moyun.portal.service.IPortalCreatorCertificationService;
import com.moyun.util.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 创作者认证 后台 Controller
 *
 * <p>供后台管理页面调用，提供分页查询与审核能力。</p>
 *
 * <p>路径前缀 /cms/creator/certification，权限标识 cms:certification:audit。</p>
 *
 * @author moyun
 */
@Tag(name = "创作者认证管理", description = "创作者认证后台审核接口")
@RestController
@RequestMapping("/cms/creator/certification")
public class CmsCreatorCertificationController extends BaseController {

    @Autowired
    private IPortalCreatorCertificationService certificationService;

    @Autowired
    private PortalUserMapper portalUserMapper;

    @Operation(summary = "认证申请分页列表", description = "分页查询认证申请，支持按状态、类型、用户ID、真实姓名筛选")
    @PreAuthorize("@ss.hasPermi('cms:certification:audit')")
    @GetMapping("/list")
    public AjaxResult list(PortalCreatorCertification query,
                            @RequestParam(defaultValue = "1") Integer pageNum,
                            @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<PortalCreatorCertification> page = new Page<>(pageNum, pageSize);
        Page<PortalCreatorCertification> result = certificationService.list(query, page);
        // 填充申请人昵称，便于后台展示
        Page<Map<String, Object>> resultPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        java.util.List<Map<String, Object>> records = new java.util.ArrayList<>(result.getRecords().size());

        // 批量查询申请人昵称，避免 N+1（分页 N 条原本 N 次查询 → 现在 1 次）
        java.util.List<Long> userIds = result.getRecords().stream()
                .map(PortalCreatorCertification::getUserId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, String> nicknameMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            List<PortalUser> users = portalUserMapper.selectBatchIds(userIds);
            for (PortalUser u : users) {
                nicknameMap.put(u.getId(), u.getNickname());
            }
        }

        for (PortalCreatorCertification c : result.getRecords()) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", c.getId());
            map.put("userId", c.getUserId());
            map.put("realName", c.getRealName());
            map.put("certType", c.getCertType());
            map.put("certNo", c.getCertNo());
            map.put("certImage", c.getCertImage());
            map.put("intro", c.getIntro());
            map.put("works", c.getWorks());
            map.put("status", c.getStatus());
            map.put("auditorId", c.getAuditorId());
            map.put("auditRemark", c.getAuditRemark());
            map.put("createdTime", c.getCreatedTime());
            map.put("auditedTime", c.getAuditedTime());
            map.put("nickname", c.getUserId() == null ? null : nicknameMap.get(c.getUserId()));
            records.add(map);
        }
        resultPage.setRecords(records);
        return success(resultPage);
    }

    @Operation(summary = "认证申请详情", description = "按ID查询单条认证申请，含申请人昵称")
    @PreAuthorize("@ss.hasPermi('cms:certification:audit')")
    @GetMapping("/{id}")
    public AjaxResult detail(@Parameter(description = "认证申请ID") @PathVariable Long id) {
        PortalCreatorCertification c = certificationService.getById(id);
        if (c == null) {
            return AjaxResult.error("认证申请不存在");
        }
        Map<String, Object> map = new HashMap<>();
        map.put("id", c.getId());
        map.put("userId", c.getUserId());
        map.put("realName", c.getRealName());
        map.put("certType", c.getCertType());
        map.put("certNo", c.getCertNo());
        map.put("certImage", c.getCertImage());
        map.put("intro", c.getIntro());
        map.put("works", c.getWorks());
        map.put("status", c.getStatus());
        map.put("auditorId", c.getAuditorId());
        map.put("auditRemark", c.getAuditRemark());
        map.put("createdTime", c.getCreatedTime());
        map.put("auditedTime", c.getAuditedTime());
        if (c.getUserId() != null) {
            PortalUser u = portalUserMapper.selectPortalUserById(c.getUserId());
            map.put("nickname", u != null ? u.getNickname() : null);
        } else {
            map.put("nickname", null);
        }
        return AjaxResult.success(map);
    }

    @Operation(summary = "审核认证申请", description = "通过或驳回认证申请，status=approved/rejected")
    @PreAuthorize("@ss.hasPermi('cms:certification:audit')")
    @Log(title = "创作者认证", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}/audit")
    public AjaxResult audit(@Parameter(description = "认证申请ID") @PathVariable Long id,
                            @RequestBody Map<String, Object> body) {
        Long auditorId = SecurityUtils.getUserId();
        String status = body.get("status") == null ? null : String.valueOf(body.get("status"));
        String remark = body.get("remark") == null ? null : String.valueOf(body.get("remark"));
        try {
            return AjaxResult.success(certificationService.audit(id, auditorId, status, remark));
        } catch (RuntimeException e) {
            return AjaxResult.error(e.getMessage());
        }
    }
}
