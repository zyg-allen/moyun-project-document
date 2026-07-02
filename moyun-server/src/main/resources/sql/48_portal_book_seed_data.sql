-- ============================================================
-- 读书模块 v1.0 第三阶段：种子数据补全
-- 创建时间：2026-07-01
-- 说明：
--   补全第三阶段功能所需的数据，让发现页/首页/章节阅读页有真实内容可展示：
--     1. 为已有书籍补充第三阶段字段（type/serial_status/word_count/chapter_count/latest_chapter 等）
--     2. 为 3 本书创建真实章节正文（portal_book_chapter，每本 3 章）
--     3. 创建推荐位数据（portal_book_recommend：discover_banner / limit_free / home_hot）
--   特性：幂等可重复执行（先清理旧数据再重建）
--   依赖：
--     - 26_reading_interview_test_data.sql 已执行（存在 portal_book 1-15 号书籍）
--     - 42_portal_book_chapter_init.sql 已执行（portal_book_chapter 表已建）
--     - 46_portal_book_recommend_init.sql 已执行（portal_book_recommend 表已建）
-- ============================================================

-- ============================================================
-- 一、清理旧数据（幂等：可重复执行）
-- ============================================================

-- 1.1 清理旧章节正文（清理本脚本管理的所有 15 本书）
DELETE FROM `portal_book_chapter` WHERE `book_id` BETWEEN 1 AND 15;

-- 1.2 清理旧推荐位数据（仅清理本脚本管理的 3 个位置）
DELETE FROM `portal_book_recommend`
WHERE `position` IN ('discover_banner', 'limit_free', 'home_hot');

-- ============================================================
-- 二、章节正文数据（3 本书 × 3 章 = 9 章）
-- ============================================================

-- -------------------------------------------------------
-- 书 1：代码整洁之道（3 章）
-- -------------------------------------------------------
INSERT INTO `portal_book_chapter`
(`book_id`, `title`, `content`, `content_markdown`, `editor_mode`, `word_count`, `chapter_no`, `is_free`, `is_published`, `publish_time`, `view_count`, `create_by`, `create_time`)
VALUES
(1, '第1章 整洁代码',
 '<h2>整洁代码的含义</h2><p>写整洁代码，是从"能让机器运行"走向"让人能读懂"的第一步。Bjarne Stroustrup 说过："整洁代码让别人读起来感到愉悦。"这不仅关乎审美，更关乎软件的可维护性。</p><h3>1.1 为什么要整洁</h3><p>代码被阅读的次数远多于被编写的次数。一份混乱的代码，在三个月后连作者自己都难以理解。整洁代码的核心目标，是降低理解成本，让团队协作更顺畅。</p><h3>1.2 童子军规则</h3><p>"让营地比你来时更干净。"每次提交代码时，都比之前更好一点。改名一个含糊的变量、拆分一个过长的函数、删除一段无用的注释，都是改善。</p><blockquote>整洁代码不是一次性的重构，而是日复一日的习惯。</blockquote>',
 '# 第1章 整洁代码\n\n整洁代码让别人读起来感到愉悦。代码被阅读的次数远多于被编写的次数。\n\n## 1.1 为什么要整洁\n\n一份混乱的代码，在三个月后连作者自己都难以理解。整洁代码的核心目标，是降低理解成本。\n\n## 1.2 童子军规则\n\n让营地比你来时更干净。每次提交代码时，都比之前更好一点。',
 'markdown', 320, 1, 1, 1, NOW(), 120, 'admin', NOW()),
(1, '第2章 有意义的命名',
 '<h2>命名是程序员的第一难题</h2><p>好的命名能让代码自解释，坏的命名会让读者陷入猜谜游戏。Phil Karlton 说："在计算机科学中只有两件难事：缓存失效和命名。"</p><h3>2.1 名副其实</h3><p>变量名应该说明它"是什么"，而不是"怎么做"。例如 <code>d</code> 不如 <code>daysSinceCreation</code> 清晰。</p><h3>2.2 避免误导</h3><p>不要用 <code>accountList</code> 来表示一个账号组——除非它真的是 List 类型，否则用 <code>accounts</code> 更安全。</p><h3>2.3 有意义的区分</h3><p><code>getActiveAccount()</code> 和 <code>getActiveAccounts()</code> 同时存在是无意义的区分，<code>getActiveAccountInfo()</code> 同样糟糕。</p>',
 '# 第2章 有意义的命名\n\n好的命名能让代码自解释。Phil Karlton 说：在计算机科学中只有两件难事：缓存失效和命名。\n\n## 2.1 名副其实\n\n变量名应该说明它"是什么"。\n\n## 2.2 避免误导\n\n不要用 accountList 来表示一个账号组。\n\n## 2.3 有意义的区分\n\ngetActiveAccount 和 getActiveAccounts 同时存在是无意义的区分。',
 'markdown', 380, 2, 1, 1, NOW(), 85, 'admin', NOW()),
