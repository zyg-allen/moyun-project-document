package com.moyun;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.Scheduler;
import org.quartz.TriggerBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.quartz.LocalDataSourceJobStore;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 真库 · 多实例验证：**同一触发器在集群中只执行一次**（v13.7 的核心证据）
 *
 * <h3>为什么这样测</h3>
 * <p>报告原文把 Quartz 集群列为"需真实多实例环境验证"，上一批因此回退。本测试用
 * <b>两个独立的 Quartz 调度器实例</b>（各自 {@code SchedulerFactoryBean}，同一物理库、
 * 同一 {@code instanceName}、{@code instanceId=AUTO}）组成**真集群**，共享 {@code qrtz_*}
 * 表，然后放一个 1 秒一次的 cron 触发器：</p>
 * <ul>
 *   <li>集群语义正确 → 6~8 秒内总执行次数 ≈ 触发次数（**1 次/秒**）；</li>
 *   <li>集群未生效（各自注册） → 次数翻倍（**2 次/秒**）——正是 P0-12 描述的多实例重复执行。</li>
 * </ul>
 * <p>为与业务调度器隔离，本测试使用**独立的 {@code instanceName}（{@code moyunClusterProbe}）**：
 * Quartz 的 {@code qrtz_*} 行以 {@code SCHED_NAME} 列区分逻辑调度器，因此探针数据与生产
 * {@code moyunScheduler} 互不影响；测试结束按 {@code SCHED_NAME} 精确清理并断言零残留。</p>
 *
 * <p>⚠️ 探针任务组固定为 {@code CLUSTER_PROBE}，且业务侧启动同步只清理
 * "sys_job 中出现过的组"，故两者不会互相误删。</p>
 *
 * @author moyun
 */
@SpringBootTest
class QuartzClusterSingleExecutionDbTest {

    private static final String PROBE_SCHED_NAME = "moyunClusterProbe";
    private static final String PROBE_GROUP = "CLUSTER_PROBE";
    private static final String JOB_NAME = "clusterProbeJob";
    private static final String TRIGGER_NAME = "clusterProbeTrigger";

    /** 观察窗口（秒）：cron 每秒一次 → 期望 ~1 次/秒 */
    private static final int OBSERVE_SECONDS = 7;

    /**
     * 负向对照开关：{@code -Dprobe.ramstore=true} 让两个探针节点各用**独立 RAMJobStore**
     * （即 v13.7 之前的历史形态）。
     *
     * <p>用途：**证明本测试装置真的能识别多实例重复执行**——历史形态下每个实例各自注册、
     * 各自触发，总次数翻倍、断言应失败。这是"验证装置必须先证有效"（附录 K 教训）的落地。</p>
     *
     * <p>⚠️ 首版负向对照用的是"共享 DB 但 {@code isClustered=false}"，实测**不会**翻倍
     * （{@code qrtz_triggers.TRIGGER_STATE} 被原子置为 ACQUIRED，另一节点抢不到）——
     * 也就是说 P0-12 的重复执行根因**恰是"每实例独立内存 JobStore"**，不是"没开 isClustered"。
     * 故此处按真实历史形态构造对照。</p>
     */
    private static final boolean PROBE_RAM_STORE = Boolean.getBoolean("probe.ramstore");

    private static final AtomicInteger EXECUTIONS = new AtomicInteger();

    /**
     * 探针涉及的全部 qrtz 表，**按删除顺序排列**（子表在前）。
     *
     * <p>⚠️ 实测：本库 {@code qrtz_*} 表带**物理外键**：
     * {@code qrtz_triggers → qrtz_job_details}、{@code qrtz_{cron,simple,simprop,blob}_triggers
     * → qrtz_triggers}。所以清理必须"先触发器子表、再触发器、最后任务明细"，
     * 否则报 {@code Cannot delete or update a parent row: a foreign key constraint fails}
     * 并留下残留数据（首版就是按字母序删的，踩到了）。</p>
     */
    private static final List<String> PROBE_TABLES = List.of(
            "qrtz_blob_triggers", "qrtz_cron_triggers", "qrtz_simple_triggers", "qrtz_simprop_triggers",
            "qrtz_fired_triggers", "qrtz_triggers", "qrtz_paused_trigger_grps",
            "qrtz_job_details", "qrtz_scheduler_state", "qrtz_calendars", "qrtz_locks");

