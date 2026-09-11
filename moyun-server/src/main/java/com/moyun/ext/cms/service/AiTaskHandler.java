package com.moyun.ext.cms.service;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * 通用 AI 异步任务处理器接口（v10.23）
 *
 * <p>每个实现类声明一种任务类型（{@link #taskType()}），由 Spring 自动收集注册到
 * {@link AiTaskService}/{@link AiTaskAsyncExecutor} 的 handlerMap。
 * 新增任务类型只需新增实现类，无需改动任务基础设施。</p>
 *
 * <p>实现类应为无状态 {@code @Service} Bean；{@link #execute} 在
 * {@code aiTaskExecutor} 线程池中执行，无 SecurityContext，用户身份通过
 * {@code userId} 参数显式传入。</p>
 *
 * @author moyun
 */
public interface AiTaskHandler {

    /** 任务类型标识，如 resume_parse */
    String taskType();

    /**
     * 执行任务，返回结果对象（将序列化为 JSON 存入 task.result）
     *
     * @param userId 任务所属用户ID（异步线程无登录上下文，显式传入）
     * @param bizRef 业务参数 JSON（提交时由调用方传入并持久化）
     * @return 任务结果对象（null 时 result 不填充）
     * @throws Exception 执行失败（由异步执行器统一捕获并回写任务失败状态）
     */
    Object execute(Long userId, JsonNode bizRef) throws Exception;
}
