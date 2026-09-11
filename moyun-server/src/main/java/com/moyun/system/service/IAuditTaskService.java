package com.moyun.system.service;

import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.system.domain.dto.AuditTaskHandleDTO;
import com.moyun.system.domain.dto.AuditTaskSubmitDTO;
import com.moyun.system.domain.entity.SysAuditTask;
import com.moyun.system.domain.vo.AuditTaskVO;

/**
 * 统一审核任务服务（v8.1）
 * <p>
 * 替代分散的各业务表 status 聚合查询，提供统一的审核任务提交、查询、处理能力。
 * <p>
 * 设计要点：
 * <ul>
 *   <li>双写策略：业务表 status 保留作真实状态，sys_audit_task 作索引 + 记录；</li>
 *   <li>策略分发：处理时根据 task_type 路由到对应 {@link AuditBizHandler}；</li>
 *   <li>幂等保护：同一 biz 不重复入队；处理时校验状态必须为 pending。</li>
 * </ul>
 *
 * @author moyun
 */
public interface IAuditTaskService {

    /**
     * 提交审核任务（业务模块调用）。
     * <p>
     * 双写：插入 sys_audit_task（status=pending），业务表 status 已由业务方先行置为 pending。
     * 幂等：若同 task_type + biz_id 已存在，更新为 pending（支持驳回后重新提交）。
     *
     * @param dto 提交参数
     * @return 审核任务ID
     */
    Long submit(AuditTaskSubmitDTO dto);

    /**
     * 处理审核任务（审核中心调用）。
     * <p>
     * 双写：
     * <ol>
     *   <li>调用 {@link AuditBizHandler#approve} 或 {@link AuditBizHandler#reject} 更新业务表；</li>
     *   <li>更新 sys_audit_task 为终态（approved/rejected）+ 审核意见 + 处理人；</li>
     *   <li>按需向提交人发送站内信通知。</li>
     * </ol>
     *
     * @param dto 处理参数（taskId / action / auditOpinion / notifyUser）
     * @return 处理后的任务 VO
     */
    AuditTaskVO handle(AuditTaskHandleDTO dto);

    /**
     * 获取审核任务详情（含业务详情 bizDetail）。
     */
    AuditTaskVO getDetail(Long taskId);

    /**
     * 待办列表（status=pending），按优先级 + 提交时间排序。
     * <p>
     * 支持按 taskType 过滤（审核中心 Tab 切换）。
     *
     * @param query 查询条件（taskType 可空、title 模糊、submitterName 模糊）
     * @param page  分页参数
     */
    Page<AuditTaskVO> listPending(SysAuditTask query, Page<SysAuditTask> page);

    /**
     * 我的已办列表（auditor_id=当前用户），按处理时间倒序。
     */
    Page<AuditTaskVO> listMyHandled(SysAuditTask query, Page<SysAuditTask> page);

    /**
     * 全部任务列表（审核中心用，含 pending + 已处理）。
     */
    Page<AuditTaskVO> listAll(SysAuditTask query, Page<SysAuditTask> page);

    /**
     * 首页待办摘要（取前 N 条，按优先级 + 时间）。
     */
    List<AuditTaskVO> listTodoSummary(int limit);

    /**
     * 首页我的已办摘要（取前 N 条）。
     */
    List<AuditTaskVO> listMyHandledSummary(int limit);

    /**
     * 按任务类型统计待办数（审核中心 Tab 角标）。
     */
    Map<String, Long> countPendingByType();

    /**
     * 待办总数（首页角标）。
     */
    long countPending();

    /**
     * 注册业务处理器（Spring 启动时各 Handler Bean 自动注册）。
     */
    void registerHandler(AuditBizHandler handler);

    /**
     * 按业务类型 + 业务ID 同步审核任务状态（兼容旧入口直接审核场景）。
     * <p>
     * 当业务模块通过自己的 Controller 直接审核（非走统一审核中心 handle 流程）时，
     * 调用此方法同步更新 sys_audit_task 为终态，避免审核中心/首页待办残留。
     * <p>
     * 幂等：仅当任务存在且 status=pending 时才更新，已处理的任务不受影响。
     *
     * @param taskType    任务类型（article / certification / topic 等）
     * @param bizId       业务ID
     * @param finalStatus 终态：approved / rejected
     * @param auditorId   审核人ID
     * @param auditorName 审核人名称
     * @param opinion     审核意见
     */
    void syncTaskStatusByBiz(String taskType, Long bizId, String finalStatus,
                             Long auditorId, String auditorName, String opinion);
}
