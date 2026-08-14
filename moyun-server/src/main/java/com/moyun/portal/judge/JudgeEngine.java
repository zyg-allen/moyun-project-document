package com.moyun.portal.judge;

import java.util.List;

import com.moyun.portal.domain.entity.PortalInterviewQuestionTestCase;

/**
 * OJ 判题引擎（v6.3 OJ 判题系统 / v8.0 沙箱演进）
 * <p>
 * 抽象判题执行细节，便于在不同环境替换实现：
 * <ul>
 *   <li>开发/测试环境：{@link ProcessJudgeEngine} 基于 ProcessBuilder 直接运行，超时控制
 *       （默认，配置 moyun.judge.engine-type=process 或缺省启用）；</li>
 *   <li>生产环境：{@link DockerJudgeEngine} 基于 Docker 沙箱的隔离运行，
 *       限制 CPU/内存/网络/文件系统（配置 moyun.judge.engine-type=docker 启用）。</li>
 * </ul>
 * 引擎切换通过 Spring {@code @ConditionalOnProperty} 自动选择，
 * 业务层 {@code PortalJudgeServiceImpl} 仅依赖本接口，对具体实现透明。
 *
 * @author moyun
 */
public interface JudgeEngine {

    /**
     * 对用户代码运行所有用例并返回判题结果。
     *
     * @param language 编程语言（javascript/typescript/python/java/go/cpp/rust）
     * @param code    用户代码
     * @param cases   测试用例列表（按顺序执行）
     * @param timeoutMs 单用例运行超时上限（毫秒），超时判 TLE
     * @return 判题结果（含逐用例明细）
     */
    JudgeResult judge(String language, String code,
                      List<PortalInterviewQuestionTestCase> cases,
                      long timeoutMs);
}