(1, '第3章 函数',
 '<h2>函数应该短小精悍</h2><p>函数的第一规则：要短小。函数的第二规则：还要更短小。20 世纪 60 年代的函数平均 50 行，而今天我们追求 4-10 行的函数。</p><h3>3.1 只做一件事</h3><p>函数应该只做一件事，做好这件事，只做这一件事。判断方法：能否再拆出一个子函数？</p><h3>3.2 参数</h3><p>最理想的参数数量是 0，其次是 1，尽量避免 3 个以上的参数。参数越多，测试组合越多。</p><h3>3.3 无副作用</h3><p>函数承诺做一件事，却偷偷做了别的事——这是 bug 的温床。</p><pre><code>// 坏例子：检查密码的同时初始化了会话\nfunction checkPassword(user, password) {\n  if (user.password === password) {\n    Session.initialize();  // 副作用\n    return true;\n  }\n  return false;\n}</code></pre>',
 '# 第3章 函数\n\n函数的第一规则：要短小。函数的第二规则：还要更短小。\n\n## 3.1 只做一件事\n\n函数应该只做一件事，做好这件事，只做这一件事。\n\n## 3.2 参数\n\n最理想的参数数量是 0，其次是 1，尽量避免 3 个以上的参数。\n\n## 3.3 无副作用\n\n函数承诺做一件事，却偷偷做了别的事——这是 bug 的温床。',
 'markdown', 450, 3, 1, 1, NOW(), 96, 'admin', NOW());

-- -------------------------------------------------------
-- 书 3：活着（3 章，文学小说）
-- -------------------------------------------------------
INSERT INTO `portal_book_chapter`
(`book_id`, `title`, `content`, `content_markdown`, `editor_mode`, `word_count`, `chapter_no`, `is_free`, `is_published`, `publish_time`, `view_count`, `create_by`, `create_time`)
VALUES
(3, '第一章 少爷福贵',
 '<p>我比现在年轻十岁的时候，获得了一个游手好闲的职业，去乡间收集民间歌谣。那一年的整个夏天，如同一只乱飞的麻雀，游荡在知了和阳光充斥的村庄。我喜欢喝农民那种带有苦味的茶水，他们的茶桶就放在田埂的树下，我毫无顾忌地拿起被他们用过的满是尘土的碗，舀水喝。</p><p>那时候我刚刚结束了和家珍的婚事，家珍是米行老板的女儿，她家有良田百亩。我爹总是说，我们家从前也是地主，只是到了我爷爷那一辈败了不少。我爹说这话的时候，总是拿眼睛瞟我，我知道他的意思是让我别学我爷爷。</p><p>可我那时候是个败家子，穿着丝绸，整天往城里跑，不是赌钱就是去妓院。我爹气得直跺脚，他说："福贵啊，你这样下去，迟早要把家产败光。"我听了只是笑笑，心想，家产那么多，哪里败得光。</p><p>后来我遇见了龙二，他是个赌徒，赌技高超。我和他赌了一夜，输了一百多亩地。我爹知道后，当场气得吐血，没过几天就死了。我把剩下的地都卖了，搬出大宅院，住进了茅草屋。</p>',
 '# 第一章 少爷福贵\n\n我比现在年轻十岁的时候，获得了一个游手好闲的职业，去乡间收集民间歌谣。\n\n那时候我刚刚结束了和家珍的婚事，家珍是米行老板的女儿，她家有良田百亩。\n\n可我那时候是个败家子，穿着丝绸，整天往城里跑，不是赌钱就是去妓院。我爹气得直跺脚。\n\n后来我遇见了龙二，他是个赌徒，赌技高超。我和他赌了一夜，输了一百多亩地。',
 'markdown', 580, 1, 1, 1, NOW(), 320, 'admin', NOW()),
(3, '第二章 战乱与归乡',
 '<p>被抓壮丁的那一年，我正在城里给我娘抓药。一队国民党兵把我抓走了，一路上枪炮声不断，我吓得躲在战壕里，身边是成堆的尸体。老全说："福贵，你得活着回去，家里还有人等你。"</p><p>老全是个老兵，他知道怎么在战场上活下来。他教我趴下、装死、抢干粮。后来他被流弹打中，死在我怀里。我把他埋了，心里想着，我一定要活着回去。</p><p>打了三年仗，我被解放军俘虏了。解放军对我们这些俘虏很好，愿意回家的发路费。我拿着路费，一路往南走，走了半个月，终于回到了村里。</p><p>家珍抱着有庆出来接我，有庆已经三岁了，不认识我。我娘已经去世了，是家珍一个人拉扯着孩子。我抱着家珍哭了，说："我回来了，再也不走了。"</p>',
 '# 第二章 战乱与归乡\n\n被抓壮丁的那一年，我正在城里给我娘抓药。一队国民党兵把我抓走了。\n\n老全是个老兵，他知道怎么在战场上活下来。他教我趴下、装死、抢干粮。后来他被流弹打中，死在我怀里。\n\n打了三年仗，我被解放军俘虏了。解放军对我们这些俘虏很好，愿意回家的发路费。\n\n家珍抱着有庆出来接我，有庆已经三岁了，不认识我。',
 'markdown', 540, 2, 1, 1, NOW(), 280, 'admin', NOW()),
