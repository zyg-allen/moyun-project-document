package com.moyun.system.flowable.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.moyun.common.core.domain.AjaxResult;
import com.moyun.common.core.domain.entity.SysUser;
import com.moyun.common.utils.SecurityUtils;
import com.moyun.system.flowable.common.constant.ProcessConstants;
import com.moyun.system.flowable.common.enums.FlowComment;
import com.moyun.system.flowable.factory.FlowServiceFactory;
import com.moyun.system.flowable.service.IFlowDefinitionService;
import com.moyun.system.flowable.service.ISysDeployFormService;
import com.moyun.system.domain.FlowProcDefDto;
import com.moyun.system.domain.SysForm;
import com.moyun.system.mapper.FlowDeployMapper;
import com.moyun.system.service.ISysDeptService;
import com.moyun.system.service.ISysUserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.engine.IdentityService;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.repository.ProcessDefinitionQuery;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.image.impl.DefaultProcessDiagramGenerator;
import org.flowable.task.api.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 流程定义
 *
 * @author Tony
 * @date 2021-04-03
 */
@Service
@Slf4j
public class FlowDefinitionServiceImpl extends FlowServiceFactory implements IFlowDefinitionService {

    @Resource
    private ISysDeployFormService sysDeployFormService;

    @Resource
    private ISysUserService sysUserService;

    @Resource
    private ISysDeptService sysDeptService;

    @Resource
    private FlowDeployMapper flowDeployMapper;

    @Autowired
    private IdentityService identityService;

    private static final String BPMN_FILE_SUFFIX = ".bpmn";

    @Override
    public boolean exist(String processDefinitionKey) {
        ProcessDefinitionQuery processDefinitionQuery
                = getRepositoryService().createProcessDefinitionQuery().processDefinitionKey(processDefinitionKey);
        long count = processDefinitionQuery.count();
        return count > 0;
    }


    /**
     * 流程定义列表
     *
     * @param name    流程名称，用于模糊查询
     * @param pageNum 当前页码
     * @param pageSize 每页条数
     * @return 流程定义分页列表数据
     */
    @Override
    public Page<FlowProcDefDto> list(String name, Integer pageNum, Integer pageSize) {
        // 创建 MyBatis-Plus 分页对象
        Page<FlowProcDefDto> page = new Page<>(pageNum, pageSize);
        
        // 使用 MyBatis-Plus 分页查询（自动添加 LIMIT 和查询总数）
        IPage<FlowProcDefDto> resultPage = flowDeployMapper.selectDeployPage(page, name);
        
        // 加载挂表单（如果需要）
        // for (FlowProcDefDto procDef : resultPage.getRecords()) {
        //     SysFormForm sysForm = sysDeployFormService.selectSysDeployFormByDeployId(procDef.getDeploymentId());
        //     if (Objects.nonNull(sysForm)) {
        //         procDef.setFormName(sysForm.getFormName());
        //         procDef.setFormId(sysForm.getFormId());
        //     }
        // }
        
        // 返回分页结果（MyBatis-Plus 已自动设置 total、records 等）
        return (Page<FlowProcDefDto>) resultPage;
    }


    /**
     * 导入流程文件
     *
     * 当每个key的流程第一次部署时，指定版本为1。对其后所有使用相同key的流程定义，
     * 部署时版本会在该key当前已部署的最高版本号基础上加1。key参数用于区分流程定义
     * @param name     流程定义名称
     * @param category 流程分类
     * @param in       流程文件输入流
     */
    @Override
    public void importFile(String name, String category, InputStream in) {
        Deployment deploy = getRepositoryService().createDeployment().addInputStream(name + BPMN_FILE_SUFFIX, in).name(name).category(category).deploy();
        ProcessDefinition definition = getRepositoryService().createProcessDefinitionQuery().deploymentId(deploy.getId()).singleResult();
        getRepositoryService().setProcessDefinitionCategory(definition.getId(), category);

    }

