package com.moyun.ext.ai.enums;

/**
 * 工作流执行状态枚举
 *
 * <p>对应数据库表 workflow_execution.status 字段，标识单次工作流执行的状态：
 * <ul>
 *   <li>{@link #RUNNING}    - 执行中</li>
 *   <li>{@link #COMPLETED}  - 已完成（成功结束）</li>
 *   <li>{@link #FAILED}     - 失败（执行过程中抛出异常）</li>
 *   <li>{@link #CANCELLED}  - 已取消（用户主动中断）</li>
 * </ul>
 *
 * @author moyun
 */
public enum WorkflowExecutionStatus {

    RUNNING("running", "执行中"),
    COMPLETED("completed", "已完成"),
    FAILED("failed", "失败"),
    CANCELLED("cancelled", "已取消");

    private final String code;
    private final String desc;

    WorkflowExecutionStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static WorkflowExecutionStatus fromCode(String code) {
        if (code == null) {
            return RUNNING;
        }
        for (WorkflowExecutionStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return RUNNING;
    }
}