(3, '第三章 苦难与坚韧',
 '<p>大跃进那年，村里成立了人民公社，家里的锅都被收去炼钢了。有庆长大了，每天去放羊。有一天，县长夫人生孩子大出血，学校组织学生去献血，有庆的血型对得上，结果抽了太多血，人就没了。</p><p>我抱着有庆的身体，他从温热慢慢变凉。我恨那个县长，后来发现县长是春生——当年和我一起被俘虏的兄弟。我没办法恨他，只是说："你欠我们家一条命。"</p><p>家珍的病越来越重，软骨病，治不好。凤霞长大了，嫁给了城里的搬运工二喜。凤霞生孩子的时候，也大出血，孩子活了下来，凤霞却没了。</p><p>家珍熬到凤霞走后不久，也走了。二喜带着苦根（凤霞的孩子）过日子，后来工地上出事，二喜被水泥板砸死了。我带着苦根，苦根七岁那年，吃豆子撑死了——那时候太穷，孩子没吃过饱饭。</p><p>最后就剩我和一头老牛，我叫它福贵。我们两个老家伙，一起在田里慢慢活。</p>',
 '# 第三章 苦难与坚韧\n\n大跃进那年，村里成立了人民公社，家里的锅都被收去炼钢了。\n\n有庆长大了，每天去放羊。有一天，县长夫人生孩子大出血，学校组织学生去献血，有庆的血型对得上，结果抽了太多血，人就没了。\n\n家珍的病越来越重。凤霞生孩子的时候也大出血走了。\n\n最后就剩我和一头老牛，我叫它福贵。',
 'markdown', 620, 3, 1, 1, NOW(), 310, 'admin', NOW());

-- -------------------------------------------------------
-- 书 15：三体（3 章，科幻连载小说）
-- -------------------------------------------------------
INSERT INTO `portal_book_chapter`
(`book_id`, `title`, `content`, `content_markdown`, `editor_mode`, `word_count`, `chapter_no`, `is_free`, `is_published`, `publish_time`, `view_count`, `create_by`, `create_time`)
VALUES
(15, '第一章 科学边界',
 '<p>汪淼是一位纳米材料研究员，他在一场莫名其妙的邀请下，接触到了一个名为"科学边界"的神秘组织。这个组织的成员都是顶尖科学家，但他们似乎都在研究着同一个奇怪的现象——物理学不存在了。</p><p>就在汪淼接到邀请的同一周，物理学家杨冬自杀了。她在遗书中写道："物理学是一门不能自洽的学科。"她的自杀像一颗石子投入湖面，在科学界激起了一圈又一圈的涟漪。</p><p>汪淼发现，自己的视野中开始出现一个倒计时——一串只有他能看见的数字，每分每秒都在跳动。他试着停下手中的纳米研究，倒计时果然停下了。这意味着，有什么东西在监视他，并且能控制他所看到的世界。</p><p>警察史强找到了汪淼，这个粗犷的刑警告诉他，最近有太多科学家自杀了，上级让他调查。史强带汪淼去参加了一个秘密会议，会上有人提到"三体"——一个让所有接触它的人都陷入绝望的词。</p>',
 '# 第一章 科学边界\n\n汪淼是一位纳米材料研究员，他在一场莫名其妙的邀请下，接触到了一个名为"科学边界"的神秘组织。\n\n物理学家杨冬自杀了。她在遗书中写道：物理学是一门不能自洽的学科。\n\n汪淼发现，自己的视野中开始出现一个倒计时——一串只有他能看见的数字。\n\n警察史强找到了汪淼，这个粗犷的刑警告诉他，最近有太多科学家自杀了。',
 'markdown', 560, 1, 1, 1, NOW(), 450, 'admin', NOW()),
(15, '第二章 三体游戏',
 '<p>史强让汪淼去玩一个叫"三体"的网络游戏。汪淼登录后，发现自己置身于一个奇异的世界——这里的天空时而出现三个太阳，时而一个也没有。文明在"恒纪元"（稳定的气候）中诞生，又在"乱纪元"（极端气候）中毁灭，如此循环往复。</p><p>汪淼在游戏中遇到了周文王、墨子、牛顿——这些人都在尝试预测三体世界的运行规律，但都失败了。因为三体问题本质上是无解的，三个太阳的运动是混沌的。</p><p>汪淼逐渐意识到，这个游戏并不是虚构的，它是对一个真实存在的世界的模拟——一个被三个太阳交替统治的星球。那个世界的文明，已经经历了数百次的毁灭与重生。</p><p>在一次聚会中，汪淼见到了"科学边界"的核心成员申玉菲。申玉菲冷淡地告诉他："主在看着你。"汪淼不明白"主"是谁，但他感觉到，一个远超人类理解的智慧，正在注视着地球。</p>',
 '# 第二章 三体游戏\n\n史强让汪淼去玩一个叫"三体"的网络游戏。\n\n汪淼在游戏中遇到了周文王、墨子、牛顿——这些人都在尝试预测三体世界的运行规律，但都失败了。\n\n汪淼逐渐意识到，这个游戏并不是虚构的，它是对一个真实存在的世界的模拟。\n\n在一次聚会中，汪淼见到了"科学边界"的核心成员申玉菲。申玉菲冷淡地告诉他：主在看着你。',
 'markdown', 590, 2, 1, 1, NOW(), 380, 'admin', NOW()),
