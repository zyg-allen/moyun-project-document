package com.moyun.system.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.common.exception.system.ServiceException;
import com.moyun.util.security.SecurityUtils;
import com.moyun.system.domain.dto.AuditTaskHandleDTO;
import com.moyun.system.domain.dto.AuditTaskSubmitDTO;
import com.moyun.system.domain.entity.SysAuditTask;
import com.moyun.system.domain.vo.AuditTaskVO;
import com.moyun.system.enums.AuditAction;
import com.moyun.system.enums.AuditTaskStatus;
import com.moyun.system.enums.AuditTaskType;
import com.moyun.system.mapper.SysAuditTaskMapper;
import com.moyun.system.service.IAuditTaskService;
import com.moyun.system.service.AuditBizHandler;

/**
 * 统一审核任务服务实现（v8.1）
 * <p>
 * 核心职责：
 * <ol>
 *   <li>submit：业务方提交审核，写入 sys_audit_task（status=pending）；</li>
 *   <li>handle：审核中心处理任务，根据 task_type 分发到对应 {@link AuditBizHandler}，
 *       双写业务表 + sys_audit_task 终态；</li>
 *   <li>list*：统一查询入口，供审核中心/首页待办/我的已办使用。</li>
 * </ol>
 *
 * @author moyun
 */
@Service
public class AuditTaskServiceImpl implements IAuditTaskService {

    private static final Logger log = LoggerFactory.getLogger(AuditTaskServiceImpl.class);

    @Autowired
    private SysAuditTaskMapper auditTaskMapper;

    /** 所有审核业务处理器（Spring 自动注入所有 AuditBizHandler 实现 Bean） */
    @Autowired
    private List<AuditBizHandler> handlers;

    /** task_type → Handler 映射（Spring 启动时各 Handler Bean 自动注册） */
    private final Map<String, AuditBizHandler> handlerMap = new ConcurrentHashMap<>();

    /**
     * 启动时自动注册所有 AuditBizHandler 实现 Bean。
     */
    @PostConstruct
    public void initHandlers() {
        if (handlers == null || handlers.isEmpty()) {
            log.warn("[AuditTask] 未发现任何 AuditBizHandler 实现 Bean");
            return;
        }
        for (AuditBizHandler handler : handlers) {
            registerHandler(handler);
        }
    }

    @Override
    public void registerHandler(AuditBizHandler handler) {
        if (handler == null || handler.supportedTaskType() == null) {
            return;
        }
        handlerMap.put(handler.supportedTaskType(), handler);
        log.info("[AuditTask] 注册审核业务处理器 taskType={} handler={}",
                handler.supportedTaskType(), handler.getClass().getSimpleName());
    }

    // ==================== 提交审核 ====================

