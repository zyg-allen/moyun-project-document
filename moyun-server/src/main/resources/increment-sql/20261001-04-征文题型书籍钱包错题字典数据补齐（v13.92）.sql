-- ============================================================
-- 增量脚本：20261001-04-征文/题型/书籍/钱包/错题 字典数据补齐（v13.92）
-- 背景：以下 6 个字典类型在 sys_dict_type **已注册**，但 sys_dict_data **一行数据都没有**
--       （清单 P2 字典化批次）。后果：后台字段管理下拉为空；门户相关页面只能硬编码文案，
--       且已按字典驱动的页面（如题型）实际一直走本地兜底分支。
--
--   portal_question_type          题目类型        0 → 5
--   cms_contest_status            征文活动状态    0 → 4
--   portal_book_type              书籍类型        0 → 3
--   portal_book_serial_status     书籍连载状态    0 → 3
--   portal_wallet_txn_type        钱包交易类型    0 → 3
--   portal_wrong_question_status  错题状态        0 → 2
--
-- 取值口径：**沿用代码中的既有枚举值**（页面硬编码口径 / 后端实际写入值），不新造。
--   ⚠ 钱包交易类型特别说明：页面原先写的是 member（会员），而后端 VIP 记账实际写入的是 **vip**
--      ⇒ 该分支从未命中、流水类型显示为原始英文。本脚本按后端真实值落 **vip**（缺陷同修）。
--   ⚠ 错题状态只有 wrong / mastered 两个：历史实现的 reviewing 状态**全仓无写入点**（v13.84 已移除）。
--
-- 幂等性：DELETE + INSERT 全量覆盖，可重复执行。
-- DDL/DML 初始化脚本已同步补齐（moyun-db-dml-init.sql），本脚本面向存量库。
-- 执行后清理 Redis 字典缓存（否则后端继续命中旧空缓存）：
--   redis-cli DEL "sys_dict:portal_question_type" "sys_dict:cms_contest_status" \
--                 "sys_dict:portal_book_type" "sys_dict:portal_book_serial_status" \
--                 "sys_dict:portal_wallet_txn_type" "sys_dict:portal_wrong_question_status"
--   或调管理端「缓存监控 → 数据字典」清理接口（CacheController /system/dict/dictCache）
-- ============================================================

DELETE FROM sys_dict_data WHERE dict_type IN (
  'portal_question_type','cms_contest_status','portal_book_type',
  'portal_book_serial_status','portal_wallet_txn_type','portal_wrong_question_status');

INSERT INTO sys_dict_data (dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
 -- 题目类型（与 QuestionDetailPage 图标/色板配套；list_class 供徽章样式）
 (1,'算法','algorithm','portal_question_type','','primary','Y','0','admin','2026-10-01 00:00:00','',NULL,'题目类型：algorithm','0'),
 (2,'八股','bagwen','portal_question_type','','info','N','0','admin','2026-10-01 00:00:00','',NULL,'题目类型：bagwen','0'),
 (3,'系统设计','system_design','portal_question_type','','warning','N','0','admin','2026-10-01 00:00:00','',NULL,'题目类型：system_design','0'),
 (4,'项目','project','portal_question_type','','success','N','0','admin','2026-10-01 00:00:00','',NULL,'题目类型：project','0'),
 (5,'HR','hr','portal_question_type','','danger','N','0','admin','2026-10-01 00:00:00','',NULL,'题目类型：hr','0'),
 -- 征文活动状态
 (1,'草稿','draft','cms_contest_status','','info','N','0','admin','2026-10-01 00:00:00','',NULL,'征文状态：draft','0'),
 (2,'征稿中','collecting','cms_contest_status','','success','Y','0','admin','2026-10-01 00:00:00','',NULL,'征文状态：collecting','0'),
 (3,'投票中','voting','cms_contest_status','','warning','N','0','admin','2026-10-01 00:00:00','',NULL,'征文状态：voting','0'),
 (4,'已结束','ended','cms_contest_status','','default','N','0','admin','2026-10-01 00:00:00','',NULL,'征文状态：ended','0'),
 -- 书籍类型
 (1,'网络小说','novel','portal_book_type','','primary','Y','0','admin','2026-10-01 00:00:00','',NULL,'书籍类型：novel','0'),
 (2,'长文文章','longform','portal_book_type','','info','N','0','admin','2026-10-01 00:00:00','',NULL,'书籍类型：longform','0'),
 (3,'出版书籍','published','portal_book_type','','success','N','0','admin','2026-10-01 00:00:00','',NULL,'书籍类型：published','0'),
 -- 书籍连载状态
 (1,'连载中','ongoing','portal_book_serial_status','','warning','Y','0','admin','2026-10-01 00:00:00','',NULL,'连载状态：ongoing','0'),
 (2,'已完结','completed','portal_book_serial_status','','success','N','0','admin','2026-10-01 00:00:00','',NULL,'连载状态：completed','0'),
 (3,'暂停更新','hiatus','portal_book_serial_status','','info','N','0','admin','2026-10-01 00:00:00','',NULL,'连载状态：hiatus','0'),
 -- 钱包交易类型（vip 为后端真实写入值，原先页面写死 member 从未命中）
 (1,'打赏','tip','portal_wallet_txn_type','','primary','N','0','admin','2026-10-01 00:00:00','',NULL,'流水类型：tip','0'),
 (2,'提现','withdraw','portal_wallet_txn_type','','warning','N','0','admin','2026-10-01 00:00:00','',NULL,'流水类型：withdraw','0'),
 (3,'会员','vip','portal_wallet_txn_type','','success','N','0','admin','2026-10-01 00:00:00','',NULL,'流水类型：vip（后端实际写入值）','0'),
 -- 错题状态（仅 wrong/mastered；reviewing 无写入点，v13.84 已移除）
 (1,'待复习','wrong','portal_wrong_question_status','','warning','Y','0','admin','2026-10-01 00:00:00','',NULL,'错题状态：wrong','0'),
 (2,'已掌握','mastered','portal_wrong_question_status','','success','N','0','admin','2026-10-01 00:00:00','',NULL,'错题状态：mastered','0');

-- 复核：各行期望 5/4/3/3/3/2
SELECT dict_type, COUNT(*) AS rows_cnt FROM sys_dict_data
 WHERE dict_type IN ('portal_question_type','cms_contest_status','portal_book_type',
                     'portal_book_serial_status','portal_wallet_txn_type','portal_wrong_question_status')
 GROUP BY dict_type ORDER BY dict_type;