(15, '第三章 红岸基地',
 '<p>叶文洁的故事，要从文化大革命说起。她的父亲是一位物理学家，在批斗中被活活打死。叶文洁被下放到大兴安岭，在那里她遇到了一个改变她命运的信号——一段来自太空的电波。</p><p>叶文洁被秘密调到了一个名为"红岸"的基地——一个对外宣称是普通军事基地，实则是用于搜索外星文明的射电望远镜基地。叶文洁在红岸工作了多年，她利用太阳作为天线放大器，向宇宙发送了一段信号。</p><p>八年后，她收到了回复。回复的内容让她颤抖："不要回答！不要回答！不要回答！"——这是一位三体世界的和平主义者发来的警告，他告诉叶文洁，如果她再发一次信号，地球的位置就会被三体世界锁定，届时将面临毁灭。</p><p>但叶文洁还是按下了发送键。她对人类已经彻底失望——父亲被打死、母亲背叛、爱人利用她。她希望三体文明来到地球，"他们"会比人类更文明。这一按，开启了长达四百年的地球与三体的恩怨。</p>',
 '# 第三章 红岸基地\n\n叶文洁的故事，要从文化大革命说起。她的父亲是一位物理学家，在批斗中被活活打死。\n\n叶文洁被秘密调到了一个名为"红岸"的基地——一个用于搜索外星文明的射电望远镜基地。\n\n八年后，她收到了回复。回复的内容让她颤抖：不要回答！不要回答！不要回答！\n\n但叶文洁还是按下了发送键。她对人类已经彻底失望。这一按，开启了长达四百年的地球与三体的恩怨。',
 'markdown', 610, 3, 1, 1, NOW(), 420, 'admin', NOW());

-- ============================================================
-- 三、为其余 12 本书补章节正文（每本 1 章，保证书籍详情页有"开始阅读"按钮可点）
-- ============================================================

INSERT INTO `portal_book_chapter`
(`book_id`, `title`, `content`, `content_markdown`, `editor_mode`, `word_count`, `chapter_no`, `is_free`, `is_published`, `publish_time`, `view_count`, `create_by`, `create_time`)
VALUES
-- 书 2：深入理解计算机系统
(2, '第1章 计算机系统漫游',
 '<h2>信息就是位+上下文</h2><p>计算机系统由硬件和系统软件组成，它们共同协作来运行应用程序。信息在计算机内部以二进制位的形式存储，相同的字节序列可能表示整数、浮点数、字符串或机器指令，具体含义由上下文决定。</p><h3>编译系统</h3><p>一个 C 程序的生命周期从源文件开始，经过预处理、编译、汇编、链接四个阶段，最终生成可执行文件。理解编译过程有助于排查问题、优化性能、理解安全漏洞。</p><h3>存储器层次</h3><p>从寄存器、L1/L2/L3 缓存、主存到磁盘，每一层都更快但更小。程序员可以利用局部性原理让程序跑得更快。</p><blockquote>理解计算机系统，是写出高性能、可靠、安全程序的前提。</blockquote>',
 '# 第1章 计算机系统漫游\n\n信息就是位+上下文。\n\n## 编译系统\n\n一个 C 程序经过预处理、编译、汇编、链接四个阶段。\n\n## 存储器层次\n\n从寄存器到磁盘，每一层都更快但更小。',
 'markdown', 380, 1, 1, 1, NOW(), 220, 'admin', NOW()),

-- 书 4：人类简史
(4, '第1章 认知革命',
 '<p>大约 7 万年前，智人开始做出非常特别的事情——他们开始讲故事，讲不存在的事情。神话、传说、神祇、宗教由此诞生。这种虚构的能力，让智人能够大规模协作，最终征服了整个地球。</p><p>"标致"汽车公司只是一个法律虚构，但全世界数百万人都相信它存在。这种集体虚构让陌生人之间能够合作，这是人类独有的能力。</p><p>农业革命后，人类开始定居，人口爆炸，但个体生活质量可能下降——这就是历史的吊诡之处。我们以为我们驯化了小麦，其实是小麦驯化了我们。</p><blockquote>认知革命让智人学会了讲故事，也学会了共同相信一个虚构。</blockquote>',
 '# 第1章 认知革命\n\n大约 7 万年前，智人开始做出非常特别的事情——他们开始讲故事。\n\n"标致"汽车公司只是一个法律虚构。\n\n农业革命后，人类开始定居，人口爆炸，但个体生活质量可能下降。',
 'markdown', 420, 1, 1, 1, NOW(), 380, 'admin', NOW()),

-- 书 5：设计模式
(5, '第1章 引言',
 '<h2>什么是设计模式</h2><p>设计模式是面向对象软件设计的经验总结。每一个设计模式都系统地命名、解释和评价了面向对象系统中的一个重要且可复用的设计。</p><p>设计模式让使用者可以更加方便地复用成功的设计和架构。它们帮助开发者做出有利于系统复用的选择，避免那些会损害系统复用性的设计。</p><h3>四人帮（GoF）</h3><p>《设计模式》一书由 Erich Gamma、Richard Helm、Ralph Johnson、John Vlissides 四人合著，故称 GoF。书中收录 23 种经典设计模式，分为创建型、结构型、行为型三大类。</p><blockquote>设计模式不是教条，而是经验沉淀。</blockquote>',
 '# 第1章 引言\n\n设计模式是面向对象软件设计的经验总结。\n\n## 四人帮（GoF）\n\n《设计模式》一书由 Erich Gamma、Richard Helm、Ralph Johnson、John Vlissides 四人合著。',
 'markdown', 320, 1, 1, 1, NOW(), 180, 'admin', NOW()),