    @Override
    public Long submit(AuditTaskSubmitDTO dto) {
        if (dto == null || dto.getTaskType() == null || dto.getBizId() == null) {
            throw new ServiceException("审核任务提交参数非法");
        }
        AuditTaskType type = AuditTaskType.fromCode(dto.getTaskType());
        if (type == null) {
            throw new ServiceException("不支持的任务类型: " + dto.getTaskType());
        }

        // 幂等：同 task_type + biz_id 已存在则更新为 pending（支持驳回后重新提交）
        SysAuditTask existing = auditTaskMapper.selectByBiz(dto.getTaskType(), dto.getBizId());
        LocalDateTime now = LocalDateTime.now();
        if (existing != null) {
            existing.setStatus(AuditTaskStatus.PENDING.getCode());
            existing.setAuditOpinion(null);
            existing.setAuditAction(null);
            existing.setAuditorId(null);
            existing.setAuditorName(null);
            existing.setAuditTime(null);
            existing.setSubmitterId(dto.getSubmitterId());
            existing.setSubmitterName(dto.getSubmitterName());
            existing.setTitle(resolveTitle(dto, type));
            existing.setDescription(dto.getDescription());
            existing.setBizType(dto.getBizType());
            existing.setPriority(dto.getPriority() != null ? dto.getPriority() : "medium");
            existing.setRoutePath(dto.getRoutePath() != null ? dto.getRoutePath() : type.getDefaultRoutePath());
            existing.setExtraData(dto.getExtraData());
            existing.setSubmitTime(now);
            existing.setUpdateTime(now);
            auditTaskMapper.updateById(existing);
            log.info("[AuditTask] 重新提交审核 taskId={} taskType={} bizId={}",
                    existing.getId(), dto.getTaskType(), dto.getBizId());
            return existing.getId();
        }

        SysAuditTask task = new SysAuditTask();
        task.setTaskType(dto.getTaskType());
        task.setBizType(dto.getBizType());
        task.setBizId(dto.getBizId());
        task.setTitle(resolveTitle(dto, type));
        task.setDescription(dto.getDescription());
        task.setSubmitterId(dto.getSubmitterId());
        task.setSubmitterName(dto.getSubmitterName());
        task.setStatus(AuditTaskStatus.PENDING.getCode());
        task.setPriority(dto.getPriority() != null ? dto.getPriority() : "medium");
        task.setRoutePath(dto.getRoutePath() != null ? dto.getRoutePath() : type.getDefaultRoutePath());
        task.setExtraData(dto.getExtraData());
        task.setSubmitTime(now);
        task.setCreateTime(now);
        task.setUpdateTime(now);
        auditTaskMapper.insert(task);
        log.info("[AuditTask] 提交审核 taskId={} taskType={} bizId={} submitter={}",
                task.getId(), dto.getTaskType(), dto.getBizId(), dto.getSubmitterName());
        return task.getId();
    }

    private String resolveTitle(AuditTaskSubmitDTO dto, AuditTaskType type) {
        if (dto.getTitle() != null && !dto.getTitle().isBlank()) {
            return dto.getTitle();
        }
        return type.getDisplayName() + " #" + dto.getBizId();
    }

    // ==================== 处理审核 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AuditTaskVO handle(AuditTaskHandleDTO dto) {
        if (dto == null || dto.getTaskId() == null) {
            throw new ServiceException("审核任务ID不能为空");
        }
        AuditAction action = AuditAction.fromCode(dto.getAction());
        if (action == null) {
            throw new ServiceException("审核操作类型非法: " + dto.getAction());
        }
        // 驳回时审核意见必填
        if (action == AuditAction.REJECT) {
            if (dto.getAuditOpinion() == null || dto.getAuditOpinion().isBlank()) {
                throw new ServiceException("驳回时审核意见原因必填");
            }
        }

        SysAuditTask task = auditTaskMapper.selectById(dto.getTaskId());
        if (task == null) {
            throw new ServiceException("审核任务不存在");
        }
        if (!AuditTaskStatus.PENDING.getCode().equals(task.getStatus())) {
            throw new ServiceException("任务已处理，请勿重复操作");
        }

        AuditBizHandler handler = handlerMap.get(task.getTaskType());
        if (handler == null) {
            throw new ServiceException("未找到任务类型处理器: " + task.getTaskType());
        }

        Long auditorId = SecurityUtils.getUserId();
        String auditorName = SecurityUtils.getUsername();
        String opinion = dto.getAuditOpinion();

        // 1. 分发到业务处理器（更新业务表 + 后置动作）
        try {
            if (action == AuditAction.APPROVE) {
                handler.approve(task.getBizId(), auditorId, auditorName, opinion);
            } else {
                handler.reject(task.getBizId(), auditorId, auditorName, opinion);
            }
        } catch (Exception e) {
            log.error("[AuditTask] 业务处理器异常 taskId={} taskType={} action={} err={}",
                    task.getId(), task.getTaskType(), action.getCode(), e.getMessage(), e);
            throw new ServiceException("审核处理失败: " + e.getMessage());
        }

        // 2. 更新 sys_audit_task 为终态
        AuditTaskStatus finalStatus = (action == AuditAction.APPROVE)
                ? AuditTaskStatus.APPROVED : AuditTaskStatus.REJECTED;
        LocalDateTime now = LocalDateTime.now();
        task.setStatus(finalStatus.getCode());
        task.setAuditorId(auditorId);
        task.setAuditorName(auditorName);
        task.setAuditOpinion(opinion);
        task.setAuditAction(action.getCode());
        task.setAuditTime(now);
        task.setUpdateTime(now);
        auditTaskMapper.updateById(task);

