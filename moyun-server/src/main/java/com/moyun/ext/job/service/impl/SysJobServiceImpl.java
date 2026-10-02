package com.moyun.ext.job.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.moyun.common.constant.ScheduleConstants;
import com.moyun.common.exception.system.job.TaskException;
import com.moyun.core.redis.DistributedLockUtil;
import com.moyun.ext.job.domain.entity.SysJob;
import com.moyun.ext.job.mapper.SysJobMapper;
import com.moyun.ext.job.service.ISysJobService;
import com.moyun.ext.job.util.CronUtils;
import com.moyun.ext.job.util.ScheduleUtils;
import com.moyun.util.string.StringUtils;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.ObjectAlreadyExistsException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 定时任务调度服务实现
 *
 * @author allen-zyg
 */
@Slf4j
@Service
public class SysJobServiceImpl implements ISysJobService {

    /** 启动同步的互斥键（多实例同时启动时串行化"收敛到 DB 状态"的过程） */
    private static final String STARTUP_SYNC_LOCK_KEY = "job:startup-sync";

    @Autowired
    private SysJobMapper jobMapper;

    @Autowired
    private Scheduler scheduler;

    @Autowired
    private DistributedLockUtil distributedLockUtil;

    /**
     * 项目启动时把 {@code sys_job} 的配置同步到 Quartz JobStore（**集群安全的幂等同步**）。
     *
     * <p><b>不调用 {@code scheduler.clear()}</b>。Quartz 使用
     * <b>JDBC 集群 JobStore</b>，JobStore 是**多实例共享的持久状态**，启动时清库会：</p>
     * <ol>
     *   <li>删掉其他实例正在使用的任务（对方不会感知，任务静默消失）；</li>
     *   <li>两个实例同时启动时，互相清掉对方刚注册的任务，产生竞态窗口。</li>
     * </ol>
     * <p>改为**幂等同步**（语义等价于"JobStore 与 sys_job 对齐"，但集群安全）：</p>
     * <ul>
     *   <li>JobStore 无该任务 → 新建（并发下若已被别的实例建好，捕获
     *       {@link ObjectAlreadyExistsException} 视为已存在）；</li>
     *   <li>已存在但 cron / misfire / concurrent / invokeTarget 有变化 → 重建；</li>
     *   <li>已存在且完全一致 → <b>不动</b>（不做无谓 delete+create，避免影响正在跑的触发）；</li>
     *   <li>按 {@code sys_job.status} 暂停/恢复（幂等）；</li>
     *   <li>孤儿清理：只清理 {@code sys_job} 中**出现过的组**里的多余任务
     *       （本应用管理的组），不触碰其他组——测试/其他组件注册的任务不受影响。</li>
     * </ul>
     *
     * <p>并发保护：优先用 Redis 锁串行化；<b>拿不到锁也继续执行</b>——同步本身幂等，
     * 且 Quartz 自己的 {@code qrtz_locks} 保证单条 CRUD 原子性；锁只减少抖动，
     * 不应成为"Redis 抖一下 → 任务全不注册"的启动依赖。</p>
     */
    @PostConstruct
    public void init() throws SchedulerException, TaskException {
        DistributedLockUtil.Lock lock = null;
        try {
            lock = distributedLockUtil.tryLock(STARTUP_SYNC_LOCK_KEY, Duration.ofSeconds(60));
            if (lock == null) {
                log.warn("[SysJob] 启动同步未取得 Redis 锁（可能有其他实例正在同步），仍继续执行幂等同步");
            }
            syncJobsFromDatabase();
        } finally {
            if (lock != null) {
                lock.close();
            }
        }
    }

    /**
     * 幂等同步 {@code sys_job} → Quartz JobStore：可重复执行、可多实例并发执行。
     *
     * @return {@code [新建数, 重建数, 孤儿删除数, 未变数]}
     */
    public int[] syncJobsFromDatabase() throws SchedulerException, TaskException {
        int created = 0;
        int rebuilt = 0;
        int removed = 0;
        int unchanged = 0;

        List<SysJob> jobList = jobMapper.selectJobList(new SysJob());
        Set<JobKey> expected = new HashSet<>();
        Set<String> managedGroups = new HashSet<>();

        for (SysJob job : jobList) {
            JobKey jobKey = ScheduleUtils.getJobKey(job.getJobId(), job.getJobGroup());
            expected.add(jobKey);
            managedGroups.add(job.getJobGroup());

            if (!scheduler.checkExists(jobKey)) {
                if (createQuietly(job)) {
                    created++;
                } else {
                    unchanged++;
                }
                continue;
            }

            if (isDefinitionChanged(jobKey, job)) {
                scheduler.deleteJob(jobKey);
                if (createQuietly(job)) {
                    rebuilt++;
                }
            } else {
                unchanged++;
                applyStatus(job, jobKey);
            }
        }

        // 孤儿清理：仅在 sys_job 出现过的组内清理（不碰其他组）
        for (String group : managedGroups) {
            for (JobKey jobKey : scheduler.getJobKeys(GroupMatcher.jobGroupEquals(group))) {
                if (!expected.contains(jobKey)) {
                    scheduler.deleteJob(jobKey);
                    removed++;
                    log.info("[SysJob] 清理孤儿任务（sys_job 已无此任务）: {}", jobKey);
                }
            }
        }

        log.info("[SysJob] 启动同步完成: 新建={}, 重建={}, 孤儿删除={}, 未变={}, 库中任务={}",
                created, rebuilt, removed, unchanged, jobList.size());
        return new int[]{created, rebuilt, removed, unchanged};
    }

