package com.moyun.ext.flowable.listener;

import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.TaskListener;
import org.flowable.task.service.delegate.DelegateTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 流程任务监听器
 *
 * @author Tony
 * @date 2021-04-03
 */
@Slf4j
@Component
public class FlowTaskListener implements TaskListener {

    @Override
    public void notify(DelegateTask delegateTask) {
        String eventName = delegateTask.getEventName();
        String taskId = delegateTask.getId();
        String taskName = delegateTask.getName();
        String assignee = delegateTask.getAssignee();

        log.info("任务监听器触发 - 事件: {}, 任务ID: {}, 任务名称: {}, 办理人: {}", 
                eventName, taskId, taskName, assignee);

        switch (eventName) {
            case EVENTNAME_CREATE:
                log.info("任务创建: {}", taskName);
                break;
            case EVENTNAME_ASSIGNMENT:
                log.info("任务分配: {} -> {}", taskName, assignee);
                break;
            case EVENTNAME_COMPLETE:
                log.info("任务完成: {}", taskName);
                break;
            case EVENTNAME_DELETE:
                log.info("任务删除: {}", taskName);
                break;
            default:
                log.info("未知事件: {}", eventName);
        }
    }
}