-- 书 6：Java 编程思想
(6, '第1章 对象导论',
 '<h2>万物皆对象</h2><p>Java 是一门纯粹的面向对象语言。在 Java 中，一切皆对象——每个变量都是某个类的实例，每个方法都依附于某个对象。这种思想让程序更易理解、更易复用。</p><p>Alan Kay 总结过面向对象的五大特征：万物皆对象、程序是对象的集合、对象通过发消息通信、每个对象都有内存、每个对象都是某个类的实例。Smalltalk 是最早实践这一思想的语言。</p><p>Java 借鉴了 C++ 的语法和 Smalltalk 的对象模型，但又去掉了 C++ 的多重继承、指针等复杂特性，让语言更简单、更安全。</p><blockquote>对象不是银弹，但它是组织复杂软件的有效工具。</blockquote>',
 '# 第1章 对象导论\n\n万物皆对象。Java 是一门纯粹的面向对象语言。\n\nAlan Kay 总结过面向对象的五大特征。',
 'markdown', 360, 1, 1, 1, NOW(), 240, 'admin', NOW()),

-- 书 7：深入浅出 MySQL
(7, '第1章 MySQL 架构',
 '<h2>MySQL 整体架构</h2><p>MySQL 的整体架构分为三层：客户端/连接层、服务器层（含 SQL 接口、解析器、优化器、缓存）、存储引擎层。存储引擎是 MySQL 最具特色的部分，它采用插件式架构，允许开发者选择 InnoDB、MyISAM、Memory 等不同引擎。</p><p>InnoDB 是 MySQL 5.5 之后的默认引擎，支持事务、行级锁、外键。它通过 redo log 保证持久性，undo log 保证原子性，MVCC 实现可重复读隔离级别。</p><h3>一条 SQL 的旅程</h3><p>从客户端发起到查询缓存命中、解析器语法检查、优化器生成执行计划、执行器调用存储引擎接口，最终返回结果。每一步都可能影响 SQL 的执行效率。</p><blockquote>理解架构，才能优化数据库。</blockquote>',
 '# 第1章 MySQL 架构\n\nMySQL 的整体架构分为三层。\n\nInnoDB 是 MySQL 5.5 之后的默认引擎。\n\n一条 SQL 从客户端发起到返回结果，要经历多步。',
 'markdown', 400, 1, 1, 1, NOW(), 160, 'admin', NOW()),

-- 书 8：Redis 设计与实现
(8, '第1章 数据结构',
 '<h2>SDS：动态字符串</h2><p>Redis 没有直接使用 C 的字符串，而是自定义了 SDS（Simple Dynamic String）。SDS 在字符串头部记录了 len 和 free 字段，让 O(1) 获取长度、二进制安全、空间预分配成为可能。</p><h3>链表、字典、跳表</h3><p>链表用于 List 类型；字典用于 Hash，采用渐进式 rehash 避免一次性搬迁阻塞；跳表用于 ZSet，让有序集合的平均查找复杂度为 O(logN)。</p><p>Redis 之所以快：单线程避免上下文切换、内存操作、IO 多路复用、高效数据结构。</p><blockquote>理解 Redis，从理解它的数据结构开始。</blockquote>',
 '# 第1章 数据结构\n\nSDS：动态字符串。Redis 没有直接使用 C 的字符串。\n\n## 链表、字典、跳表\n\n跳表让有序集合的平均查找复杂度为 O(logN)。',
 'markdown', 340, 1, 1, 1, NOW(), 200, 'admin', NOW()),

-- 书 9：Spring 实战
(9, '第1章 Spring 核心',
 '<h2>Spring 是什么</h2><p>Spring 是一个开源的轻量级 Java 应用框架。它的核心是 IoC（控制反转）和 AOP（面向切面编程）。IoC 让对象的创建和管理交由容器负责，AOP 让横切关注点（日志、事务、安全）从业务代码中分离。</p><h3>依赖注入</h3><p>传统写法：对象自己 new 依赖。Spring 写法：对象声明依赖，容器注入。这让代码解耦、易于测试。</p><pre><code>@Service\npublic class UserService {\n    @Autowired\n    private UserRepository repo;\n}</code></pre><p>Spring Boot 在 Spring 之上做了约定优于配置的封装，让微服务开发更简单。</p><blockquote>Spring 的核心是 DI + AOP。</blockquote>',
 '# 第1章 Spring 核心\n\nSpring 是一个开源的轻量级 Java 应用框架。核心是 IoC 和 AOP。\n\n## 依赖注入\n\n传统写法：对象自己 new 依赖。Spring 写法：对象声明依赖，容器注入。',
 'markdown', 380, 1, 1, 1, NOW(), 170, 'admin', NOW()),

