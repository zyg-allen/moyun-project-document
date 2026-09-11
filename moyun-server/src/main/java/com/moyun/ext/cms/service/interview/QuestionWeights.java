package com.moyun.ext.cms.service.interview;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 出题权重（v11.x 智能出题）
 *
 * <p>四路题源：job 岗位核心 / resume 简历深挖 / weak 薄弱点 / random 随机兜底。
 * 默认 40/30/20/10；quota(count) 用最大余数法分配题额，保证合计恒等于 count。</p>
 *
 * <p>解析链（QuestionPickerImpl 内落定）：前端传参 > 面试配置 > 岗位模板 > 默认值。</p>
 *
 * @author moyun
 */
public class QuestionWeights {

    private static final Logger log = LoggerFactory.getLogger(QuestionWeights.class);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** 岗位核心题权重（默认 40） */
    private int job = 40;

    /** 简历深挖题权重（默认 30） */
    private int resume = 30;

    /** 薄弱点题权重（默认 20） */
    private int weak = 20;

    /** 随机兜底题权重（默认 10） */
    private int random = 10;

    public static QuestionWeights defaults() {
        return new QuestionWeights();
    }

    /**
     * 宽容解析 JSON（如 {"job":40,"resume":30,"weak":20,"random":10}）；
     * 解析失败或字段缺失时使用默认值
     */
    public static QuestionWeights fromJson(String json) {
        QuestionWeights weights = new QuestionWeights();
        if (json == null || json.isBlank()) {
            return weights;
        }
        try {
            JsonNode node = MAPPER.readTree(json);
            weights.job = clamp(node.path("job").asInt(weights.job));
            weights.resume = clamp(node.path("resume").asInt(weights.resume));
            weights.weak = clamp(node.path("weak").asInt(weights.weak));
            weights.random = clamp(node.path("random").asInt(weights.random));
        } catch (Exception e) {
            log.warn("[QuestionWeights] 权重 JSON 解析失败，使用默认值：{} -> {}", json, e.getMessage());
        }
        return weights;
    }

    /**
     * 从前端传参 Map 构建（VoiceStartConfig.questionWeights）；
     * 空值/缺失字段沿用默认值
     */
    public static QuestionWeights fromMap(java.util.Map<String, Integer> map) {
        QuestionWeights weights = new QuestionWeights();
        if (map == null || map.isEmpty()) {
            return weights;
        }
        Integer job = map.get("job");
        Integer resume = map.get("resume");
        Integer weak = map.get("weak");
        Integer random = map.get("random");
        if (job != null) weights.job = clamp(job);
        if (resume != null) weights.resume = clamp(resume);
        if (weak != null) weights.weak = clamp(weak);
        if (random != null) weights.random = clamp(random);
        return weights;
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(100, v));
    }

    /**
     * 按总题数分配各题源题额（最大余数法）：
     * 先按比例取整，余数按小数部分从大到小逐一分给（同小数时按 job > resume > weak > random）
     *
     * @param count 主问题目总数
     * @return source → 题额（四键齐全，合计 = count）
     */
    public Map<String, Integer> quota(int count) {
        Map<String, Integer> quota = new LinkedHashMap<>();
        if (count <= 0) {
            quota.put("job", 0);
            quota.put("resume", 0);
            quota.put("weak", 0);
            quota.put("random", 0);
            return quota;
        }
        int total = job + resume + weak + random;
        if (total <= 0) {
            // 全 0 权重：全部走随机（等价旧行为）
            quota.put("job", 0);
            quota.put("resume", 0);
            quota.put("weak", 0);
            quota.put("random", count);
            return quota;
        }
        // 比例下限（保底至少 1 题的题源直接给 1，避免小权重被完全挤掉）
        String[] keys = {"job", "resume", "weak", "random"};
        int[] values = {job, resume, weak, random};
        double[] raw = new double[4];
        int[] floor = new int[4];
        int allocated = 0;
        for (int i = 0; i < 4; i++) {
            raw[i] = (double) values[i] / total * count;
            floor[i] = (int) Math.floor(raw[i]);
            if (values[i] > 0 && floor[i] == 0 && count > 0) {
                floor[i] = 1; // 有权重的题源保底 1 题
            }
            allocated += floor[i];
        }
        // 保底可能超出总数：从题额最大的题源回扣
        while (allocated > count) {
            int maxIdx = 0;
            for (int i = 1; i < 4; i++) {
                if (floor[i] > floor[maxIdx]) {
                    maxIdx = i;
                }
            }
            floor[maxIdx]--;
            allocated--;
        }
        // 最大余数法分配剩余额度
        int remain = count - allocated;
        while (remain > 0) {
            int bestIdx = -1;
            double bestFrac = -1;
            for (int i = 0; i < 4; i++) {
                double frac = raw[i] - floor[i];
                if (frac > bestFrac || (frac == bestFrac && bestIdx >= 0 && values[i] > values[bestIdx])) {
                    bestFrac = frac;
                    bestIdx = i;
                }
            }
            if (bestIdx < 0) {
                bestIdx = 3; // 兜底给 random
            }
            floor[bestIdx]++;
            remain--;
        }
        for (int i = 0; i < 4; i++) {
            quota.put(keys[i], floor[i]);
        }
        return quota;
    }

    public int getJob() {
        return job;
    }

    public int getResume() {
        return resume;
    }

    public int getWeak() {
        return weak;
    }

    public int getRandom() {
        return random;
    }
}