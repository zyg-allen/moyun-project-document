-- =============================================================================
-- V12.0 题库成长闭环（增量）
-- 背景：题库三种练习模式（reading/choice/coding）统一接入成长体系：
--   1. read_question 成长规则缺失 → 详情页阅读行为记不进成长时间线
--      （recordEvent 找不到规则时直接 return 0，growth_log 不落库）
--   2. practice_mode 历史数据塌缩：早期按 question_type 录入的题目未回填，
--      选择题/编程题练习列表按 practice_mode 筛选会漏题
-- 位置：portal_growth_rule / portal_interview_question / portal_achievement
-- 幂等：规则先 DELETE 再 INSERT；practice_mode 用条件 UPDATE，可重复执行。
-- =============================================================================

-- 1. read_question 成长规则（每日同题幂等由 recordQuestionRead 保证，daily_limit 控每日积分总量）
DELETE FROM `moyun-db`.portal_growth_rule WHERE module = 'interview' AND action = 'read_question';
INSERT INTO `moyun-db`.portal_growth_rule (module, action, growth_delta, daily_limit, description, status, sort, create_by, create_time, update_by, update_time, remark) VALUES ('interview', 'read_question', 1, 20, '阅读题目', '0', 19, 'admin', NOW(), '', null, '题库阅读学习行为，同题每日仅记一次');

-- 2. practice_mode 回填（幂等：仅修正空值或按题目特征可推断的历史数据）
-- 2.1 有选项+正确答案的题目 → choice
UPDATE `moyun-db`.portal_interview_question
SET practice_mode = 'choice'
WHERE del_flag = '0'
  AND (practice_mode IS NULL OR practice_mode = '' OR practice_mode = 'reading')
  AND options IS NOT NULL AND options != ''
  AND correct_answer IS NOT NULL AND correct_answer != '';

-- 2.2 算法类且有参考代码的题目 → coding
UPDATE `moyun-db`.portal_interview_question
SET practice_mode = 'coding'
WHERE del_flag = '0'
  AND (practice_mode IS NULL OR practice_mode = '' OR practice_mode = 'reading')
  AND (question_type = 'algorithm' OR (solution IS NOT NULL AND solution != ''))
  AND (options IS NULL OR options = '');

-- 2.3 其余无判分材料的题目 → reading（展示阅读）
UPDATE `moyun-db`.portal_interview_question
SET practice_mode = 'reading'
WHERE del_flag = '0'
  AND (practice_mode IS NULL OR practice_mode = '');

-- 2.4 choice 模式必备字段补全校验标记（options/correct_answer 任一为空的 choice 视为脏数据，回退 reading）
UPDATE `moyun-db`.portal_interview_question
SET practice_mode = 'reading'
WHERE del_flag = '0'
  AND practice_mode = 'choice'
  AND (options IS NULL OR options = '' OR correct_answer IS NULL OR correct_answer = '');

-- 3. 编程题测试用例校验：coding 模式无任何用例的题目回退 reading（避免做题页报"未配置测试用例"）
--    注意：不物理删除，仅调整模式，录题补全用例后可再改回 coding
UPDATE `moyun-db`.portal_interview_question q
SET q.practice_mode = 'reading'
WHERE q.del_flag = '0'
  AND q.practice_mode = 'coding'
  AND NOT EXISTS (SELECT 1 FROM `moyun-db`.portal_interview_question_test_case tc WHERE tc.question_id = q.id);

-- 3.1 历史状态枚举修正：早期 insertQuestion 默认写入 'active'（已废弃），
--     前台列表默认查 status='published'，导致这部分题目不可见 → 统一回迁 published
UPDATE `moyun-db`.portal_interview_question
SET status = 'published'
WHERE del_flag = '0'
  AND status = 'active';

-- 4. 阅读学习成就（激励闭环：首次阅读 / 坚持30题）
DELETE FROM `moyun-db`.portal_achievement WHERE module = 'interview' AND code IN ('first_read', 'read_50');
INSERT INTO `moyun-db`.portal_achievement (code, name, description, icon, module, condition_json, growth_reward, sort, status, create_by, create_time, update_by, update_time, remark) VALUES ('first_read', '开卷有益', '阅读第一道面试题', null, 'interview', '{"action":"read_question","count":1}', 5, 24, '0', 'admin', NOW(), '', null, null);
INSERT INTO `moyun-db`.portal_achievement (code, name, description, icon, module, condition_json, growth_reward, sort, status, create_by, create_time, update_by, update_time, remark) VALUES ('read_50', '博览群题', '累计阅读50道题目', null, 'interview', '{"action":"read_question","count":50}', 60, 25, '0', 'admin', NOW(), '', null, null);
