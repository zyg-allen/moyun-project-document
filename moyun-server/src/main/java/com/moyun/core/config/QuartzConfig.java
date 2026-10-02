package com.moyun.core.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.LocalDataSourceJobStore;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.scheduling.quartz.SpringBeanJobFactory;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.Properties;

/**
 * Quartz 调度器配置：**JDBCJobStore 集群模式**
 *
 * <h3>为什么必须自定义 {@code SchedulerFactoryBean}（而不是只改 yaml）</h3>
 * <p>只需把 {@code spring.quartz.job-store-type} 设为 {@code jdbc} 会**启动失败**（上一批实测）：</p>
 * <pre>
 * SchedulerConfigException: No local DataSource found for configuration
 *   - 'dataSource' property must be set on SchedulerFactoryBean
 * </pre>
 * <p>根因：{@link LocalDataSourceJobStore}（{@code JobStoreCMT} 的 Spring 子类）要求 Spring 把
 * **真实 DataSource 显式注入** {@code SchedulerFactoryBean}；而 Spring Boot 的
 * {@code QuartzAutoConfiguration} 本身不设置 DataSource。本项目 {@code DruidConfig} 定义了
 * {@code masterDataSource} / {@code slaveDataSource} 两个 DataSource，且以
 * {@code dynamicDataSource}（{@code AbstractRoutingDataSource} 路由数据源）为 {@code @Primary}
 * ——路由数据源随线程上下文切换，**不能**给 Quartz 做事务感知的 JobStore。
 * 故这里显式注入**物理主库** {@code masterDataSource}（定时任务日志/状态只写主库）。</p>
 *
 * <h3>集群语义（多实例部署时不再重复触发）</h3>
 * <ul>
 *   <li>{@code jobStore.isClustered=true} + {@code instanceId=AUTO}：每个节点用
 *       {@code qrtz_locks} 行锁竞争触发权，**同一时刻同一触发器只在一个节点上执行**；</li>
 *   <li>{@code instanceName} 集群内**必须一致**（同一个逻辑调度器），{@code instanceId=AUTO}
 *       保证各节点唯一；</li>
 *   <li>{@code acquireTriggersWithinLock=true} + {@code batchTriggerAcquisitionMaxCount=1}：
 *       官方推荐的多节点死锁规避组合（避免节点间"先取触发器再抢锁"的循环等待）；</li>
 *   <li>{@code clusterCheckinInterval=10s}：故障节点被判定失效的时间窗。</li>
 * </ul>
 *
 * <p>注意：{@code @Configuration} 类上标 {@code @Order} 对本类的 bean 无效的坑，
 * 已在安全链上踩过一次；此处只有一个 {@code SchedulerFactoryBean}，不涉及顺序。</p>
 *
 * <p>配套改动：{@code SysJobServiceImpl#init()} 不再 {@code scheduler.clear()}
 * （共享 JobStore 下清库会波及其他实例），改为**幂等同步**；见该类注释。</p>
 *
 * @author moyun
 * @see com.moyun.ext.job.service.impl.SysJobServiceImpl
 */
@Slf4j
@Configuration
public class QuartzConfig {

    /** 逻辑调度器名：集群内所有节点必须一致 */
    public static final String SCHEDULER_INSTANCE_NAME = "moyunScheduler";

    @Bean
    public SchedulerFactoryBean schedulerFactoryBean(
            @Qualifier("masterDataSource") DataSource masterDataSource,
            PlatformTransactionManager transactionManager,
            ApplicationContext applicationContext) {

        SchedulerFactoryBean factory = new SchedulerFactoryBean();

        // ① 根因修复：注入物理主库（不能用 @Primary 的路由数据源）
        factory.setDataSource(masterDataSource);
        factory.setTransactionManager(transactionManager);

        // ② 任务实例交给 Spring 创建（与 Spring Boot 默认一致：支持属性注入）
        SpringBeanJobFactory jobFactory = new SpringBeanJobFactory();
        jobFactory.setApplicationContext(applicationContext);
        factory.setJobFactory(jobFactory);

        // ③ 集群属性
        Properties props = new Properties();
        props.setProperty("org.quartz.scheduler.instanceName", SCHEDULER_INSTANCE_NAME);
        props.setProperty("org.quartz.scheduler.instanceId", "AUTO");
        props.setProperty("org.quartz.jobStore.class", LocalDataSourceJobStore.class.getName());
        props.setProperty("org.quartz.jobStore.driverDelegateClass",
                "org.quartz.impl.jdbcjobstore.StdJDBCDelegate");
        props.setProperty("org.quartz.jobStore.tablePrefix", "QRTZ_");
        props.setProperty("org.quartz.jobStore.isClustered", "true");
        props.setProperty("org.quartz.jobStore.clusterCheckinInterval", "10000");
        props.setProperty("org.quartz.jobStore.acquireTriggersWithinLock", "true");
        // 注意：batchTriggerAcquisitionMaxCount 是 **scheduler** 级属性（不是 jobStore 级，
        // 写成 org.quartz.jobStore.* 会启动失败：No setter for property 'batchTriggerAcquisitionMaxCount'）
        props.setProperty("org.quartz.scheduler.batchTriggerAcquisitionMaxCount", "1");
        props.setProperty("org.quartz.scheduler.skipUpdateCheck", "true");
        factory.setQuartzProperties(props);

        // ④ 生命周期：随容器启动、关闭时等待在跑任务结束
        factory.setAutoStartup(true);
        factory.setWaitForJobsToCompleteOnShutdown(true);
        factory.setOverwriteExistingJobs(true);

        log.info("✅ Quartz 已切换为 JDBC 集群 JobStore: instanceName={}, instanceId=AUTO, tablePrefix=QRTZ_, clustered=true",
                SCHEDULER_INSTANCE_NAME);
        return factory;
    }
}