        log.info("[AuditTask] 审核完成 taskId={} taskType={} bizId={} action={} auditor={}",
                task.getId(), task.getTaskType(), task.getBizId(), action.getCode(), auditorName);

        return toVO(task, null);
    }

    // ==================== 查询 ====================

    @Override
    public AuditTaskVO getDetail(Long taskId) {
        if (taskId == null) {
            throw new ServiceException("任务ID不能为空");
        }
        SysAuditTask task = auditTaskMapper.selectById(taskId);
        if (task == null) {
            throw new ServiceException("审核任务不存在");
        }
        Map<String, Object> bizDetail = null;
        AuditBizHandler handler = handlerMap.get(task.getTaskType());
        if (handler != null) {
            try {
                bizDetail = handler.getBizDetail(task.getBizId());
            } catch (Exception e) {
                log.warn("[AuditTask] 获取业务详情失败 taskId={} err={}", taskId, e.getMessage());
            }
        }
        return toVO(task, bizDetail);
    }

    @Override
    public Page<AuditTaskVO> listPending(SysAuditTask query, Page<SysAuditTask> page) {
        return queryPage(buildWrapper(query, true), page, false);
    }

    @Override
    public Page<AuditTaskVO> listMyHandled(SysAuditTask query, Page<SysAuditTask> page) {
        Long userId = SecurityUtils.getUserId();
        LambdaQueryWrapper<SysAuditTask> wrapper = buildWrapper(query, false);
        wrapper.eq(SysAuditTask::getAuditorId, userId);
        wrapper.in(SysAuditTask::getStatus,
                AuditTaskStatus.APPROVED.getCode(), AuditTaskStatus.REJECTED.getCode());
        return queryPage(wrapper, page, false);
    }

    @Override
    public Page<AuditTaskVO> listAll(SysAuditTask query, Page<SysAuditTask> page) {
        return queryPage(buildWrapper(query, false), page, false);
    }

    @Override
    public List<AuditTaskVO> listTodoSummary(int limit) {
        LambdaQueryWrapper<SysAuditTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysAuditTask::getStatus, AuditTaskStatus.PENDING.getCode())
                .orderByDesc(SysAuditTask::getPriority)
                .orderByAsc(SysAuditTask::getSubmitTime)
                .last("LIMIT " + Math.max(1, limit));
        List<SysAuditTask> list = auditTaskMapper.selectList(wrapper);
        List<AuditTaskVO> result = new ArrayList<>(list.size());
        for (SysAuditTask t : list) {
            result.add(toVO(t, null));
        }
        return result;
    }

    @Override
    public List<AuditTaskVO> listMyHandledSummary(int limit) {
        Long userId = SecurityUtils.getUserId();
        LambdaQueryWrapper<SysAuditTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysAuditTask::getAuditorId, userId)
                .in(SysAuditTask::getStatus,
                        AuditTaskStatus.APPROVED.getCode(), AuditTaskStatus.REJECTED.getCode())
                .orderByDesc(SysAuditTask::getAuditTime)
                .last("LIMIT " + Math.max(1, limit));
        List<SysAuditTask> list = auditTaskMapper.selectList(wrapper);
        List<AuditTaskVO> result = new ArrayList<>(list.size());
        for (SysAuditTask t : list) {
            result.add(toVO(t, null));
        }
        return result;
    }

    @Override
    public Map<String, Long> countPendingByType() {
        List<Map<String, Object>> rows = auditTaskMapper.countPendingByType();
        Map<String, Long> result = new HashMap<>();
        if (rows != null) {
            for (Map<String, Object> row : rows) {
                Object type = row.get("taskType");
                Object cnt = row.get("cnt");
                if (type != null && cnt != null) {
                    result.put(type.toString(), ((Number) cnt).longValue());
                }
            }
        }
        return result;
    }

    @Override
    public long countPending() {
        LambdaQueryWrapper<SysAuditTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysAuditTask::getStatus, AuditTaskStatus.PENDING.getCode());
        return auditTaskMapper.selectCount(wrapper);
    }

    // ==================== 内部工具 ====================

    private LambdaQueryWrapper<SysAuditTask> buildWrapper(SysAuditTask query, boolean pendingOnly) {
        LambdaQueryWrapper<SysAuditTask> wrapper = new LambdaQueryWrapper<>();
        if (query != null) {
            if (query.getTaskType() != null && !query.getTaskType().isBlank()) {
                wrapper.eq(SysAuditTask::getTaskType, query.getTaskType());
            }
            if (query.getBizType() != null && !query.getBizType().isBlank()) {
                wrapper.eq(SysAuditTask::getBizType, query.getBizType());
            }
            if (query.getStatus() != null && !query.getStatus().isBlank()) {
                wrapper.eq(SysAuditTask::getStatus, query.getStatus());
            }
            if (query.getTitle() != null && !query.getTitle().isBlank()) {
                wrapper.like(SysAuditTask::getTitle, query.getTitle());
            }
            if (query.getSubmitterName() != null && !query.getSubmitterName().isBlank()) {
                wrapper.like(SysAuditTask::getSubmitterName, query.getSubmitterName());
            }
        }
        if (pendingOnly) {
            wrapper.eq(SysAuditTask::getStatus, AuditTaskStatus.PENDING.getCode());
        }
        return wrapper;
    }

    private Page<AuditTaskVO> queryPage(LambdaQueryWrapper<SysAuditTask> wrapper,
                                         Page<SysAuditTask> page, boolean withBizDetail) {
        // 排序：待办按优先级+提交时间；已办按处理时间倒序
        if (wrapper.getSqlSegment() == null || !wrapper.getSqlSegment().contains("ORDER")) {
            wrapper.orderByDesc(SysAuditTask::getSubmitTime);
        }
        Page<SysAuditTask> result = auditTaskMapper.selectPage(page, wrapper);
        Page<AuditTaskVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        List<AuditTaskVO> voList = new ArrayList<>(result.getRecords().size());
        for (SysAuditTask t : result.getRecords()) {
            Map<String, Object> bizDetail = null;
            if (withBizDetail) {
                AuditBizHandler handler = handlerMap.get(t.getTaskType());
                if (handler != null) {
                    try {
                        bizDetail = handler.getBizDetail(t.getBizId());
                    } catch (Exception e) {
                        log.debug("[AuditTask] 列表批量获取详情失败 taskId={} err={}", t.getId(), e.getMessage());
                    }
                }
            }
            voList.add(toVO(t, bizDetail));
        }
        voPage.setRecords(voList);
        return voPage;
    }

    private AuditTaskVO toVO(SysAuditTask task, Map<String, Object> bizDetail) {
        if (task == null) {
            return null;
        }
        AuditTaskVO vo = new AuditTaskVO();
        vo.setId(task.getId());
        vo.setTaskType(task.getTaskType());
        AuditTaskType type = AuditTaskType.fromCode(task.getTaskType());
        vo.setTaskTypeLabel(type != null ? type.getDisplayName() : task.getTaskType());
        vo.setBizType(task.getBizType());
        vo.setBizId(task.getBizId());
        vo.setTitle(task.getTitle());
        vo.setDescription(task.getDescription());
        vo.setSubmitterName(task.getSubmitterName());
        vo.setStatus(task.getStatus());
        AuditTaskStatus status = AuditTaskStatus.fromCode(task.getStatus());
        vo.setStatusLabel(status.getDisplayName());
        vo.setAuditorName(task.getAuditorName());
        vo.setAuditOpinion(task.getAuditOpinion());
        vo.setAuditAction(task.getAuditAction());
        vo.setSubmitTime(task.getSubmitTime());
        vo.setAuditTime(task.getAuditTime());
        vo.setPriority(task.getPriority());
        vo.setPriorityLabel(priorityLabel(task.getPriority()));
        vo.setRoutePath(task.getRoutePath());
        vo.setBizDetail(bizDetail);
        return vo;
    }

    private String priorityLabel(String priority) {
        if (priority == null) return "普通";
        return switch (priority) {
            case "high" -> "紧急";
            case "low" -> "低";
            default -> "普通";
        };
    }
}
