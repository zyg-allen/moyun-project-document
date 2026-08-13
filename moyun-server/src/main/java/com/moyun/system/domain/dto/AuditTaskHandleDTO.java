package com.moyun.system.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 审核任务处理参数（v8.1）
 * <p>
 * 处理人通过审核中心提交「同意/驳回」操作。
 * 驳回时 {@link #auditOpinion} 必填。
 *
 * @author moyun
 */
@Data
public class AuditTaskHandleDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 审核任务ID */
    @NotNull(message = "审核任务ID不能为空")
    private Long taskId;

    /** 审核操作：approve=同意 / reject=驳回 */
    @NotBlank(message = "审核操作类型不能为空")
    private String action;

    /** 审核意见（驳回时必填，同意时可选） */
    private String auditOpinion;

    /** 是否通知提交人（默认 true） */
    private Boolean notifyUser = Boolean.TRUE;
}