-- 书 10：算法导论
(10, '第1章 算法基础',
 '<h2>插入排序</h2><p>插入排序是一种原地排序算法，最坏情况 O(n²)，但对小规模或近似有序数据非常高效。它的核心思想是：把每个元素插入到已排序部分的合适位置。</p><h3>分治法</h3><p>归并排序采用分治思想：把数组对半切，递归排序，然后合并。时间复杂度 O(n log n)，但需要 O(n) 额外空间。</p><h3>渐进记号</h3><p>Θ 给出函数的上下界；O 给出上界；Ω 给出下界。大 O 是最常用的，它告诉我们算法在最坏情况下不会比什么更差。</p><blockquote>算法分析从渐进记号开始。</blockquote>',
 '# 第1章 算法基础\n\n插入排序是一种原地排序算法，最坏情况 O(n²)。\n\n## 分治法\n\n归并排序采用分治思想：把数组对半切，递归排序，然后合并。',
 'markdown', 350, 1, 1, 1, NOW(), 290, 'admin', NOW()),

-- 书 11：百年孤独
(11, '第1章 马孔多的诞生',
 '<p>多年以后，面对行刑队，奥雷里亚诺·布恩迪亚上校将会回想起父亲带他去见识冰块的那个遥远的下午。那时的马孔多是一个二十户人家的村落，房屋沿河而建，河水清澈，河床里卵石洁白光滑宛如史前巨蛋。</p><p>世界新生伊始，许多事物还没有名字，提到的时候尚需用手指指点点。每年三月前后，一家衣衫褴褛的吉卜赛人都会来到村边扎下帐篷，吹笛击鼓，吵吵嚷嚷地向人们展示新近的发明。</p><p>最初他们带来了磁铁。一个身形高大的吉卜赛人，胡须蓬乱，雀爪般的双手，自称梅尔基亚德斯，当众进行了惊人的演示——他把两块磁铁拖过房屋，铁锅、铁盆、铁钳、小炭炉纷纷从原地落下，木板因钉子和螺丝奋力挣脱而吱呀作响。</p><blockquote>多年以后，奥雷里亚诺·布恩迪亚上校面对行刑队，将会回想起父亲带他去见识冰块的那个遥远的下午。</blockquote>',
 '# 第1章 马孔多的诞生\n\n多年以后，面对行刑队，奥雷里亚诺·布恩迪亚上校将会回想起父亲带他去见识冰块的那个遥远的下午。\n\n世界新生伊始，许多事物还没有名字。',
 'markdown', 480, 1, 1, 1, NOW(), 410, 'admin', NOW()),

-- 书 12：围城
(12, '第1章 归国',
 '<p>红海早过了，船在印度洋面上开驶着，但是太阳依然不饶人地迟落，依然劝诱着人们早早地休息。在这蒸笼般的热天气里，<strong>方鸿渐</strong>正靠在船舷上，思念着家乡。</p><p>方鸿渐在欧洲游学了四年，辗转了伦敦、巴黎、柏林三所大学，却没拿到任何学位。他从爱尔兰人手里买了一张"克莱登大学"的假博士文凭，准备回国蒙混过关。</p><p>船到上海，他先去了岳父母家——他的未婚妻周淑英已经去世，但岳父仍念旧情，让他住在家里，并为他谋了一个银行的差事。方鸿渐从此开始了他在上海、内地之间的漂泊与"围城"人生。</p><blockquote>城外的人想进去，城里的人想出来。</blockquote>',
 '# 第1章 归国\n\n红海早过了，船在印度洋面上开驶着。\n\n方鸿渐在欧洲游学了四年，却没拿到任何学位。他从爱尔兰人手里买了一张"克莱登大学"的假博士文凭。',
 'markdown', 460, 1, 1, 1, NOW(), 350, 'admin', NOW()),

-- 书 13：平凡的世界
(13, '第1章 双水村的清晨',
 '<p>1975 年二、三月间，一个平平常常的日子，细蒙蒙的雨丝夹着一星半点的雪花，正纷纷淋淋地向大地飘洒着。时令已快到惊蛰，雪当然再不会下大了，但依然让人感到一种春寒料峭的凉意。</p><p>在黄土高原千沟万壑的褶皱里，有一个叫<strong>双水村</strong>的村庄。村东的庙坪上，一座破庙正进行着一种几乎原始的劳动——一群衣衫褴褛的农民，正光着膀子用木锨扬场。</p><p>村中半山腰的一孔窑洞里，<strong>孙少平</strong>正背着书包上学去。他是这村里少有的高中生，他的哥哥孙少安早早辍学务农，把希望寄托在了弟弟身上。这是一个平凡的世界，却有一群不甘平凡的人。</p><blockquote>生活不能等待别人来安排，要自己去争取。</blockquote>',
 '# 第1章 双水村的清晨\n\n1975 年二、三月间，一个平平常常的日子，细蒙蒙的雨丝夹着一星半点的雪花。\n\n在黄土高原千沟万壑的褶皱里，有一个叫双水村的村庄。',
 'markdown', 470, 1, 1, 1, NOW(), 320, 'admin', NOW()),

