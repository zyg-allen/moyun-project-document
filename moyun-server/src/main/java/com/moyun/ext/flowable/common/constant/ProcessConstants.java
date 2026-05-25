
package com.moyun.ext.flowable.common.constant;

/**
 * 流程常量信息
 *
 * @author Tony
 * @date 2021-04-03
 */
public class ProcessConstants {

    /**
     * 动态数据
     */
    public static final String DYNAMIC = "dynamic";

    /**
     * 固定任务接收
     */
    public static final String FIXED = "fixed";

    /**
     * 单个审批人
     */
    public static final String ASSIGNEE = "assignee";

    /**
     * 候选人
     */
    public static final String CANDIDATE_USERS = "candidateUsers";

    /**
     * 审批组
     */
    public static final String CANDIDATE_GROUPS = "candidateGroups";

    /**
     * 单个审批人
     */
    public static final String PROCESS_APPROVAL = "approval";

    /**
     * 会签人员
     */
    public static final String PROCESS_MULTI_INSTANCE_USER = "userList";

    /**
     * nameapace
     */
    public static final String NAMASPASE = "http://flowable.org/bpmn";

    /**
     * 会签节点
     */
    public static final String PROCESS_MULTI_INSTANCE = "multiInstance";

    /**
     * 自定义属性 dataType
     */
    public static final String PROCESS_CUSTOM_DATA_TYPE = "dataType";

    /**
     * 自定义属性 userType
     */
    public static final String PROCESS_CUSTOM_USER_TYPE = "userType";

    /**
     * 初始化人员
     */
    public static final String PROCESS_INITIATOR = "INITIATOR";

    /**
     * 单实例
     */
    public static final String SINGLE = "single";

    /**
     * 会签
     */
    public static final String MULTI_INSTANCE = "multiInstance";

    /**
     * 用户任务监听器
     */
    public static final String TASK_LISTENER = "taskListener";

    /**
     * 执行监听器
     */
    public static final String EXECUTION_LISTENER = "executionListener";

    /**
     * 自定义属性
     */
    public static final String PROPERTIES = "properties";

    /**
     * 流程状态
     */
    public static final String PROCESS_STATUS = "processStatus";

    /**
     * 流程状态-进行中
     */
    public static final Integer PROCESS_RUNNING = 1;

    /**
     * 流程状态-已结束
     */
    public static final Integer PROCESS_FINISHED = 2;
}
