package com.moyun.ext.flowable.listener;

import org.flowable.engine.delegate.ExecutionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 流程执行监听器
 *
 * @author Tony
 * @date 2021-04-03
 */
@Component
public class FlowExecutionListener implements ExecutionListener {

    private static final Logger log = LoggerFactory.getLogger(FlowExecutionListener.class);

    @Override
    public void notify(org.flowable.engine.delegate.DelegateExecution execution) {
        String eventName = execution.getEventName();
        String processInstanceId = execution.getProcessInstanceId();

        log.info("执行监听器触发 - 事件: {}, 流程实例ID: {}", eventName, processInstanceId);

        switch (eventName) {
            case EVENTNAME_START:
                log.info("流程开始");
                break;
            case EVENTNAME_END:
                log.info("流程结束");
                break;
            case EVENTNAME_TAKE:
                log.info("连线执行");
                break;
            default:
                log.info("其他事件: {}", eventName);
        }
    }
}