    @Autowired
    @Qualifier("masterDataSource")
    private DataSource masterDataSource;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private final List<Scheduler> startedSchedulers = new ArrayList<>();

    /** 探针任务：只做计数 */
    public static class ProbeJob implements Job {
        @Override
        public void execute(JobExecutionContext context) {
            EXECUTIONS.incrementAndGet();
        }
    }

    @Test
    @DisplayName("两个集群节点共享 JDBC JobStore：1 秒触发器总执行次数 ≈ 1 次/秒（不是 2 次/秒）")
    void triggerFiresExactlyOnceAcrossTwoClusterNodes() throws Exception {
        Scheduler nodeA = startProbeNode("probe-node-A");
        Scheduler nodeB = startProbeNode("probe-node-B");

        for (Scheduler node : List.of(nodeA, nodeB)) {
            assertTrue(node.isStarted(), "探针节点未启动: " + node.getSchedulerName());
            if (!PROBE_RAM_STORE) {
                assertTrue(node.getMetaData().isJobStoreClustered(),
                        "节点未处于集群模式（元数据 isJobStoreClustered=false）");
            }
            assertEquals(PROBE_SCHED_NAME, node.getMetaData().getSchedulerName());
        }
        assertEquals(nodeA.getMetaData().getSchedulerName(), nodeB.getMetaData().getSchedulerName(),
                "集群内两个节点的 instanceName 必须相同");

        JobDetail job = JobBuilder.newJob(ProbeJob.class)
                .withIdentity(JOB_NAME, PROBE_GROUP)
                .storeDurably()
                .build();
        CronTrigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(TRIGGER_NAME, PROBE_GROUP)
                .withSchedule(CronScheduleBuilder.cronSchedule("0/1 * * * * ?")
                        // 错过就跳过，避免"补偿执行"把计数抬高造成误判
                        .withMisfireHandlingInstructionDoNothing())
                .build();
        nodeA.scheduleJob(job, trigger);
        if (PROBE_RAM_STORE) {
            // 历史形态：每个实例各自注册同一任务（内存 JobStore，互不可见）
            nodeB.scheduleJob(job, trigger);
        }

        // 留出集群 check-in 时间（clusterCheckinInterval=1s）后观察
        Thread.sleep(OBSERVE_SECONDS * 1000L);

        int total = EXECUTIONS.get();
        int fires = OBSERVE_SECONDS - 1; // 7 秒窗口内 cron 触发 6~7 次
        System.out.println("[QuartzCluster] jobStore=" + (PROBE_RAM_STORE ? "RAM(历史形态)" : "JDBC-clustered")
                + " 观察 " + OBSERVE_SECONDS + "s，总执行次数=" + total
                + "（集群正确应为≈" + fires + "，两实例各自触发则为≈" + (fires * 2) + "）");
        // 判别标准：共享集群 JobStore ≈ fires（实测 7~8）；历史 RAMJobStore 形态 ≈ 2×fires（≥12）。
        // 上界取 fires+3（=10）留抖动余量，同时与"翻倍"（≥12）明确分离。
        assertTrue(total >= fires - 2 && total <= fires + 3,
                "集群下同一触发器总执行次数应≈每秒 1 次（期望 " + fires + " 左右，上界 " + (fires + 3) + "），实际=" + total
                        + "（若为每秒约 2 次即 " + (fires * 2) + " 左右，说明两个实例各自注册/各自触发，重复执行未消除）");

        shutdownAndCleanup();
        assertEquals(0, probeRowCount(),
                "探针数据未清理干净（SCHED_NAME=" + PROBE_SCHED_NAME + "）");
    }

    @AfterEach
    void afterEach() throws Exception {
        shutdownAndCleanup();
    }

