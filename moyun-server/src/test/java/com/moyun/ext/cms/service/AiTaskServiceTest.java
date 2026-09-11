package com.moyun.ext.cms.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moyun.common.exception.system.ServiceException;
import com.moyun.portal.domain.entity.PortalAiTask;
import com.moyun.portal.mapper.PortalAiTaskMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 表驱动 AI 异步任务服务单测（v11.67 P1-6）
 *
 * <p>覆盖：提交校验与入库触发 / 触发失败回写 / 轮询查询权限 / result 解析容错 /
 * 启动孤儿任务恢复（pending/running 置 failed）。</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AiTaskServiceTest {

    @Mock
    private PortalAiTaskMapper aiTaskMapper;

    @Mock
    private AiTaskAsyncExecutor asyncExecutor;

    @Mock
    private AiTaskHandler handler;

    private AiTaskService service;

    @BeforeEach
    void setUp() {
        when(handler.taskType()).thenReturn("test_type");
        service = new AiTaskService(List.of(handler));
        ReflectionTestUtils.setField(service, "aiTaskMapper", aiTaskMapper);
        ReflectionTestUtils.setField(service, "asyncExecutor", asyncExecutor);
        ReflectionTestUtils.setField(service, "objectMapper", new ObjectMapper());
    }

    // ---------- submitTask ----------

    @Test
    void submitTaskRejectsUnknownType() {
        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.submitTask(1L, "no_such_type", null));
        assertTrue(ex.getMessage().contains("未知的AI任务类型"));
        verify(aiTaskMapper, never()).insert(any(PortalAiTask.class));
    }

    @Test
    void submitTaskInsertsPendingAndTriggersAsync() {
        doAnswer(inv -> {
            inv.getArgument(0, PortalAiTask.class).setId(100L);
            return 1;
        }).when(aiTaskMapper).insert(any(PortalAiTask.class));

        Long taskId = service.submitTask(7L, "test_type", Map.of("resumeId", 5));

        assertEquals(100L, taskId);
        ArgumentCaptor<PortalAiTask> captor = ArgumentCaptor.forClass(PortalAiTask.class);
        verify(aiTaskMapper).insert(captor.capture());
        PortalAiTask inserted = captor.getValue();
        assertEquals("pending", inserted.getStatus());
        assertEquals(7L, inserted.getUserId());
        assertEquals("test_type", inserted.getTaskType());
        assertNotNull(inserted.getBizRef());
        assertTrue(inserted.getBizRef().contains("\"resumeId\":5"));

        // 异步触发使用回填后的任务 ID 与业务参数
        ArgumentCaptor<JsonNode> bizRefCaptor = ArgumentCaptor.forClass(JsonNode.class);
        verify(asyncExecutor).execute(eq(100L), eq(7L), eq("test_type"), bizRefCaptor.capture());
        assertEquals(5, bizRefCaptor.getValue().get("resumeId").asInt());
    }

    @Test
    void submitTaskWritesFailedWhenAsyncTriggerRejected() {
        doAnswer(inv -> {
            inv.getArgument(0, PortalAiTask.class).setId(200L);
            return 1;
        }).when(aiTaskMapper).insert(any(PortalAiTask.class));
        doThrow(new RuntimeException("线程池已满")).when(asyncExecutor)
                .execute(any(), any(), any(), any());

        Long taskId = service.submitTask(7L, "test_type", null);

        assertEquals(200L, taskId);
        ArgumentCaptor<PortalAiTask> captor = ArgumentCaptor.forClass(PortalAiTask.class);
        verify(aiTaskMapper).updateById(captor.capture());
        assertEquals("failed", captor.getValue().getStatus());
        assertTrue(captor.getValue().getError().contains("任务触发失败"));
    }

    // ---------- getTask ----------

    @Test
    void getTaskRejectsMissingTask() {
        when(aiTaskMapper.selectById(999L)).thenReturn(null);
        assertThrows(ServiceException.class, () -> service.getTask(999L, 7L));
    }

    @Test
    void getTaskRejectsOthersTask() {
        PortalAiTask task = new PortalAiTask();
        task.setId(1L);
        task.setUserId(7L);
        when(aiTaskMapper.selectById(1L)).thenReturn(task);
        // 用户 8 查询用户 7 的任务 → 越权拒绝
        assertThrows(ServiceException.class, () -> service.getTask(1L, 8L));
    }

    @Test
    void getTaskParsesResultJson() {
        PortalAiTask task = new PortalAiTask();
        task.setId(1L);
        task.setUserId(7L);
        task.setTaskType("test_type");
        task.setStatus("success");
        task.setResult("{\"summary\":\"优化完成\"}");
        when(aiTaskMapper.selectById(1L)).thenReturn(task);

        var vo = service.getTask(1L, 7L);

        assertEquals("success", vo.getStatus());
        assertNotNull(vo.getResult());
        assertEquals("优化完成", vo.getResult().get("summary").asText());
    }

    @Test
    void getTaskToleratesIllegalResultJson() {
        PortalAiTask task = new PortalAiTask();
        task.setId(1L);
        task.setUserId(7L);
        task.setStatus("success");
        task.setResult("{not-json");
        when(aiTaskMapper.selectById(1L)).thenReturn(task);

        var vo = service.getTask(1L, 7L);

        // 解析失败不阻断轮询，result 保持 null
        assertEquals("success", vo.getStatus());
        assertTrue(vo.getResult() == null || vo.getResult().isNull());
    }

    // ---------- recoverOrphanTasks（v11.67 孤儿恢复） ----------

    @Test
    @SuppressWarnings("unchecked")
    void recoverOrphanTasksMarksPendingRunningAsFailed() {
        when(aiTaskMapper.update(isNull(), any(Wrapper.class))).thenReturn(3);

        service.recoverOrphanTasks();

        ArgumentCaptor<Wrapper<PortalAiTask>> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(aiTaskMapper).update(isNull(), captor.capture());
        UpdateWrapper<PortalAiTask> uw = (UpdateWrapper<PortalAiTask>) captor.getValue();
        // 先触发 SQL 段求值（MP 的 IN 表达式惰性注册参数），再从参数表断言
        String whereSegment = uw.getSqlSegment();
        assertTrue(whereSegment.contains("status"));
        assertTrue(whereSegment.contains("IN"));
        Map<String, Object> params = uw.getParamNameValuePairs();
        assertTrue(params.containsValue("pending"));
        assertTrue(params.containsValue("running"));
        assertTrue(params.containsValue("failed"));
        assertTrue(params.containsValue("服务重启，任务中断，请重新提交"));
    }

    @Test
    void recoverOrphanTasksNoopWhenNoOrphan() {
        when(aiTaskMapper.update(isNull(), any(Wrapper.class))).thenReturn(0);

        service.recoverOrphanTasks();

        verify(aiTaskMapper).update(isNull(), any(Wrapper.class));
    }
}
