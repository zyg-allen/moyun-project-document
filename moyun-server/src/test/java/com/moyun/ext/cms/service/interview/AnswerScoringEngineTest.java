package com.moyun.ext.cms.service.interview;

import com.moyun.portal.domain.entity.PortalInterviewQuestion;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 答题评分引擎单测（v11.60 P0-4：v11.47 拆出后零测试补齐）
 *
 * <p>纯规则无 LLM/无 Spring 依赖，同步离线可测——验证：
 * 评分公式（覆盖率×80 + 长度加成≤20）、6 维连续映射与雷达图维度键对齐、
 * 关键词提取护栏（停用词/纯数字/长度/上限）、LLM 动态题的题干回退提取。</p>
 *
 * @author laomao
 * @since 2026-09-11
 */
class AnswerScoringEngineTest {

    private final AnswerScoringEngine engine = new AnswerScoringEngine();

    private static final String[] DIMENSION_KEYS = {
            "relevance", "professionalism", "fluency", "interactivity", "confidence", "logic"};

    // ==================== 评分主流程 ====================

    @Test
    void scoreAnswer_fullCoverageLongAnswer_highScore() {
        PortalInterviewQuestion q = new PortalInterviewQuestion();
        q.setTags("Java,JVM,MySQL");
        String answer = "首先Java内存模型分堆和栈，其次JVM垃圾回收分代收集，"
                + "然后MySQL索引采用B+树，因为这样范围查询高效，所以整体设计权衡了读写性能，"
                + "举个例子比如聚簇索引叶子节点存整行数据，我个人认为理解这些底层原理对排查线上问题很有帮助，"
                + "我曾经在生产环境处理过一次FullGC频发的问题，通过调整新生代比例解决了。";

        AnswerScoringEngine.ScoreResult r = engine.scoreAnswer(q, answer);

        // 3 关键词全命中 + 长答案 → 高分
        assertTrue(r.score >= 80, "全覆盖长答案应得高分，实际=" + r.score);
        assertTrue(r.feedback.startsWith("回答全面"));
        // 6 维键齐全且在 [0,100]
        assertDimensionsWellFormed(r.dimensions);
        // 结构词/互动信号丰富 → logic/interactivity 应显著高于 30 基线
        assertTrue(r.dimensions.get("logic") >= 54, "多结构词 logic 应≥54，实际=" + r.dimensions.get("logic"));
        assertTrue(r.dimensions.get("interactivity") >= 50, "多互动信号 interactivity 应≥50，实际=" + r.dimensions.get("interactivity"));
    }

    @Test
    void scoreAnswer_zeroCoverageShortAnswer_lowScore() {
        PortalInterviewQuestion q = new PortalInterviewQuestion();
        q.setTags("Redis,分布式锁,RedLock");
        AnswerScoringEngine.ScoreResult r = engine.scoreAnswer(q, "不太清楚");

        assertTrue(r.score < 40, "零覆盖短答案应低分，实际=" + r.score);
        assertTrue(r.feedback.startsWith("回答不够充分"));
        assertEquals("关键词覆盖 0/3", extractCoverage(r.feedback));
        assertDimensionsWellFormed(r.dimensions);
        // relevance = 覆盖率 0
        assertEquals(0, r.dimensions.get("relevance"));
    }

    @Test
    void scoreAnswer_partialCoverage_middleScore() {
        PortalInterviewQuestion q = new PortalInterviewQuestion();
        q.setTags("Java,MySQL");
        String answer = "Java 是面向对象语言，封装继承多态，jvm 虚拟机跨平台，"
                + "集合框架有 List 和 Map，stream api 也可以做数据处理，泛型保证类型安全。";

        AnswerScoringEngine.ScoreResult r = engine.scoreAnswer(q, answer);

        // 覆盖 1/2 = 50%，答案 >100 字 → 中间分
        assertTrue(r.score >= 40 && r.score <= 79, "半覆盖应得中间分，实际=" + r.score);
        assertEquals(50, r.dimensions.get("relevance"));
        // professionalism = 30 + 0.5*50 + min(len/6, 20)
        int expectedProf = Math.min(100, (int) Math.round(30 + 0.5 * 50 + Math.min(answer.length() / 6.0, 20)));
        assertEquals(expectedProf, r.dimensions.get("professionalism"));
    }

    // ==================== LLM 动态题（question 无 tags/solution） ====================

    @Test
    void scoreAnswer_dynamicQuestion_keywordsFromTitle() {
        // v11.30.2：追问/系统设计题无 tags，从题干提取关键词保证 coverage 有意义
        PortalInterviewQuestion q = new PortalInterviewQuestion();
        q.setTitle("谈谈 Redis 缓存穿透的解决方案");
        AnswerScoringEngine.ScoreResult r = engine.scoreAnswer(q,
                "缓存穿透可以用布隆过滤器拦截，Redis 也可以缓存空值，谈谈我的理解。");

        assertNotNull(r.dimensions);
        // "redis"（题干）在答案小写contains 命中 → coverage > 0
        assertTrue(r.dimensions.get("relevance") > 0,
                "题干关键词应参与覆盖计算，relevance=" + r.dimensions.get("relevance"));
    }

