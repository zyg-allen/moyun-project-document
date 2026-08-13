package com.moyun.portal.judge;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * OJ 判题配置（v8.0 沙箱与异步判题）
 * <p>
 * 对应配置前缀 {@code moyun.judge}，覆盖以下能力：
 * <ul>
 *   <li>判题引擎类型切换：process / docker；</li>
 *   <li>同步/异步判题开关与 Redis 队列参数；</li>
 *   <li>Docker 沙箱资源限制：CPU / 内存 / 网络 / 文件系统 / 进程数；</li>
 *   <li>每种语言的运行时镜像与基础参数（按需扩展）。</li>
 * </ul>
 *
 * @author moyun
 */
@Data
@Component
@ConfigurationProperties(prefix = "moyun.judge")
public class JudgeProperties {

    /** 判题引擎类型：process=ProcessBuilder 直接执行（开发/测试环境），docker=Docker 沙箱（生产环境） */
    private String engineType = "process";

    /** 单用例运行超时上限（毫秒），统一同步与 Docker 沙箱执行时的 TLE 阈值 */
    private long timeoutMs = 2000L;

    /** 内存上限（MB），Docker 沙箱生效（--memory） */
    private int memoryLimitMb = 256;

    /** 是否启用异步判题（Redis 队列 + Worker），false=同步判题（开发环境默认） */
    private boolean asyncEnabled = false;

    /** 异步队列配置 */
    private Queue queue = new Queue();

    /** Worker 配置 */
    private Worker worker = new Worker();

    /** Docker 沙箱配置（engine-type=docker 时生效） */
    private Docker docker = new Docker();

    @Data
    public static class Queue {
        /** Redis 队列 key（List 结构，LPUSH 入队 / RPOP 出队） */
        private String name = "moyun:judge:queue";
        /** 结果缓存 key 前缀，存储 PENDING 提交的快速状态摘要，避免每次回查 DB */
        private String resultKeyPrefix = "moyun:judge:result:";
        /** 结果缓存 TTL（秒），用于 PENDING 状态快速过滤，超时由 DB 兜底 */
        private long resultTtlSeconds = 600L;
    }

    @Data
    public static class Worker {
        /** Worker 并发线程数 */
        private int concurrency = 2;
        /** 单次拉取阻塞超时（秒），使用 BLPOP */
        private long popTimeoutSeconds = 5L;
        /** 任务执行失败后的最大重试次数（不含首次执行） */
        private int maxRetry = 1;
        /** Worker 优雅关闭等待（毫秒） */
        private long shutdownAwaitMs = 5000L;
    }

    @Data
    public static class Docker {
        /** Docker CLI 路径，默认使用 PATH 中的 docker */
        private String binary = "docker";
        /** 各语言运行时镜像（key 为 language 小写） */
        private java.util.Map<String, String> images = new java.util.HashMap<>();

        public Docker() {
            // 默认镜像（生产环境可通过 moyun.judge.docker.images.<lang>=xxx 覆盖）
            images.put("javascript", "node:20-alpine");
            images.put("typescript", "node:20-alpine");
            images.put("python", "python:3.11-slim");
            images.put("java", "eclipse-temurin:21-jre-alpine");
            images.put("go", "golang:1.22-alpine");
            images.put("cpp", "gcc:13-bookworm");
            images.put("rust", "rust:1.78-slim");
        }

        /** CPU 配额（--cpus），1.0 = 单核 */
        private double cpus = 1.0;
        /** CPU 帐目周期（微秒，--cpu-period） */
        private long cpuPeriod = 100_000L;
        /** CPU 配额（微秒，--cpu-quota），与 period 配合实现 CPU 限流；-1 表示不限制 */
        private long cpuQuota = -1L;
        /** PID 上限（--pids-limit），防止 fork 炸弹 */
        private long pidsLimit = 100L;
        /** 是否禁用网络（--network=none） */
        private boolean networkDisabled = true;
        /** 只读根文件系统（--read-only） */
        private boolean readOnlyRoot = true;
        /** 临时文件系统挂载（--tmpfs /tmp:rw,size=64m），多挂载点用英文逗号分隔 */
        private String tmpfs = "/tmp:rw,size=64m";
        /** 用户提交代码在容器内挂载目录（绝对路径，对应宿主临时目录） */
        private String workDir = "/sandbox";
        /** 是否使用 seccomp=unconfined（false 表示应用默认 seccomp profile） */
        private boolean seccompUnconfined = false;
        /** 不新增 capability（默认丢弃所有 cap，仅保留计算所需） */
        private boolean dropAllCaps = true;
        /** 单次容器最大执行时间（秒，docker run --rm 超时由外部 Process.waitFor 兜底） */
        private long containerTimeoutSeconds = 30L;
    }
}
