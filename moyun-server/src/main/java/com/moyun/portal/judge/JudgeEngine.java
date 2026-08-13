package com.moyun.portal.judge;

import java.util.List;

import com.moyun.portal.domain.entity.PortalInterviewQuestionTestCase;

/**
 * OJ 判题引擎（v6.3 OJ 判题系统）
 * <p>
 * 抽象判题执行细节，便于在不同环境替换实现：
 * <ul>
 *   <li>开发环境：{@link ProcessJudgeEngine} 基于 ProcessBuilder 直接运行，超时控制；</li>
 *   <li>生产环境：替换为基于 Docker / Firecracker MicroVM 的隔离沙箱实现（限制 CPU/内存/网络/文件系统）。</li>
 * </ul>
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