    @Test
    void scoreAnswer_nullQuestion_lengthBasedCoverage() {
        // 自我介绍等无题目场景：无关键词 → 长度推断（≥50 字 0.6 / 短 0.2）
        AnswerScoringEngine.ScoreResult long_ = engine.scoreAnswer(null,
                "面试官您好，我叫张三，五年Java开发经验，主要负责电商订单系统的设计与优化，"
                        + "熟悉高并发场景下的缓存与消息队列方案，曾主导订单峰值处理能力从一千QPS提升到一万QPS的改造。");
        AnswerScoringEngine.ScoreResult short_ = engine.scoreAnswer(null, "大家好。");

        assertEquals(60, long_.dimensions.get("relevance"), "≥50字无关键词 coverage=0.6");
        assertEquals(20, short_.dimensions.get("relevance"), "<50字无关键词 coverage=0.2");
        assertEquals("关键词覆盖 0/0", extractCoverage(long_.feedback));
    }

    // ==================== 长度 → 维度连续性 ====================

    @Test
    void scoreAnswer_fluency_isContinuousNotBinary() {
        PortalInterviewQuestion q = new PortalInterviewQuestion();
        q.setTags("Java");
        int prev = -1;
        for (int len : new int[]{10, 60, 150, 300, 400, 800}) {
            String answer = "Java 虚拟机 ".repeat(Math.max(1, len / 8));
            int fluency = engine.scoreAnswer(q, answer).dimensions.get("fluency");
            // 不再是二值固定值：不同长度产生不同（或合理饱和）的流畅分
            assertTrue(fluency >= 30 && fluency <= 100, "fluency 应在[30,100]，len=" + len + " 实际=" + fluency);
            if (prev >= 0 && len <= 300) {
                assertTrue(fluency >= prev, "40-300 区间 fluency 应单调不降");
            }
            prev = fluency;
        }
    }

    @Test
    void scoreAnswer_nullAnswer_noException() {
        PortalInterviewQuestion q = new PortalInterviewQuestion();
        q.setTags("Java");
        // answer=null → lowerAnswer 空串；length 分支依赖 answer.length() —— 验证无 NPE
        AnswerScoringEngine.ScoreResult r = engine.scoreAnswer(q, null);
        assertEquals(0, r.dimensions.get("relevance"));
    }

    // ==================== 关键词提取护栏 ====================

    @Test
    void extractKeywords_filtersStopwordsDigitsAndLength() {
        // 中文逗号分隔 + 停用词（"使用"/"一个"）+ 纯数字 + 单字 + 超长词
        List<String> kws = engine.extractKeywords("Redis,使用，123，A，这是一段超过十个字符的超长关键词内容",
                "MySQL 索引优化 of the");
        assertTrue(kws.contains("Redis"));
        assertTrue(kws.contains("MySQL"));
        assertTrue(kws.contains("索引优化"));
        assertFalse(kws.contains("使用"), "停用词应被过滤");
        assertFalse(kws.contains("123"), "纯数字应被过滤");
        assertFalse(kws.contains("A"), "单字符应被过滤");
        assertFalse(kws.stream().anyMatch(k -> k.length() > 10), "超长词应被过滤");
    }

    @Test
    void extractKeywords_maxTwelveCap() {
        StringBuilder tags = new StringBuilder();
        for (int i = 1; i <= 20; i++) {
            tags.append("关键词").append(i).append(i < 20 ? "," : "");
        }
        List<String> kws = engine.extractKeywords(tags.toString(), null);
        assertEquals(12, kws.size(), "关键词提取应封顶 12 个");
    }

    @Test
    void extractKeywords_dedup() {
        List<String> kws = engine.extractKeywords("Redis,Redis,MySQL", "Redis 缓存");
        assertEquals(1, kws.stream().filter("Redis"::equals).count(), "重复关键词应去重");
    }

    @Test
    void extractKeywords_nullSafety() {
        assertTrue(engine.extractKeywords(null, null).isEmpty());
        assertTrue(engine.extractKeywords("", "").isEmpty());
    }

    // ==================== clamp ====================

    @Test
    void clamp_bounds() {
        assertEquals(0, engine.clamp(-5, 0, 100));
        assertEquals(100, engine.clamp(105, 0, 100));
        assertEquals(42, engine.clamp(42, 0, 100));
    }

    // ==================== 断言辅助 ====================

    private void assertDimensionsWellFormed(Map<String, Integer> dimensions) {
        assertNotNull(dimensions);
        assertEquals(6, dimensions.size(), "雷达图 6 维键应齐全");
        for (String key : DIMENSION_KEYS) {
            Integer v = dimensions.get(key);
            assertNotNull(v, "缺少维度: " + key);
            assertTrue(v >= 0 && v <= 100, key + " 越界: " + v);
        }
    }

    /** 从 feedback 提取 "关键词覆盖 x/y" 段 */
    private String extractCoverage(String feedback) {
        int i = feedback.indexOf("关键词覆盖");
        int j = feedback.indexOf("，答案长度");
        return feedback.substring(i, j);
    }
}
