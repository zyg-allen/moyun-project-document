package com.moyun.ext.cms.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.ext.cms.domain.vo.VoiceInterviewVO;
import com.moyun.ext.cms.service.IVoiceInterviewService;
import com.moyun.portal.domain.entity.PortalUser;
import com.moyun.portal.domain.entity.PortalVoiceInterview;
import com.moyun.portal.mapper.PortalUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 语音面试管理 Controller（Admin 端复盘，v11.30 补建）
 *
 * <p>前端 views/cms/voiceInterview 调用 /cms/voice-interview/*；
 * 菜单 5253-5256（20260901-moyun-voice-interview-menu.sql），权限 cms:voiceInterview:*。
 * 只读复盘 + 删除，不含出题/答题操作。</p>
 *
 * @author moyun
 */
@Tag(name = "语音面试管理")
@RestController
@RequestMapping("/cms/voice-interview")
public class CmsVoiceInterviewController extends BaseController {

    @Autowired
    private IVoiceInterviewService voiceInterviewService;

    @Autowired
    private PortalUserMapper portalUserMapper;

    @Operation(summary = "语音面试分页列表（所有用户）")
    @PreAuthorize("@ss.hasPermi('cms:voiceInterview:list')")
    @GetMapping("/list")
    public com.moyun.core.base.TableDataInfo list(@RequestParam(required = false) String username,
                           @RequestParam(required = false) String position,
                           @RequestParam(required = false) String status,
                           @RequestParam(defaultValue = "1") Integer pageNum,
                           @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<PortalVoiceInterview> page = voiceInterviewService
                .adminList(username, position, status, pageNum, pageSize);
        // 前端读取 rows/total（TableDataInfo 结构，与 request 拦截器对齐）
        com.moyun.core.base.TableDataInfo table = new com.moyun.core.base.TableDataInfo();
        table.setCode(200);
        table.setMsg("查询成功");
        table.setRows(fillUsername(page.getRecords()));
        table.setTotal(page.getTotal());
        return table;
    }

    @Operation(summary = "语音面试详情（含 qaList，管理端不校验归属）")
    @PreAuthorize("@ss.hasPermi('cms:voiceInterview:query')")
    @GetMapping("/{id:[0-9]+}")
    public AjaxResult getDetail(@PathVariable("id") Long id) {
        VoiceInterviewVO vo = voiceInterviewService.adminGetDetail(id);
        // 补充用户名（管理端复盘展示）
        if (vo != null && vo.getUserId() != null) {
            PortalUser user = portalUserMapper.selectOne(Wrappers.<PortalUser>lambdaQuery()
                    .select(PortalUser::getId, PortalUser::getUsername, PortalUser::getNickname)
                    .eq(PortalUser::getId, vo.getUserId())
                    .last("LIMIT 1"));
            if (user != null) {
                vo.setUsername(user.getUsername());
            }
        }
        return success(vo);
    }

    @Operation(summary = "删除语音面试会话（逻辑删除）")
    @PreAuthorize("@ss.hasPermi('cms:voiceInterview:remove')")
    @DeleteMapping("/{id:[0-9]+}")
    public AjaxResult remove(@PathVariable("id") Long id) {
        return toAjax(voiceInterviewService.adminDelete(id));
    }

    /** 批量填充用户名（列表展示）：userId → username */
    private List<Map<String, Object>> fillUsername(List<PortalVoiceInterview> records) {
        if (records == null || records.isEmpty()) {
            return new ArrayList<>();
        }
        Set<Long> userIds = records.stream()
                .map(PortalVoiceInterview::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> nameMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            portalUserMapper.selectList(Wrappers.<PortalUser>lambdaQuery()
                            .select(PortalUser::getId, PortalUser::getUsername)
                            .in(PortalUser::getId, userIds))
                    .forEach(u -> nameMap.put(u.getId(), u.getUsername()));
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        for (PortalVoiceInterview it : records) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", it.getId());
            row.put("userId", it.getUserId());
            row.put("username", nameMap.get(it.getUserId()));
            row.put("position", it.getPosition());
            row.put("scene", it.getScene());
            row.put("phase", it.getPhase());
            row.put("status", it.getStatus());
            row.put("style", it.getStyle());
            row.put("difficulty", it.getDifficulty());
            row.put("totalQa", it.getTotalQa());
            row.put("currentIdx", it.getCurrentIdx());
            row.put("score", it.getScore());
            row.put("summary", it.getSummary());
            row.put("isPersonalized", it.getIsPersonalized());
            row.put("agentId", it.getAgentId());
            row.put("resumeId", it.getResumeId());
            row.put("createTime", it.getCreateTime());
            rows.add(row);
        }
        return rows;
    }
}