-- 书 14：重构
(14, '第1章 重构的第一个案例',
 '<h2>重构是什么</h2><p>重构是在不改变软件外部行为的前提下，调整其内部结构，使其更易理解、更易修改。它不是重写，而是小步、安全的改进。</p><h3>影片租赁系统</h3><p>一个简单的影片租赁系统：顾客租了影片，系统计算费用和积分。最初的代码把所有逻辑塞在一个方法里，每次新增影片类型都要修改这个方法。</p><p>重构步骤：先提取"计算费用"和"计算积分"两个方法，再引入"影片类型"的多态，最后把状态模式应用到"租赁"上。每一步都通过测试验证。</p><blockquote>重构是程序员的健身操，每天做一点，代码更健康。</blockquote>',
 '# 第1章 重构的第一个案例\n\n重构是在不改变软件外部行为的前提下，调整其内部结构。\n\n## 影片租赁系统\n\n一个简单的影片租赁系统：顾客租了影片，系统计算费用和积分。',
 'markdown', 360, 1, 1, 1, NOW(), 150, 'admin', NOW());

-- ============================================================
-- 四、更新全部 15 本书的第三阶段字段
-- ============================================================

-- 4.1 书 1（代码整洁之道）：已完结出版物
UPDATE `portal_book` SET
    `type` = 'published',
    `serial_status` = 'completed',
    `is_finished` = 1,
    `chapter_count` = 3,
    `word_count` = 1150,
    `latest_chapter_id` = (SELECT max_id FROM (SELECT MAX(`id`) AS max_id FROM `portal_book_chapter` WHERE `book_id` = 1) t),
    `latest_chapter_title` = '第3章 函数',
    `last_update_time` = NOW()
WHERE `id` = 1;

-- 4.2 书 3（活着）：已完结网络小说
UPDATE `portal_book` SET
    `type` = 'novel',
    `serial_status` = 'completed',
    `is_finished` = 1,
    `chapter_count` = 3,
    `word_count` = 1740,
    `latest_chapter_id` = (SELECT max_id FROM (SELECT MAX(`id`) AS max_id FROM `portal_book_chapter` WHERE `book_id` = 3) t),
    `latest_chapter_title` = '第三章 苦难与坚韧',
    `last_update_time` = NOW()
WHERE `id` = 3;

-- 4.3 书 15（三体）：连载中网络小说（让发现页"连载中"区块有数据）
UPDATE `portal_book` SET
    `type` = 'novel',
    `serial_status` = 'ongoing',
    `is_finished` = 0,
    `chapter_count` = 3,
    `word_count` = 1760,
    `latest_chapter_id` = (SELECT max_id FROM (SELECT MAX(`id`) AS max_id FROM `portal_book_chapter` WHERE `book_id` = 15) t),
    `latest_chapter_title` = '第三章 红岸基地',
    `last_update_time` = NOW()
WHERE `id` = 15;

-- 4.4 其余 12 本书（每本 1 章）：统一标记 + latest_chapter_id 关联（每本一条 UPDATE，兼容所有 MySQL 版本）
-- 书 2
UPDATE `portal_book` SET
    `type` = 'published', `serial_status` = 'completed', `is_finished` = 1, `chapter_count` = 1, `word_count` = 380,
    `latest_chapter_id` = (SELECT max_id FROM (SELECT MAX(`id`) AS max_id FROM `portal_book_chapter` WHERE `book_id` = 2) t),
    `latest_chapter_title` = '第1章 计算机系统漫游', `last_update_time` = NOW()
WHERE `id` = 2;
-- 书 4
UPDATE `portal_book` SET
    `type` = 'published', `serial_status` = 'completed', `is_finished` = 1, `chapter_count` = 1, `word_count` = 420,
    `latest_chapter_id` = (SELECT max_id FROM (SELECT MAX(`id`) AS max_id FROM `portal_book_chapter` WHERE `book_id` = 4) t),
    `latest_chapter_title` = '第1章 认知革命', `last_update_time` = NOW()
WHERE `id` = 4;
-- 书 5
UPDATE `portal_book` SET
    `type` = 'published', `serial_status` = 'completed', `is_finished` = 1, `chapter_count` = 1, `word_count` = 320,
    `latest_chapter_id` = (SELECT max_id FROM (SELECT MAX(`id`) AS max_id FROM `portal_book_chapter` WHERE `book_id` = 5) t),
    `latest_chapter_title` = '第1章 引言', `last_update_time` = NOW()
WHERE `id` = 5;
-- 书 6
UPDATE `portal_book` SET
    `type` = 'published', `serial_status` = 'completed', `is_finished` = 1, `chapter_count` = 1, `word_count` = 360,
    `latest_chapter_id` = (SELECT max_id FROM (SELECT MAX(`id`) AS max_id FROM `portal_book_chapter` WHERE `book_id` = 6) t),
    `latest_chapter_title` = '第1章 对象导论', `last_update_time` = NOW()
WHERE `id` = 6;
-- 书 7
UPDATE `portal_book` SET
    `type` = 'published', `serial_status` = 'completed', `is_finished` = 1, `chapter_count` = 1, `word_count` = 400,
    `latest_chapter_id` = (SELECT max_id FROM (SELECT MAX(`id`) AS max_id FROM `portal_book_chapter` WHERE `book_id` = 7) t),
    `latest_chapter_title` = '第1章 MySQL 架构', `last_update_time` = NOW()
WHERE `id` = 7;
-- 书 8
UPDATE `portal_book` SET
    `type` = 'published', `serial_status` = 'completed', `is_finished` = 1, `chapter_count` = 1, `word_count` = 340,
    `latest_chapter_id` = (SELECT max_id FROM (SELECT MAX(`id`) AS max_id FROM `portal_book_chapter` WHERE `book_id` = 8) t),
    `latest_chapter_title` = '第1章 数据结构', `last_update_time` = NOW()