    /** 上一轮异常退出可能留下探针行，先按外键顺序清干净，保证测试可重复执行 */
    @BeforeEach
    void beforeEach() throws Exception {
        deleteProbeRows();
        EXECUTIONS.set(0);
    }

    // ==================== 内部 ====================

    /**
     * 以"物理主库 DataSource + 集群 JDBC JobStore"启动一个探针调度器节点。
     * 与生产 {@code QuartzConfig} 使用同一套 JobStore 配置（只换 instanceName 做隔离）。
     */
    private Scheduler startProbeNode(String instanceId) throws Exception {
        SchedulerFactoryBean factory = new SchedulerFactoryBean();
        if (!PROBE_RAM_STORE) {
            factory.setDataSource(masterDataSource);
            factory.setTransactionManager(transactionManager);
        }
        factory.setAutoStartup(true);
        factory.setWaitForJobsToCompleteOnShutdown(true);

        Properties props = new Properties();
        props.setProperty("org.quartz.scheduler.instanceName", PROBE_SCHED_NAME);
        props.setProperty("org.quartz.scheduler.instanceId", instanceId);
        if (PROBE_RAM_STORE) {
            props.setProperty("org.quartz.jobStore.class", "org.quartz.simpl.RAMJobStore");
        } else {
            props.setProperty("org.quartz.jobStore.class", LocalDataSourceJobStore.class.getName());
            props.setProperty("org.quartz.jobStore.driverDelegateClass",
                    "org.quartz.impl.jdbcjobstore.StdJDBCDelegate");
            props.setProperty("org.quartz.jobStore.tablePrefix", "QRTZ_");
            props.setProperty("org.quartz.jobStore.isClustered", "true");
            props.setProperty("org.quartz.jobStore.clusterCheckinInterval", "1000");
            props.setProperty("org.quartz.jobStore.acquireTriggersWithinLock", "true");
            // scheduler 级（不是 jobStore 级，见 QuartzConfig 同名注释）
            props.setProperty("org.quartz.scheduler.batchTriggerAcquisitionMaxCount", "1");
        }
        factory.setQuartzProperties(props);

        factory.afterPropertiesSet();
        // 手工创建的 SchedulerFactoryBean 不在 Spring 容器里：afterPropertiesSet() 只做初始化，
        // 启动发生在 SmartLifecycle#start()（容器 refresh 时才调）——必须显式 start()，
        // 否则调度器根本不跑（首版实测执行次数=0 就是这个原因）
        factory.start();
        Scheduler scheduler = factory.getScheduler();
        startedSchedulers.add(scheduler);
        return scheduler;
    }

    /** 关闭探针调度器并删除其全部 qrtz 行（幂等，可重复调用） */
    private void shutdownAndCleanup() throws Exception {
        for (Scheduler scheduler : startedSchedulers) {
            try {
                scheduler.shutdown(true);
            } catch (Exception ignored) {
                // 已关闭/未启动，忽略
            }
        }
        startedSchedulers.clear();
        EXECUTIONS.set(0);
        deleteProbeRows();
    }

    /** 按外键安全顺序删除探针行 */
    private void deleteProbeRows() throws Exception {
        try (Connection connection = masterDataSource.getConnection();
             Statement statement = connection.createStatement()) {
            for (String table : PROBE_TABLES) {
                statement.executeUpdate("DELETE FROM " + table + " WHERE SCHED_NAME = '" + PROBE_SCHED_NAME + "'");
            }
        }
    }

    /** 统计探针 SCHED_NAME 在全部 qrtz 表中的残留行数 */
    private int probeRowCount() throws Exception {
        int rows = 0;
        try (Connection connection = masterDataSource.getConnection();
             Statement statement = connection.createStatement()) {
            for (String table : PROBE_TABLES) {
                try (ResultSet rs = statement.executeQuery(
                        "SELECT COUNT(*) FROM " + table + " WHERE SCHED_NAME = '" + PROBE_SCHED_NAME + "'")) {
                    if (rs.next()) {
                        rows += rs.getInt(1);
                    }
                }
            }
        }
        return rows;
    }
}
