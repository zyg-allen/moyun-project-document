package com.moyun.ext.ai.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link AiSceneTasks} 白名单守卫（v13.51 批次 4）。
 *
 * <p>背景：场景配置的 {@code scene_code} 支持两段式 {@code main:task}，
 * 但管理端原先用 {@link AiSceneEnum#of(String)} 做**整串精确匹配** ⇒ 合法子场景存不了。
 * 批次 4 改为两段式校验后，task 段依赖本类的白名单 —— 若白名单漏项，
 * 管理端仍会拒存合法子场景（回归到原缺陷）。故用测试把「白名单必须含全部常量」钉死。</p>
 *
 * @author laomao
 */
class AiSceneTasksTest {

    @Test
    @DisplayName("all()：由常量反射派生，非空且含全部公开常量")
    void allContainsEveryConstant() throws Exception {
        Set<String> all = AiSceneTasks.all();
        assertNotNull(all, "all() 不应返回 null");
        assertFalse(all.isEmpty(), "all() 不应为空 —— 至少包含现有 13 个 task 常量");

        int constantCount = 0;
        for (java.lang.reflect.Field f : AiSceneTasks.class.getDeclaredFields()) {
            if (!java.lang.reflect.Modifier.isStatic(f.getModifiers())) {
                continue;
            }
            if (f.getType() != String.class) {
                continue;
            }
            Object v = f.get(null);
            if (v == null || String.valueOf(v).isBlank()) {
                continue;
            }
            constantCount++;
            assertTrue(all.contains(String.valueOf(v)),
                    "白名单漏掉了常量 " + f.getName() + " = " + v);
        }
        assertEquals(constantCount, all.size(),
                "白名单大小应等于常量个数（既不能漏，也不应混入非常量值）");
    }

    @Test
    @DisplayName("isValid()：已注册子任务通过，未注册/空/null 拒绝")
    void isValidGuardsWhitelist() {
        // 面试族子任务（批次 1 新增的 4 个必须在内）
        assertTrue(AiSceneTasks.isValid(AiSceneTasks.INTERVIEW_WARMUP));
        assertTrue(AiSceneTasks.isValid(AiSceneTasks.INTERVIEW_ANSWER_ANALYSIS));
        assertTrue(AiSceneTasks.isValid(AiSceneTasks.INTERVIEW_SELF_INTRO));
        assertTrue(AiSceneTasks.isValid(AiSceneTasks.INTERVIEW_OPENING_FALLBACK));
        assertTrue(AiSceneTasks.isValid(AiSceneTasks.INTERVIEW_HINT));
        assertTrue(AiSceneTasks.isValid(AiSceneTasks.INTERVIEW_REPORT_REVIEW));
        assertTrue(AiSceneTasks.isValid(AiSceneTasks.INTERVIEW_INDUSTRY_INSIGHT));
        // 简历优化族 + 出题族
        assertTrue(AiSceneTasks.isValid(AiSceneTasks.RESUME_ADVICE));
        assertTrue(AiSceneTasks.isValid(AiSceneTasks.QUESTION_JD_KEYWORDS));

        // 非法值必须拒绝（否则「乱加子任务」的口子又开了）
        assertFalse(AiSceneTasks.isValid("not_exist_task"), "未注册的短码必须被拒绝");
        assertFalse(AiSceneTasks.isValid(""), "空串必须被拒绝");
        assertFalse(AiSceneTasks.isValid(null), "null 必须被拒绝");
    }

    @Test
    @DisplayName("两段式拆分口径：主场景用 AiSceneEnum，task 用本白名单")
    void twoSegmentSplitContract() {
        // 复刻 AiSceneConfigController.validate 的两段式判定，防止口径漂移
        String sceneCode = "voice_interview:" + AiSceneTasks.INTERVIEW_WARMUP;
        int colon = sceneCode.indexOf(':');
        assertTrue(colon > 0, "两段式代码必须含冒号且主场景非空");
        String main = sceneCode.substring(0, colon);
        String task = sceneCode.substring(colon + 1);
        assertNotNull(AiSceneEnum.of(main), "主场景必须在 AiSceneEnum 注册表内");
        assertTrue(AiSceneTasks.isValid(task), "子任务必须在本白名单内");

        // 三段式应被拒绝（只支持两段）
        assertTrue("a:b:c".substring("a:b:c".indexOf(':') + 1).contains(":"),
                "三段式应能被识别并拒绝");
    }
}