WHERE `id` = 8;
-- 书 9
UPDATE `portal_book` SET
    `type` = 'published', `serial_status` = 'completed', `is_finished` = 1, `chapter_count` = 1, `word_count` = 380,
    `latest_chapter_id` = (SELECT max_id FROM (SELECT MAX(`id`) AS max_id FROM `portal_book_chapter` WHERE `book_id` = 9) t),
    `latest_chapter_title` = '第1章 Spring 核心', `last_update_time` = NOW()
WHERE `id` = 9;
-- 书 10
UPDATE `portal_book` SET
    `type` = 'published', `serial_status` = 'completed', `is_finished` = 1, `chapter_count` = 1, `word_count` = 350,
    `latest_chapter_id` = (SELECT max_id FROM (SELECT MAX(`id`) AS max_id FROM `portal_book_chapter` WHERE `book_id` = 10) t),
    `latest_chapter_title` = '第1章 算法基础', `last_update_time` = NOW()
WHERE `id` = 10;
-- 书 11
UPDATE `portal_book` SET
    `type` = 'published', `serial_status` = 'completed', `is_finished` = 1, `chapter_count` = 1, `word_count` = 480,
    `latest_chapter_id` = (SELECT max_id FROM (SELECT MAX(`id`) AS max_id FROM `portal_book_chapter` WHERE `book_id` = 11) t),
    `latest_chapter_title` = '第1章 马孔多的诞生', `last_update_time` = NOW()
WHERE `id` = 11;
-- 书 12
UPDATE `portal_book` SET
    `type` = 'published', `serial_status` = 'completed', `is_finished` = 1, `chapter_count` = 1, `word_count` = 460,
    `latest_chapter_id` = (SELECT max_id FROM (SELECT MAX(`id`) AS max_id FROM `portal_book_chapter` WHERE `book_id` = 12) t),
    `latest_chapter_title` = '第1章 归国', `last_update_time` = NOW()
WHERE `id` = 12;
-- 书 13
UPDATE `portal_book` SET
    `type` = 'published', `serial_status` = 'completed', `is_finished` = 1, `chapter_count` = 1, `word_count` = 470,
    `latest_chapter_id` = (SELECT max_id FROM (SELECT MAX(`id`) AS max_id FROM `portal_book_chapter` WHERE `book_id` = 13) t),
    `latest_chapter_title` = '第1章 双水村的清晨', `last_update_time` = NOW()
WHERE `id` = 13;
-- 书 14
UPDATE `portal_book` SET
    `type` = 'published', `serial_status` = 'completed', `is_finished` = 1, `chapter_count` = 1, `word_count` = 360,
    `latest_chapter_id` = (SELECT max_id FROM (SELECT MAX(`id`) AS max_id FROM `portal_book_chapter` WHERE `book_id` = 14) t),
    `latest_chapter_title` = '第1章 重构的第一个案例', `last_update_time` = NOW()
WHERE `id` = 14;

-- ============================================================
-- 四、推荐位数据（让发现页 Banner + 限免专区 + 首页热门有内容）
-- ============================================================

INSERT INTO `portal_book_recommend`
(`book_id`, `position`, `sort`, `start_time`, `end_time`, `is_active`, `remark`, `create_by`, `create_time`)
VALUES
-- 发现页 Banner（DiscoverPage 顶部轮播）
(4,  'discover_banner', 1, DATE_SUB(NOW(), INTERVAL 7 DAY), DATE_ADD(NOW(), INTERVAL 90 DAY), 1, '发现页Banner-人类简史', 'admin', NOW()),
(11, 'discover_banner', 2, DATE_SUB(NOW(), INTERVAL 7 DAY), DATE_ADD(NOW(), INTERVAL 90 DAY), 1, '发现页Banner-百年孤独', 'admin', NOW()),
(15, 'discover_banner', 3, DATE_SUB(NOW(), INTERVAL 7 DAY), DATE_ADD(NOW(), INTERVAL 90 DAY), 1, '发现页Banner-三体', 'admin', NOW()),
-- 限免专区（DiscoverPage + ReadingPage 限免区块）
(5,  'limit_free', 1, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 30 DAY), 1, '限免-设计模式', 'admin', NOW()),
(9,  'limit_free', 2, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 30 DAY), 1, '限免-Spring实战', 'admin', NOW()),
(7,  'limit_free', 3, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 30 DAY), 1, '限免-深入浅出MySQL', 'admin', NOW()),
-- 首页热门（ReadingPage 入口位，预留）
(1,  'home_hot', 1, NULL, NULL, 1, '首页热门-代码整洁之道', 'admin', NOW()),
(6,  'home_hot', 2, NULL, NULL, 1, '首页热门-Java编程思想', 'admin', NOW()),
(14, 'home_hot', 3, NULL, NULL, 1, '首页热门-重构', 'admin', NOW());

-- ============================================================
-- 脚本执行完成
-- ============================================================
SELECT '种子数据补全完成（幂等，可重复执行）！' AS message;
SELECT CONCAT('已创建章节：3本书×3章 + 12本书×1章 = 共21章；推荐位：discover_banner=3, limit_free=3, home_hot=3') AS detail;
