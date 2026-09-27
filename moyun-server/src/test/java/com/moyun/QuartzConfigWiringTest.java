package com.moyun;

import com.moyun.core.config.QuartzConfig;
import com.moyun.ext.job.domain.entity.SysJob;
import com.moyun.ext.job.mapper.SysJobMapper;
import com.moyun.ext.job.service.impl.SysJobServiceImpl;
import com.moyun.ext.job.util.ScheduleUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.quartz.Scheduler;
import org.quartz.SchedulerMetaData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 配置接线守卫：Quartz 必须是 **JDBC 集群 JobStore**，且启动同步是幂等的
 *
 * <h3>被守的两件事（v13.7）</h3>
 * <ol>
 *   <li><b>JobStore 归属</b>：历史（v13 之前）是默认 RAMJobStore，多实例部署时
 *       每个实例各自注册并执行全部 {@code sys_job} → **重复执行**。上批切 JDBC 集群时
 *       实测启动失败（{@code No local DataSource found}，根因是 {@code @Primary} 的
 *       {@code dynamicDataSource} 路由数据源不能给 Quartz 用），故回退并只记录前置条件；
 *       v13.7 用显式 {@link QuartzConfig} 注入物理主库解决。本测试锁死"确实是集群 JDBC JobStore"。</li>
 *   <li><b>启动同步幂等</b>：{@code SysJobServiceImpl#init()} 原为
 *       {@code scheduler.clear()} + 全量重注册 —— 在共享 JobStore 下会清理掉
 *       **其他实例正在使用的任务**。现改为幂等同步；本测试断言"再同步一次 = 什么都没改"，
 *       同时反证它没有清库（清库会导致重建数 = 任务总数）。</li>
 * </ol>
 *
 * @author moyun
 */
@SpringBootTest
class QuartzConfigWiringTest {

    @Autowired
    private Scheduler scheduler;

    @Autowired
    private SysJobMapper jobMapper;

    @Autowired
    private SysJobServiceImpl sysJobService;

    @Test
    @DisplayName("JobStore 必须是 clustered JDBC（LocalDataSourceJobStore），不得是 RAMJobStore")
    void schedulerUsesClusteredJdbcJobStore() throws Exception {
        SchedulerMetaData meta = scheduler.getMetaData();
        String jobStoreClass = meta.getJobStoreClass().getName();

        assertTrue(meta.isJobStoreClustered(),
                "Quartz 未处于集群模式（多实例会重复触发）：jobStoreClass=" + jobStoreClass);
        assertFalse(jobStoreClass.contains("RAMJobStore"),
                "仍是内存 JobStore —— 多实例各自注册、重复执行：jobStoreClass=" + jobStoreClass);
        assertTrue(jobStoreClass.contains("LocalDataSourceJobStore"),
                "JobStore 不是 LocalDataSourceJobStore（DataSource 未正确注入）：jobStoreClass=" + jobStoreClass);
        assertEquals(QuartzConfig.SCHEDULER_INSTANCE_NAME, meta.getSchedulerName(),
                "逻辑调度器名必须与集群配置一致（集群内所有节点同名）");
        assertNotEquals("NON_CLUSTERED", meta.getSchedulerInstanceId(),
                "instanceId 为 NON_CLUSTERED 说明集群属性没生效");
        assertTrue(scheduler.isStarted(), "调度器未启动（SchedulerFactoryBean 生命周期未接管）");
    }

    @Test
    @DisplayName("sys_job 的每个任务都已进入共享 JobStore（启动同步确实生效）")
    void allSysJobsAreRegisteredInJobStore() throws Exception {
        List<SysJob> jobs = jobMapper.selectJobList(new SysJob());
        assertFalse(jobs.isEmpty(), "sys_job 为空，本测试失去意义");
        for (SysJob job : jobs) {
            assertTrue(scheduler.checkExists(ScheduleUtils.getJobKey(job.getJobId(), job.getJobGroup())),
                    "sys_job 中的任务未同步到 JobStore: jobId=" + job.getJobId() + ", name=" + job.getJobName());
        }
    }

    @Test
    @DisplayName("启动同步幂等：再同步一次不得新建/重建/删除任何任务（反证它没有 clear 清库）")
    void startupSyncIsIdempotentAndNonDestructive() throws Exception {
        int jobCount = jobMapper.selectJobList(new SysJob()).size();

        int[] result = sysJobService.syncJobsFromDatabase();

        assertEquals(0, result[0], "二次同步不该新建任务（created）");
        assertEquals(0, result[1], "二次同步不该重建任务（rebuilt）——重建意味着定义比较有误或清了库");
        assertEquals(0, result[2], "二次同步不该删除任务（removed）");
        assertEquals(jobCount, result[3], "二次同步应全部命中'未变'分支");
    }
}
