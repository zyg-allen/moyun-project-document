package com.moyun.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.common.annotation.Log;
import com.moyun.common.enums.BusinessType;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.core.base.TableDataInfo;
import com.moyun.system.domain.dto.AuditTaskHandleDTO;
import com.moyun.system.domain.entity.SysAuditTask;
import com.moyun.system.domain.vo.AuditTaskVO;
import com.moyun.system.service.IAuditTaskService;
import com.moyun.util.bean.PageUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 统一审核任务 Controller（v8.1）
 * <p>
 * 审核中心、首页待办/已办、我的待办/已办的统一入口。
 * <ul>
 *   <li>{@code /system/audit-task/pending} 待办列表（审核中心 / 首页「更多」）；</li>
 *   <li>{@code /system/audit-task/my-handled} 我的已办（按当前处理人过滤）；</li>
 *   <li>{@code /system/audit-task/list} 全部任务（含 pending + 已处理，审核中心「全部」Tab）；</li>
 *   <li>{@code /system/audit-task/{id}} 任务详情（含业务详情 bizDetail）；</li>
 *   <li>{@code /system/audit-task/handle} 处理任务（同意/驳回，驳回时原因必填）；</li>
 *   <li>{@code /system/audit-task/todo-summary} 首页待办摘要（前 N 条）；</li>
 *   <li>{@code /system/audit-task/my-handled-summary} 首页我的已办摘要（前 N 条）；</li>
 *   <li>{@code /system/audit-task/count-pending} 待办总数（首页角标）；</li>
 *   <li>{@code /system/audit-task/count-by-type} 按类型统计待办数（审核中心 Tab 角标）。</li>
 * </ul>
 *
 * @author moyun
 */
@Tag(name = "统一审核任务", description = "审核中心：待办/已办/全部/处理，首页待办摘要")
@RestController
@RequestMapping("/system/audit-task")
public class AuditTaskController extends BaseController {

    @Autowired
    private IAuditTaskService auditTaskService;

    /**
     * 待办列表（status=pending），审核中心 / 首页「更多」入口
     */
    @Operation(summary = "待办列表", description = "分页查询待审核任务，支持按 taskType / bizType / title / submitterName 过滤")
    @PreAuthorize("@ss.hasPermi('system:auditTask:list')")
    @GetMapping("/pending")
    public TableDataInfo pending(SysAuditTask query) {
        Page<SysAuditTask> page = PageUtils.startPage();
        Page<AuditTaskVO> result = auditTaskService.listPending(query, page);
        TableDataInfo rsp = new TableDataInfo();
        rsp.setCode(200);
        rsp.setMsg("查询成功");
        rsp.setRows(result.getRecords());
        rsp.setTotal(result.getTotal());
        return rsp;
    }

    /**
     * 我的已办列表（auditor_id=当前用户）
     */
    @Operation(summary = "我的已办列表", description = "分页查询当前用户已处理的审核任务")
    @PreAuthorize("@ss.hasPermi('system:auditTask:list')")
    @GetMapping("/my-handled")
    public TableDataInfo myHandled(SysAuditTask query) {
        Page<SysAuditTask> page = PageUtils.startPage();
        Page<AuditTaskVO> result = auditTaskService.listMyHandled(query, page);
        TableDataInfo rsp = new TableDataInfo();
        rsp.setCode(200);
        rsp.setMsg("查询成功");
        rsp.setRows(result.getRecords());
        rsp.setTotal(result.getTotal());
        return rsp;
    }

    /**
     * 全部任务列表（含 pending + 已处理），审核中心「全部」Tab
     */
    @Operation(summary = "全部任务列表", description = "分页查询所有审核任务（待办+已处理），支持按 taskType / status 过滤")
    @PreAuthorize("@ss.hasPermi('system:auditTask:list')")
    @GetMapping("/list")
    public TableDataInfo list(SysAuditTask query) {
        Page<SysAuditTask> page = PageUtils.startPage();
        Page<AuditTaskVO> result = auditTaskService.listAll(query, page);
        TableDataInfo rsp = new TableDataInfo();
        rsp.setCode(200);
        rsp.setMsg("查询成功");
        rsp.setRows(result.getRecords());
        rsp.setTotal(result.getTotal());
        return rsp;
    }

    /**
     * 获取任务详情（含业务详情 bizDetail，供审核中心详情弹窗展示）
     */
    @Operation(summary = "获取任务详情", description = "根据任务ID获取审核任务详情，含业务详情 bizDetail")
    @PreAuthorize("@ss.hasPermi('system:auditTask:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        return success(auditTaskService.getDetail(id));
    }

    /**
     * 处理审核任务（同意/驳回）
     * <p>驳回时 {@code auditOpinion} 必填。
     */
    @Operation(summary = "处理审核任务", description = "同意/驳回审核任务，驳回时审核意见原因必填")
    @PreAuthorize("@ss.hasPermi('system:auditTask:handle')")
    @Log(title = "统一审核任务", businessType = BusinessType.UPDATE)
    @PostMapping("/handle")
    public AjaxResult handle(@RequestBody AuditTaskHandleDTO dto) {
        AuditTaskVO vo = auditTaskService.handle(dto);
        return success(vo);
    }

    /**
     * 首页待办摘要（默认取前 5 条，按优先级 + 提交时间排序）
     */
    @Operation(summary = "首页待办摘要", description = "返回前 N 条待办任务（默认5条），首页待办卡片展示")
    @GetMapping("/todo-summary")
    public AjaxResult todoSummary(@RequestParam(defaultValue = "5") int limit) {
        List<AuditTaskVO> list = auditTaskService.listTodoSummary(limit);
        Map<String, Object> data = new HashMap<>();
        data.put("list", list);
        data.put("total", auditTaskService.countPending());
        return success(data);
    }

    /**
     * 首页我的已办摘要（默认取前 5 条）
     */
    @Operation(summary = "首页我的已办摘要", description = "返回当前用户前 N 条已处理任务（默认5条）")
    @GetMapping("/my-handled-summary")
    public AjaxResult myHandledSummary(@RequestParam(defaultValue = "5") int limit) {
        List<AuditTaskVO> list = auditTaskService.listMyHandledSummary(limit);
        return success(list);
    }

    /**
     * 待办总数（首页角标）
     */
    @Operation(summary = "待办总数", description = "返回待审核任务总数，用于首页角标")
    @GetMapping("/count-pending")
    public AjaxResult countPending() {
        return success(auditTaskService.countPending());
    }

    /**
     * 按任务类型统计待办数（审核中心 Tab 角标）
     */
    @Operation(summary = "按类型统计待办数", description = "返回各任务类型的待办数量，用于审核中心 Tab 角标")
    @GetMapping("/count-by-type")
    public AjaxResult countByType() {
        return success(auditTaskService.countPendingByType());
    }
}
