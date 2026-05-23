package com.moyun.system.flowable.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.common.core.domain.AjaxResult;
import com.moyun.system.domain.FlowProcDefDto;

import java.io.InputStream;
import java.util.Map;

/**
 * 流程定义Service接口
 *
 * @author Tony
 * @date 2021-04-03
 */
public interface IFlowDefinitionService {

    /**
     * 检查流程定义是否存在
     *
     * @param processDefinitionKey 流程定义KEY
     * @return 是否存在
     */
    boolean exist(String processDefinitionKey);

    /**
     * 流程定义列表
     *
     * @param name 流程名称
     * @param pageNum 当前页码
     * @param pageSize 每页条数
     * @return 流程定义分页列表数据
     */
    Page<FlowProcDefDto> list(String name, Integer pageNum, Integer pageSize);

    /**
     * 导入流程文件
     * 当每个key的流程第一次部署时，指定版本为1。对其后所有使用相同key的流程定义，
     * 部署时版本会在该key当前已部署的最高版本号基础上加1。
     *
     * @param name 流程名称
     * @param category 流程分类
     * @param in 输入流
     */
    void importFile(String name, String category, InputStream in);

    /**
     * 读取xml
     *
     * @param deployId 部署ID
     * @return XML内容
     */
    AjaxResult readXml(String deployId);

    /**
     * 根据流程定义ID启动流程实例
     *
     * @param procDefId 流程定义ID
     * @param variables 流程变量
     * @return 结果
     */
    AjaxResult startProcessInstanceById(String procDefId, Map<String, Object> variables);

    /**
     * 激活或挂起流程定义
     *
     * @param state 状态（1:激活,2:挂起）
     * @param deployId 流程部署ID
     */
    void updateState(Integer state, String deployId);

    /**
     * 删除流程定义
     *
     * @param deployId 流程部署ID act_ge_bytearray 表中 deployment_id值
     */
    void delete(String deployId);

    /**
     * 读取图片文件
     *
     * @param deployId 部署ID
     * @return 图片输入流
     */
    InputStream readImage(String deployId);
}