    /**
     * 读取xml
     *
     * @param deployId 部署ID
     * @return 包含XML内容的AjaxResult
     */
    @Override
    public AjaxResult readXml(String deployId) {
        try {
            ProcessDefinition definition = getRepositoryService().createProcessDefinitionQuery().deploymentId(deployId).singleResult();
            InputStream inputStream = getRepositoryService().getResourceAsStream(definition.getDeploymentId(), definition.getResourceName());
            String result = IOUtils.toString(inputStream, StandardCharsets.UTF_8.name());
            return AjaxResult.success("", result);
        } catch (IOException e) {
            return AjaxResult.error("读取XML失败: " + e.getMessage());
        }
    }

    /**
     * 读取图片文件
     * @param deployId 部署ID
     * @return 图片文件输入流
     */
    @Override
    public InputStream readImage(String deployId) {
        ProcessDefinition processDefinition = getRepositoryService().createProcessDefinitionQuery().deploymentId(deployId).singleResult();
        //获得图片流
        DefaultProcessDiagramGenerator diagramGenerator = new DefaultProcessDiagramGenerator();
        BpmnModel bpmnModel = getRepositoryService().getBpmnModel(processDefinition.getId());
        
        // Flowable 7.x API - 使用新的 generateDiagram 方法
        return diagramGenerator.generateDiagram(
                bpmnModel,
                "png",
                Collections.emptyList(),  // 高亮节点
                Collections.emptyList(),  // 高亮连线
                "宋体",                    // 活动字体
                "宋体",                    // 标签字体
                "宋体",                    // 注释字体
                null,                      // 自定义类加载器
                1.0,                       // 缩放比例
                false);                    // 是否绘制注释
    }

    /**
     * 根据流程定义ID启动流程实例
     *
     * @param procDefId 流程模板ID
     * @param variables 流程变量
     * @return 操作结果
     */
    @Override
    public AjaxResult startProcessInstanceById(String procDefId, Map<String, Object> variables) {
        try {
            ProcessDefinition processDefinition = getRepositoryService().createProcessDefinitionQuery().processDefinitionId(procDefId)
                    .latestVersion().singleResult();
            if (Objects.nonNull(processDefinition) && processDefinition.isSuspended()) {
                return AjaxResult.error("流程已被挂起,请先激活流程");
            }
            // 设置流程发起人Id到流程中
            SysUser sysUser = SecurityUtils.getLoginUser().getUser();
            identityService.setAuthenticatedUserId(sysUser.getUserId().toString());
            variables.put(ProcessConstants.PROCESS_INITIATOR, sysUser.getUserId());

            // 流程发起时 跳过发起人节点
            ProcessInstance processInstance = getRuntimeService().startProcessInstanceById(procDefId, variables);
            // 给第一步申请人节点设置任务执行人和意见
            Task task = getTaskService().createTaskQuery().processInstanceId(processInstance.getProcessInstanceId()).singleResult();
            if (Objects.nonNull(task)) {
                getTaskService().addComment(task.getId(), processInstance.getProcessInstanceId(), FlowComment.NORMAL.getType(), sysUser.getNickName() + "发起流程申请");
                getTaskService().complete(task.getId(), variables);
            }
            return AjaxResult.success("流程启动成功");
        } catch (Exception e) {
            log.error("流程启动错误", e);
            return AjaxResult.error("流程启动错误");
        }
    }


    /**
     * 激活或挂起流程定义
     *
     * @param state    状态（1:激活, 2:挂起）
     * @param deployId 流程部署ID
     */
    @Override
    public void updateState(Integer state, String deployId) {
        ProcessDefinition procDef = getRepositoryService().createProcessDefinitionQuery().deploymentId(deployId).singleResult();
        // 激活
        if (state == 1) {
            getRepositoryService().activateProcessDefinitionById(procDef.getId(), true, null);
        }
        // 挂起
        if (state == 2) {
            getRepositoryService().suspendProcessDefinitionById(procDef.getId(), true, null);
        }
    }


    /**
     * 删除流程定义
     *
     * @param deployId 流程部署ID act_ge_bytearray 表中 deployment_id值
     */
    @Override
    public void delete(String deployId) {
        // true 允许级联删除 ,不设置会导致数据库外键关联异常
        getRepositoryService().deleteDeployment(deployId, true);
    }


}