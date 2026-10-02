package com.moyun.system.service.handler;

import com.moyun.core.portal.AuditContentPort;
import com.moyun.system.service.AuditBizHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 面试经验审核业务处理器
 *
 * <p><b>防腐层</b>：本类不再直接依赖门户实体 / Mapper（原先 {@code system -> portal}
 * 共 24 条边，其中 8 个 AuditBizHandler 占 18 条）。详情读取与审核落地一律经
 * {@link AuditContentPort}（依赖倒置；实现见 {@code com.moyun.portal.audit.AuditContentAdapter}）。
 * 各业务的状态取值（如 published/rejected/active/resolved）与字段口径均由适配器负责，
 * 本类只负责"任务类型 → 端口调用"的映射。</p>
 *
 * @author moyun
 */
@Component
public class InterviewExpAuditBizHandler implements AuditBizHandler {

    /** 审核任务类型（与 ai/审核中心的任务类型一致） */
    private static final String TASK_TYPE = "interview_exp";

    @Autowired
    private AuditContentPort auditContentPort;

    @Override
    public String supportedTaskType() {
        return TASK_TYPE;
    }

    @Override
    public void approve(Long bizId, Long auditorId, String auditorName, String opinion) {
        auditContentPort.applyAudit(TASK_TYPE, bizId, true, auditorId, auditorName, opinion);
    }

    @Override
    public void reject(Long bizId, Long auditorId, String auditorName, String opinion) {
        auditContentPort.applyAudit(TASK_TYPE, bizId, false, auditorId, auditorName, opinion);
    }

    @Override
    public Map<String, Object> getBizDetail(Long bizId) {
        return auditContentPort.loadDetail(TASK_TYPE, bizId);
    }
}