    /** 仅当 JobStore 中不存在该 key 时新建；并发下已被别的实例建好则视为成功（返回 false） */
    private boolean createQuietly(SysJob job) throws SchedulerException, TaskException {
        try {
            ScheduleUtils.createScheduleJob(scheduler, job);
            return true;
        } catch (ObjectAlreadyExistsException e) {
            // 另一个实例刚建好同一任务：这是集群下的正常竞态，不是错误
            log.debug("[SysJob] 任务已由其他实例注册，跳过: jobId={}", job.getJobId());
            return false;
        }
    }

    /** 定义是否变化（触发器 cron / 调用目标 / 计划策略 / 并发标记 / 任务类） */
    private boolean isDefinitionChanged(JobKey jobKey, SysJob job) throws SchedulerException {
        JobDetail detail = scheduler.getJobDetail(jobKey);
        if (detail == null) {
            return true;
        }
        Trigger trigger = scheduler.getTrigger(ScheduleUtils.getTriggerKey(job.getJobId(), job.getJobGroup()));
        String storedCron = (trigger instanceof CronTrigger cron) ? cron.getCronExpression() : null;
        if (!Objects.equals(storedCron, job.getCronExpression())) {
            return true;
        }
        if (!Objects.equals(detail.getJobClass(), ScheduleUtils.getQuartzJobClass(job))) {
            return true;
        }
        Object stored = detail.getJobDataMap().get(ScheduleConstants.TASK_PROPERTIES);
        if (!(stored instanceof SysJob storedJob)) {
            return true;
        }
        return !Objects.equals(storedJob.getInvokeTarget(), job.getInvokeTarget())
                || !Objects.equals(storedJob.getMisfirePolicy(), job.getMisfirePolicy())
                || !Objects.equals(storedJob.getConcurrent(), job.getConcurrent());
    }

    /** 按 sys_job.status 暂停/恢复（幂等） */
    private void applyStatus(SysJob job, JobKey jobKey) throws SchedulerException {
        if (ScheduleConstants.STATUS_PAUSE.equals(job.getStatus())) {
            scheduler.pauseJob(jobKey);
        } else {
            scheduler.resumeJob(jobKey);
        }
    }

    @Override
    public List<SysJob> selectJobList(SysJob job) {
        return jobMapper.selectJobList(job);
    }

    /**
     * 分页查询定时任务调度（MyBatis-Plus 标准分页）
     */
    @Override
    public IPage<SysJob> selectJobPage(IPage<SysJob> page, SysJob job) {
        return jobMapper.selectJobPage(page, job);
    }

    @Override
    public SysJob selectJobById(Long jobId) {
        return jobMapper.selectJobById(jobId);
    }

    @Override
    public int pauseJob(SysJob job) throws SchedulerException {
        Long jobId = job.getJobId();
        String jobGroup = job.getJobGroup();
        job.setStatus(ScheduleConstants.STATUS_PAUSE);
        int rows = jobMapper.updateJob(job);
        if (rows > 0) {
            scheduler.pauseJob(ScheduleUtils.getJobKey(jobId, jobGroup));
        }
        return rows;
    }

    @Override
    public int resumeJob(SysJob job) throws SchedulerException {
        Long jobId = job.getJobId();
        String jobGroup = job.getJobGroup();
        job.setStatus(ScheduleConstants.STATUS_NORMAL);
        int rows = jobMapper.updateJob(job);
        if (rows > 0) {
            scheduler.resumeJob(ScheduleUtils.getJobKey(jobId, jobGroup));
        }
        return rows;
    }

    @Override
    public int deleteJob(SysJob job) throws SchedulerException {
        Long jobId = job.getJobId();
        String jobGroup = job.getJobGroup();
        int rows = jobMapper.deleteJobById(job.getJobId());
        if (rows > 0) {
            scheduler.deleteJob(ScheduleUtils.getJobKey(jobId, jobGroup));
        }
        return rows;
    }

    @Override
    public void deleteJobByIds(Long[] jobIds) throws SchedulerException {
        for (Long jobId : jobIds) {
            SysJob job = jobMapper.selectJobById(jobId);
            if (job != null) {
                deleteJob(job);
            }
        }
    }

    @Override
    public int changeStatus(SysJob job) throws SchedulerException {
        int rows = 0;
        String status = job.getStatus();
        if (ScheduleConstants.STATUS_NORMAL.equals(status)) {
            rows = resumeJob(job);
        } else if (ScheduleConstants.STATUS_PAUSE.equals(status)) {
            rows = pauseJob(job);
        }
        return rows;
    }

    @Override
    public boolean run(SysJob job) throws SchedulerException {
        Long jobId = job.getJobId();
        String jobGroup = job.getJobGroup();
        SysJob properties = selectJobById(job.getJobId());
        if (StringUtils.isNull(properties)) {
            return false;
        }
        scheduler.triggerJob(ScheduleUtils.getJobKey(jobId, jobGroup));
        return true;
    }

    @Override
    public int insertJob(SysJob job) throws SchedulerException, TaskException {
        int rows = jobMapper.insertJob(job);
        if (rows > 0) {
            ScheduleUtils.createScheduleJob(scheduler, job);
        }
        return rows;
    }

    @Override
    public int updateJob(SysJob job) throws SchedulerException, TaskException {
        int rows = jobMapper.updateJob(job);
        if (rows > 0) {
            updateSchedulerJob(job);
        }
        return rows;
    }

    @Override
    public boolean checkCronExpressionIsValid(String cronExpression) {
        return CronUtils.isValid(cronExpression);
    }

    private void updateSchedulerJob(SysJob job) throws SchedulerException, TaskException {
        Long jobId = job.getJobId();
        String jobGroup = job.getJobGroup();
        scheduler.deleteJob(ScheduleUtils.getJobKey(jobId, jobGroup));
        ScheduleUtils.createScheduleJob(scheduler, job);
    }
}
