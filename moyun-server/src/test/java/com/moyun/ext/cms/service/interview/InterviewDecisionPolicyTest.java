package com.moyun.ext.cms.service.interview;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 出题决策策略单测：预算 × 链深 × 轮数矩阵
 */
class InterviewDecisionPolicyTest {

    @Test
    void deepenWithinAllBudgets() {
        InterviewDecisionPolicy.Decision d = InterviewDecisionPolicy.resolve(
                "deepen", 0, 0, 1, 5);
        assertEquals("deepen", d.getAction());
    }

    @Test
    void deepenRejectedWhenDepthExceeded() {
        InterviewDecisionPolicy.Decision d = InterviewDecisionPolicy.resolve(
                "deepen", 2, 0, 1, 5);
        assertEquals("change_topic", d.getAction());
        assertNotNull(d.getOverrideReason());
    }

    @Test
    void deepenRejectedWhenBudgetExhausted() {
        InterviewDecisionPolicy.Decision d = InterviewDecisionPolicy.resolve(
                "deepen", 0, 4, 1, 5);
        assertEquals("change_topic", d.getAction());
    }

    @Test
    void deepenRejectedWhenAllRoundsDone() {
        InterviewDecisionPolicy.Decision d = InterviewDecisionPolicy.resolve(
                "deepen", 0, 0, 5, 5);
        assertEquals("wrap_up", d.getAction());
    }

    @Test
    void changeTopicRejectedWhenDone() {
        InterviewDecisionPolicy.Decision d = InterviewDecisionPolicy.resolve(
                "change_topic", 0, 0, 5, 5);
        assertEquals("wrap_up", d.getAction());
    }

    @Test
    void wrapUpAllowedAtFullProgress() {
        InterviewDecisionPolicy.Decision d = InterviewDecisionPolicy.resolve(
                "wrap_up", 0, 0, 5, 5);
        assertEquals("wrap_up", d.getAction());
    }

    @Test
    void wrapUpAllowedAt80Percent() {
        // 4/5 = 80%，允许提前收尾
        InterviewDecisionPolicy.Decision d = InterviewDecisionPolicy.resolve(
                "wrap_up", 0, 0, 4, 5);
        assertEquals("wrap_up", d.getAction());
    }

    @Test
    void earlyWrapUpRejected() {
        // 2/5 = 40%，拒绝提前收尾 → 降级 change_topic
        InterviewDecisionPolicy.Decision d = InterviewDecisionPolicy.resolve(
                "wrap_up", 0, 0, 2, 5);
        assertEquals("change_topic", d.getAction());
        assertNotNull(d.getOverrideReason());
    }

    @Test
    void nullActionDefaultsToChangeTopic() {
        InterviewDecisionPolicy.Decision d = InterviewDecisionPolicy.resolve(
                null, 0, 0, 1, 5);
        assertEquals("change_topic", d.getAction());
    }

    @Test
    void nullActionDefaultsToWrapUpWhenDone() {
        InterviewDecisionPolicy.Decision d = InterviewDecisionPolicy.resolve(
                null, 0, 0, 5, 5);
        assertEquals("wrap_up", d.getAction());
    }

    @Test
    void unknownActionFallsBack() {
        InterviewDecisionPolicy.Decision d = InterviewDecisionPolicy.resolve(
                "dance", 0, 0, 1, 5);
        assertEquals("change_topic", d.getAction());
        assertTrue(d.getOverrideReason().contains("dance"));
    }

    @Test
    void zeroTotalPlannedAlwaysWrapsUp() {
        InterviewDecisionPolicy.Decision d = InterviewDecisionPolicy.resolve(
                "change_topic", 0, 0, 0, 0);
        assertEquals("wrap_up", d.getAction());
    }

    @Test
    void deepenAtDepthOneStillValid() {
        InterviewDecisionPolicy.Decision d = InterviewDecisionPolicy.resolve(
                "deepen", 1, 3, 2, 5);
        assertEquals("deepen", d.getAction());
    }
}
