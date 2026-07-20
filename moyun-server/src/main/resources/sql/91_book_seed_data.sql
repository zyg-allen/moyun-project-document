-- ============================================================
-- 脚本编号：91
-- 脚本名称：读书空间示例书籍数据初始化
-- 说明：导入一本完整的技术书籍《工程师修炼之道：从码农到架构师》
--       含 10 章正文 + 6 条金句摘录 + 2 条推荐位
-- 涉及表：portal_book / portal_book_chapter / portal_book_quote / portal_book_recommend
-- 幂等设计：可重复执行（INSERT IGNORE + 变量引用 book_id）
-- ============================================================

-- ------------------------------------------------------------
-- 1. 书籍主表 portal_book（幂等：按 title 去重）
-- ------------------------------------------------------------
INSERT IGNORE INTO `portal_book` (
    `title`, `author`, `cover`, `description`, `summary`, `isbn`,
    `publisher`, `publish_date`, `page_count`, `category_id`, `tags`,
    `rating`, `reading_count`, `status`, `type`, `serial_status`,
    `word_count`, `chapter_count`, `is_finished`, `access_level`,
    `preview_ratio`, `price`, `is_featured`, `is_recommended`,
    `author_bio`, `create_by`, `create_time`
) VALUES (
    '工程师修炼之道：从码农到架构师',
    '墨韵技术社',
    'https://images.moyun.com/books/engineer-way-cover.jpg',
    '本书写给所有在代码世界里摸爬滚打的工程师。从写出第一行可运行的代码，到设计支撑百万并发的系统，这条路没有捷径，但有方向。十个章节，覆盖代码质量、系统设计、数据库、API、并发、安全、性能、DevOps 到技术领导力，每一章都是一次认知升级。',
    '一本面向中高级工程师的实战进阶指南，覆盖代码质量、架构设计、数据库优化、并发编程、安全防护、性能调优与团队协作，用真实案例讲透从"能写代码"到"能扛系统"的完整路径。',
    '978-7-2026-0001-1',
    '墨韵出版社',
    '2026-06-15',
    320,
    NULL,
    '工程师,架构,后端,成长,系统设计',
    4.85,
    0,
    'active',
    'published',
    'completed',
    85000,
    10,
    1,
    'free',
    100,
    0.00,
    1,
    1,
    '墨韵技术社，由多位一线互联网公司资深工程师组成，专注于技术写作与工程实践传播。',
    'admin',
    NOW()
);

-- 取回书籍 ID（幂等：已存在则查已存记录）
SET @book_id = (SELECT id FROM `portal_book` WHERE `title` = '工程师修炼之道：从码农到架构师' LIMIT 1);

-- ------------------------------------------------------------
-- 2. 章节表 portal_book_chapter（10 章，幂等：uk_book_chapter_no 去重）
-- ------------------------------------------------------------
INSERT IGNORE INTO `portal_book_chapter` (
    `book_id`, `title`, `content`, `content_markdown`, `editor_mode`,
    `word_count`, `chapter_no`, `is_free`, `price`, `is_published`,
    `publish_time`, `view_count`, `create_by`, `create_time`
) VALUES
(@book_id, '第一章 工程师的成长路径',
'<h2>1.1 从码农到工程师</h2><p>很多人把"写代码"等同于"做工程"，这是一个常见的认知偏差。写代码只是手段，解决问题才是目的。一个成熟的工程师，首先想的不是用什么框架，而是这个问题本质是什么、边界在哪、谁来用、用多久。</p><p>成长路径通常分为三个阶段：能完成（把需求变成可运行代码）、能做对（考虑边界、异常、可维护性）、能扛事（对系统的可用性、成本、演进负责）。多数人卡在第一阶段到第二阶段的跨越，因为那意味着从"实现思维"转向"工程思维"。</p><h2>1.2 技术深度的三个层次</h2><p>第一层：会用。知道 API 怎么调，框架怎么配。</p><p>第二层：懂原理。知道 API 背后做了什么，框架的设计权衡是什么。</p><p>第三层：能造轮子。在理解原理的基础上，能针对自己的场景设计替代方案。注意，能造轮子不等于一定要造，但具备这个能力，意味着你对技术的理解已经穿透了表象。</p><h2>1.3 刻意练习</h2><p>读源码是高效的刻意练习方式之一。但不要从头到尾通读，而是带着问题去读：为什么这里用策略模式而不是 if-else？为什么这个缓存要用 ConcurrentHashMap 而不是 HashMap 加锁？每一个"为什么"的答案，都是一次认知边界的扩展。</p>',
'## 1.1 从码农到工程师\n\n很多人把"写代码"等同于"做工程"，这是一个常见的认知偏差。写代码只是手段，解决问题才是目的。一个成熟的工程师，首先想的不是用什么框架，而是这个问题本质是什么、边界在哪、谁来用、用多久。\n\n成长路径通常分为三个阶段：能完成（把需求变成可运行代码）、能做对（考虑边界、异常、可维护性）、能扛事（对系统的可用性、成本、演进负责）。多数人卡在第一阶段到第二阶段的跨越，因为那意味着从"实现思维"转向"工程思维"。\n\n## 1.2 技术深度的三个层次\n\n第一层：会用。知道 API 怎么调，框架怎么配。\n\n第二层：懂原理。知道 API 背后做了什么，框架的设计权衡是什么。\n\n第三层：能造轮子。在理解原理的基础上，能针对自己的场景设计替代方案。注意，能造轮子不等于一定要造，但具备这个能力，意味着你对技术的理解已经穿透了表象。\n\n## 1.3 刻意练习\n\n读源码是高效的刻意练习方式之一。但不要从头到尾通读，而是带着问题去读：为什么这里用策略模式而不是 if-else？为什么这个缓存要用 ConcurrentHashMap 而不是 HashMap 加锁？每一个"为什么"的答案，都是一次认知边界的扩展。',
'richtext', 580, 1, 1, 0.00, 1, NOW(), 0, 'admin', NOW()),

(@book_id, '第二章 代码质量与整洁之道',
'<h2>2.1 命名：最廉价的工程质量</h2><p>好的命名是自解释的，读到名字就知道它在做什么，不需要跳进去看实现。坏命名有三个典型特征：缩写（usr、cnt、flg）、泛化（data、info、manager）、误导（叫 list 实际是 map）。</p><h2>2.2 函数：短小再短小</h2><p>一个函数只做一件事。判断标准：能否用一句话描述它的职责而不用"和"字。如果"做A和做B"，就该拆成两个函数。短函数的好处不是"代码行数少"，而是降低认知负担——人脑能同时持有的上下文是有限的。</p><h2>2.3 注释：写"为什么"而不是"是什么"</h2><p>代码已经说了"是什么"，注释要补的是"为什么"。比如 <code>// 这里 +1 是因为后端分页从0开始，前端从1开始</code> 是好注释；<code>// 循环数组</code> 就是废话。</p><h2>2.4 异常处理</h2><p>不要吞异常。<code>catch(Exception e) {}</code> 是工程灾难。要么处理、要么抛出、要么转换成业务异常并记日志。静默吞掉的异常，会在生产环境变成无法定位的幽灵问题。</p>',
'## 2.1 命名：最廉价的工程质量\n\n好的命名是自解释的，读到名字就知道它在做什么，不需要跳进去看实现。坏命名有三个典型特征：缩写（usr、cnt、flg）、泛化（data、info、manager）、误导（叫 list 实际是 map）。\n\n## 2.2 函数：短小再短小\n\n一个函数只做一件事。判断标准：能否用一句话描述它的职责而不用"和"字。如果"做A和做B"，就该拆成两个函数。短函数的好处不是"代码行数少"，而是降低认知负担——人脑能同时持有的上下文是有限的。\n\n## 2.3 注释：写"为什么"而不是"是什么"\n\n代码已经说了"是什么"，注释要补的是"为什么"。比如 `// 这里 +1 是因为后端分页从0开始，前端从1开始` 是好注释；`// 循环数组` 就是废话。\n\n## 2.4 异常处理\n\n不要吞异常。`catch(Exception e) {}` 是工程灾难。要么处理、要么抛出、要么转换成业务异常并记日志。静默吞掉的异常，会在生产环境变成无法定位的幽灵问题。',
'richtext', 520, 2, 1, 0.00, 1, NOW(), 0, 'admin', NOW()),

(@book_id, '第三章 系统设计与架构思维',
'<h2>3.1 架构的本质是权衡</h2><p>没有"最好"的架构，只有"最合适"的架构。单体还是微服务、强一致还是最终一致、同步还是异步，每个选择背后都是 trade-off。架构师的工作不是选"最优解"，而是在成本、人力、时间、复杂度之间找到当前阶段最合理的平衡点。</p><h2>3.2 分层与解耦</h2><p>经典三层架构（Controller-Service-Mapper）不是教条，而是关注点分离的实践。每一层只关心自己的职责：Controller 校验入参和组装响应，Service 编排业务，Mapper 持久化。跨层调用（比如 Controller 直接调 Mapper）是架构腐化的开始。</p><h2>3.3 面向接口编程</h2><p>依赖抽象，不依赖具体。Service 调用 Mapper 时依赖接口（IPortalBookMapper），而不是实现类。这样换实现（比如从 MySQL 换 ES）时，上层无需改动。这是开闭原则在工程中的落地。</p><h2>3.4 演进式架构</h2><p>不要一开始就设计"完美架构"。先做能跑的，再做能扩展的，最后才是能演进的。过早优化和过度设计，比不做设计更危险。</p>',
'## 3.1 架构的本质是权衡\n\n没有"最好"的架构，只有"最合适"的架构。单体还是微服务、强一致还是最终一致、同步还是异步，每个选择背后都是 trade-off。架构师的工作不是选"最优解"，而是在成本、人力、时间、复杂度之间找到当前阶段最合理的平衡点。\n\n## 3.2 分层与解耦\n\n经典三层架构（Controller-Service-Mapper）不是教条，而是关注点分离的实践。每一层只关心自己的职责：Controller 校验入参和组装响应，Service 编排业务，Mapper 持久化。跨层调用（比如 Controller 直接调 Mapper）是架构腐化的开始。\n\n## 3.3 面向接口编程\n\n依赖抽象，不依赖具体。Service 调用 Mapper 时依赖接口（IPortalBookMapper），而不是实现类。这样换实现（比如从 MySQL 换 ES）时，上层无需改动。这是开闭原则在工程中的落地。\n\n## 3.4 演进式架构\n\n不要一开始就设计"完美架构"。先做能跑的，再做能扩展的，最后才是能演进的。过早优化和过度设计，比不做设计更危险。',
'richtext', 610, 3, 1, 0.00, 1, NOW(), 0, 'admin', NOW()),

(@book_id, '第四章 数据库设计与优化',
'<h2>4.1 索引：查询的加速器</h2><p>索引不是越多越好。每个索引都有写入开销和维护成本。建立索引的三原则：查询频次高、区分度高、覆盖查询字段。区分度低于 30% 的列建索引基本无效（比如 status 只有 0/1 两个值）。</p><p>联合索引遵循最左前缀原则。索引 (user_id, status, create_time) 能命中 user_id 单独查询，但命中不了 status 单独查询。</p><h2>4.2 事务与隔离级别</h2><p>MySQL 默认隔离级别是 RR（可重复读），通过 MVCC + 间隙锁实现。但在高并发写场景下，RR 的间隙锁会导致锁等待，适当降到 RC（读已提交）能提升吞吐。注意 RC 会引入幻读，需业务层兜底。</p><h2>4.3 分页优化</h2><p>深分页 <code>LIMIT 1000000, 20</code> 极慢，因为要扫描 100 万行再丢弃。优化方案：用游标分页 <code>WHERE id &gt; #{lastId} LIMIT 20</code>，或用覆盖索引子查询。</p><pre><code>-- 慢\nSELECT * FROM article ORDER BY id LIMIT 1000000, 20;\n-- 快（游标分页）\nSELECT * FROM article WHERE id &gt; #{lastId} ORDER BY id LIMIT 20;</code></pre><h2>4.4 避免 N+1 查询</h2><p>循环里查数据库是性能杀手。批量查询 + 内存组装，比循环单查快几个数量级。</p>',
'## 4.1 索引：查询的加速器\n\n索引不是越多越好。每个索引都有写入开销和维护成本。建立索引的三原则：查询频次高、区分度高、覆盖查询字段。区分度低于 30% 的列建索引基本无效（比如 status 只有 0/1 两个值）。\n\n联合索引遵循最左前缀原则。索引 `(user_id, status, create_time)` 能命中 user_id 单独查询，但命中不了 status 单独查询。\n\n## 4.2 事务与隔离级别\n\nMySQL 默认隔离级别是 RR（可重复读），通过 MVCC + 间隙锁实现。但在高并发写场景下，RR 的间隙锁会导致锁等待，适当降到 RC（读已提交）能提升吞吐。注意 RC 会引入幻读，需业务层兜底。\n\n## 4.3 分页优化\n\n深分页 `LIMIT 1000000, 20` 极慢，因为要扫描 100 万行再丢弃。优化方案：用游标分页 `WHERE id > #{lastId} LIMIT 20`，或用覆盖索引子查询。\n\n```sql\n-- 慢\nSELECT * FROM article ORDER BY id LIMIT 1000000, 20;\n-- 快（游标分页）\nSELECT * FROM article WHERE id > #{lastId} ORDER BY id LIMIT 20;\n```\n\n## 4.4 避免 N+1 查询\n\n循环里查数据库是性能杀手。批量查询 + 内存组装，比循环单查快几个数量级。',
'richtext', 750, 4, 1, 0.00, 1, NOW(), 0, 'admin', NOW()),

(@book_id, '第五章 API 设计原则',
'<h2>5.1 RESTful 不是教条</h2><p>REST 风格的 API 用 HTTP 动词表达意图（GET 查、POST 增、PUT 改、DELETE 删），用 URL 表达资源。但不要为了 REST 而 REST——比如"批量删除"用 DELETE 传数组 body 就很别扭，此时 POST /batch-delete 更务实。</p><h2>5.2 版本控制</h2><p>API 一旦上线就有人依赖，改动是破坏性的。用 <code>/v1/articles</code> 而不是 <code>/articles</code>，给未来留演进空间。大版本用路径区分，小版本用 header 或 query。</p><h2>5.3 统一响应结构</h2><p>所有接口返回统一结构：<code>{code, msg, data}</code>。code=200 成功，其他失败。这样前端只需一套拦截器处理，不用每个接口判断不同格式。</p><pre><code>{\n  "code": 200,\n  "msg": "success",\n  "data": {...}\n}</code></pre><h2>5.4 幂等性</h2><p>POST 创建接口要考虑幂等：用户点两次"提交"按钮，不应该创建两条数据。方案：前端传 clientToken，后端用 Redis SETNX 去重，或用唯一索引兜底。</p>',
'## 5.1 RESTful 不是教条\n\nREST 风格的 API 用 HTTP 动词表达意图（GET 查、POST 增、PUT 改、DELETE 删），用 URL 表达资源。但不要为了 REST 而 REST——比如"批量删除"用 DELETE 传数组 body 就很别扭，此时 `POST /batch-delete` 更务实。\n\n## 5.2 版本控制\n\nAPI 一旦上线就有人依赖，改动是破坏性的。用 `/v1/articles` 而不是 `/articles`，给未来留演进空间。大版本用路径区分，小版本用 header 或 query。\n\n## 5.3 统一响应结构\n\n所有接口返回统一结构：`{code, msg, data}`。code=200 成功，其他失败。这样前端只需一套拦截器处理，不用每个接口判断不同格式。\n\n```json\n{\n  "code": 200,\n  "msg": "success",\n  "data": {...}\n}\n```\n\n## 5.4 幂等性\n\nPOST 创建接口要考虑幂等：用户点两次"提交"按钮，不应该创建两条数据。方案：前端传 clientToken，后端用 Redis SETNX 去重，或用唯一索引兜底。',
'richtext', 560, 5, 1, 0.00, 1, NOW(), 0, 'admin', NOW()),

(@book_id, '第六章 并发编程实战',
'<h2>6.1 并发问题的根源</h2><p>并发问题的本质是"共享可变状态"。多个线程同时读写同一份数据，没有同步就会出错。解决思路三条：不共享（ThreadLocal）、不修改（不可变对象）、加锁（同步）。</p><h2>6.2 锁的层级</h2><p>从轻到重：原子类（CAS）→ 读写锁（ReentrantReadWriteLock）→ 互斥锁（synchronized / ReentrantLock）。能用原子类就别用锁，能用读写锁就别用互斥锁。锁粒度越小，并发度越高。</p><h2>6.3 ConcurrentHashMap 的正确用法</h2><p>CHM 的 get/put 是线程安全的，但"读-判断-写"复合操作不是。</p><pre><code>// 错误：复合操作非原子\nif (!map.containsKey(key)) {\n    map.put(key, value);\n}\n// 正确：用原子方法\nmap.putIfAbsent(key, value);</code></pre><h2>6.4 线程池不要用 Executors 创建</h2><p>Executors.newFixedThreadPool 用的是无界队列，OOM 风险。用 ThreadPoolExecutor 显式指定队列容量和拒绝策略。</p><pre><code>new ThreadPoolExecutor(\n    8, 16, 60, TimeUnit.SECONDS,\n    new LinkedBlockingQueue<>(200),\n    new ThreadFactoryBuilder().setNameFormat("biz-pool-%d").build(),\n    new ThreadPoolExecutor.CallerRunsPolicy() // 拒绝策略：让调用方执行，形成背压\n);</code></pre>',
'## 6.1 并发问题的根源\n\n并发问题的本质是"共享可变状态"。多个线程同时读写同一份数据，没有同步就会出错。解决思路三条：不共享（ThreadLocal）、不修改（不可变对象）、加锁（同步）。\n\n## 6.2 锁的层级\n\n从轻到重：原子类（CAS）→ 读写锁（ReentrantReadWriteLock）→ 互斥锁（synchronized / ReentrantLock）。能用原子类就别用锁，能用读写锁就别用互斥锁。锁粒度越小，并发度越高。\n\n## 6.3 ConcurrentHashMap 的正确用法\n\nCHM 的 get/put 是线程安全的，但"读-判断-写"复合操作不是。\n\n```java\n// 错误：复合操作非原子\nif (!map.containsKey(key)) {\n    map.put(key, value);\n}\n// 正确：用原子方法\nmap.putIfAbsent(key, value);\n```\n\n## 6.4 线程池不要用 Executors 创建\n\nExecutors.newFixedThreadPool 用的是无界队列，OOM 风险。用 ThreadPoolExecutor 显式指定队列容量和拒绝策略。\n\n```java\nnew ThreadPoolExecutor(\n    8, 16, 60, TimeUnit.SECONDS,\n    new LinkedBlockingQueue<>(200),\n    new ThreadFactoryBuilder().setNameFormat("biz-pool-%d").build(),\n    new ThreadPoolExecutor.CallerRunsPolicy()\n);\n```',
'richtext', 820, 6, 1, 0.00, 1, NOW(), 0, 'admin', NOW()),

(@book_id, '第七章 安全防护要点',
'<h2>7.1 SQL 注入</h2><p>永远用参数化查询，不要拼接 SQL。MyBatis 的 #{} 是参数化（安全），${} 是字符串拼接（危险）。${} 只能用于动态表名/列名等不能参数化的场景，且必须做白名单校验。</p><h2>7.2 XSS</h2><p>用户输入的内容渲染到 HTML 时必须转义。用白名单方式：只允许安全标签和属性，其余全部过滤。Markdown 渲染器输出后必须过 sanitize。</p><h2>7.3 越权（IDOR）</h2><p>所有写操作前校验资源归属：这篇文章的 authorId 是不是当前用户？这个订单的 buyerId 是不是当前用户？不校验就是越权漏洞，任意用户可改/删他人数据。</p><pre><code>// 错误：只校验登录，不校验归属\nLong userId = getUserId();\narticleService.update(article); // article.authorId 可能是别人的\n\n// 正确：校验归属\nif (!article.getAuthorId().equals(userId)) {\n    throw new RuntimeException("无权操作他人文章");\n}</code></pre><h2>7.4 密码存储</h2><p>BCrypt 加盐哈希，永远不要明文存储。BCrypt 自带盐，且可调成本因子，是当前最推荐的方案。</p><h2>7.5 最小权限与 fail-fast</h2><p>安全配置缺失时应该拒绝启动，而不是回退到默认弱配置。密钥未配置就抛异常，而不是用硬编码备用密钥继续跑。</p>',
'## 7.1 SQL 注入\n\n永远用参数化查询，不要拼接 SQL。MyBatis 的 `#{}` 是参数化（安全），`${}` 是字符串拼接（危险）。`${}` 只能用于动态表名/列名等不能参数化的场景，且必须做白名单校验。\n\n## 7.2 XSS\n\n用户输入的内容渲染到 HTML 时必须转义。用白名单方式：只允许安全标签和属性，其余全部过滤。Markdown 渲染器输出后必须过 sanitize。\n\n## 7.3 越权（IDOR）\n\n所有写操作前校验资源归属：这篇文章的 authorId 是不是当前用户？这个订单的 buyerId 是不是当前用户？不校验就是越权漏洞，任意用户可改/删他人数据。\n\n```java\n// 错误：只校验登录，不校验归属\nLong userId = getUserId();\narticleService.update(article);\n\n// 正确：校验归属\nif (!article.getAuthorId().equals(userId)) {\n    throw new RuntimeException("无权操作他人文章");\n}\n```\n\n## 7.4 密码存储\n\nBCrypt 加盐哈希，永远不要明文存储。BCrypt 自带盐，且可调成本因子，是当前最推荐的方案。\n\n## 7.5 最小权限与 fail-fast\n\n安全配置缺失时应该拒绝启动，而不是回退到默认弱配置。密钥未配置就抛异常，而不是用硬编码备用密钥继续跑。',
'richtext', 780, 7, 1, 0.00, 1, NOW(), 0, 'admin', NOW()),

(@book_id, '第八章 性能调优方法论',
'<h2>8.1 先测量，再优化</h2><p>"过早优化是万恶之源"——但更糟的是凭感觉优化。优化前先用 APM 或日志定位瓶颈：是 CPU、IO、数据库、还是网络？优化数据库索引和优化网络调用是两个完全不同方向，不测量就是瞎猜。</p><h2>8.2 缓存分层</h2><p>缓存不是银弹，引入缓存就引入了一致性问题。分层策略：浏览器缓存 → CDN → 本地缓存（Caffeine）→ 分布式缓存（Redis）→ 数据库。能短就不长，能近就不远。</p><p>缓存三大问题：穿透（查不存在的 key）、击穿（热 key 过期）、雪崩（大量 key 同时过期）。穿透用布隆过滤器，击穿用互斥锁，雪崩用随机过期时间。</p><h2>8.3 异步化</h2><p>耗时操作（发邮件、推送、写日志、统计）异步化，主流程快速返回。用消息队列削峰填谷。但异步意味着最终一致，要考虑消息丢失和重复消费。</p><h2>8.4 数据库优化优先级</h2><p>SQL 优化 &gt; 索引优化 &gt; 表结构优化 &gt; 分库分表。成本从低到高，收益从快到慢。不要一上来就分库分表，先把 SQL 和索引调好。</p>',
'## 8.1 先测量，再优化\n\n"过早优化是万恶之源"——但更糟的是凭感觉优化。优化前先用 APM 或日志定位瓶颈：是 CPU、IO、数据库、还是网络？优化数据库索引和优化网络调用是两个完全不同方向，不测量就是瞎猜。\n\n## 8.2 缓存分层\n\n缓存不是银弹，引入缓存就引入了一致性问题。分层策略：浏览器缓存 → CDN → 本地缓存（Caffeine）→ 分布式缓存（Redis）→ 数据库。能短就不长，能近就不远。\n\n缓存三大问题：穿透（查不存在的 key）、击穿（热 key 过期）、雪崩（大量 key 同时过期）。穿透用布隆过滤器，击穿用互斥锁，雪崩用随机过期时间。\n\n## 8.3 异步化\n\n耗时操作（发邮件、推送、写日志、统计）异步化，主流程快速返回。用消息队列削峰填谷。但异步意味着最终一致，要考虑消息丢失和重复消费。\n\n## 8.4 数据库优化优先级\n\nSQL 优化 > 索引优化 > 表结构优化 > 分库分表。成本从低到高，收益从快到慢。不要一上来就分库分表，先把 SQL 和索引调好。',
'richtext', 690, 8, 1, 0.00, 1, NOW(), 0, 'admin', NOW()),

(@book_id, '第九章 DevOps 与持续交付',
'<h2>9.1 自动化是工程化的前提</h2><p>手动部署 = 不稳定。构建、测试、部署、回滚全链路自动化，是团队规模超过 5 人后的必备能力。CI/CD 不是工具，是一种工程纪律。</p><h2>9.2 容器化的边界</h2><p>容器不是银弹。无状态服务适合容器化（API、Web），有状态服务谨慎（数据库、消息队列建议用托管服务而非自建容器）。容器的价值在于环境一致性和快速伸缩，不是"用了就显得先进"。</p><h2>9.3 监控与告警</h2><p>没有监控的系统等于盲飞。三层监控：基础设施（CPU/内存/磁盘）、应用（QPS/延迟/错误率）、业务（订单量/转化率）。告警要精准，噪声告警会让团队麻木，最终忽略真正的故障。</p><h2>9.4 灰度发布</h2><p>不要一次性全量发布。灰度策略：先小流量验证，再逐步放量。有问题快速回滚，而不是在线上 debug。回滚机制比发版机制更重要。</p>',
'## 9.1 自动化是工程化的前提\n\n手动部署 = 不稳定。构建、测试、部署、回滚全链路自动化，是团队规模超过 5 人后的必备能力。CI/CD 不是工具，是一种工程纪律。\n\n## 9.2 容器化的边界\n\n容器不是银弹。无状态服务适合容器化（API、Web），有状态服务谨慎（数据库、消息队列建议用托管服务而非自建容器）。容器的价值在于环境一致性和快速伸缩，不是"用了就显得先进"。\n\n## 9.3 监控与告警\n\n没有监控的系统等于盲飞。三层监控：基础设施（CPU/内存/磁盘）、应用（QPS/延迟/错误率）、业务（订单量/转化率）。告警要精准，噪声告警会让团队麻木，最终忽略真正的故障。\n\n## 9.4 灰度发布\n\n不要一次性全量发布。灰度策略：先小流量验证，再逐步放量。有问题快速回滚，而不是在线上 debug。回滚机制比发版机制更重要。',
'richtext', 600, 9, 1, 0.00, 1, NOW(), 0, 'admin', NOW()),

(@book_id, '第十章 技术领导力',
'<h2>10.1 技术领导力 ≠ 管理岗</h2><p>技术领导力不是"带几个人"，而是"用技术影响团队的方向"。一个资深工程师的价值，不只是自己能写多少代码，而是能让团队少踩多少坑、少走多少弯路。</p><h2>10.2 技术决策的责任</h2><p>选型决策要留痕——写技术方案文档（RFC），记录为什么选 A 不选 B、当时的前提假设是什么。半年后回头看，能复盘决策是否正确，而不是凭记忆争论"当时为什么这么选"。</p><h2>10.3 代码审查的价值</h2><p>Code Review 不是找茬，是知识传递。好的 CR 关注三点：逻辑是否正确、边界是否覆盖、可维护性是否及格。风格问题交给 lint 工具，CR 聚焦在人和机器都难发现的问题上。</p><h2>10.4 成长是长期主义</h2><p>技术的红利是复利的。今天多读的一篇源码、多写的一个测试、多复盘的一个事故，短期看不出差别，三年后是分水岭。保持学习，保持输出，保持对技术的好奇心——这是工程师能走多远的根本。</p>',
'## 10.1 技术领导力 ≠ 管理岗\n\n技术领导力不是"带几个人"，而是"用技术影响团队的方向"。一个资深工程师的价值，不只是自己能写多少代码，而是能让团队少踩多少坑、少走多少弯路。\n\n## 10.2 技术决策的责任\n\n选型决策要留痕——写技术方案文档（RFC），记录为什么选 A 不选 B、当时的前提假设是什么。半年后回头看，能复盘决策是否正确，而不是凭记忆争论"当时为什么这么选"。\n\n## 10.3 代码审查的价值\n\nCode Review 不是找茬，是知识传递。好的 CR 关注三点：逻辑是否正确、边界是否覆盖、可维护性是否及格。风格问题交给 lint 工具，CR 聚焦在人和机器都难发现的问题上。\n\n## 10.4 成长是长期主义\n\n技术的红利是复利的。今天多读的一篇源码、多写的一个测试、多复盘的一个事故，短期看不出差别，三年后是分水岭。保持学习，保持输出，保持对技术的好奇心——这是工程师能走多远的根本。',
'richtext', 620, 10, 1, 0.00, 1, NOW(), 0, 'admin', NOW());

-- 更新书籍的最新章节信息
SET @latest_chapter_id = (SELECT id FROM `portal_book_chapter` WHERE `book_id` = @book_id ORDER BY `chapter_no` DESC LIMIT 1);
SET @latest_chapter_title = (SELECT `title` FROM `portal_book_chapter` WHERE `id` = @latest_chapter_id);

UPDATE `portal_book` SET
    `latest_chapter_id` = @latest_chapter_id,
    `latest_chapter_title` = @latest_chapter_title,
    `last_update_time` = NOW()
WHERE `id` = @book_id;

-- ------------------------------------------------------------
-- 3. 金句摘录 portal_book_quote（6 条，幂等：用内容去重）
-- ------------------------------------------------------------
-- 取一个已存在的用户作为金句作者（admin = user_id 1）
SET @quote_user_id = (SELECT id FROM `portal_user` WHERE `username` = 'admin' LIMIT 1);

INSERT IGNORE INTO `portal_book_quote` (
    `user_id`, `book_id`, `content`, `page`, `chapter`, `location`,
    `like_count`, `is_public`, `is_featured`, `create_by`, `create_time`
) VALUES
(@quote_user_id, @book_id,
 '架构师的工作不是选"最优解"，而是在成本、人力、时间、复杂度之间找到当前阶段最合理的平衡点。',
 NULL, '第一章', '1.1 从码农到工程师', 0, 1, 1, 'admin', NOW()),

(@quote_user_id, @book_id,
 '能造轮子不等于一定要造，但具备这个能力，意味着你对技术的理解已经穿透了表象。',
 NULL, '第一章', '1.2 技术深度的三个层次', 0, 1, 0, 'admin', NOW()),

(@quote_user_id, @book_id,
 '一个函数只做一件事。判断标准：能否用一句话描述它的职责而不用"和"字。',
 NULL, '第二章', '2.2 函数：短小再短小', 0, 1, 1, 'admin', NOW()),

(@quote_user_id, @book_id,
 '静默吞掉的异常，会在生产环境变成无法定位的幽灵问题。',
 NULL, '第二章', '2.4 异常处理', 0, 1, 0, 'admin', NOW()),

(@quote_user_id, @book_id,
 '安全配置缺失时应该拒绝启动，而不是回退到默认弱配置。',
 NULL, '第七章', '7.5 最小权限与 fail-fast', 0, 1, 1, 'admin', NOW()),

(@quote_user_id, @book_id,
 '技术的红利是复利的。今天多读的一篇源码、多复盘的一个事故，三年后是分水岭。',
 NULL, '第十章', '10.4 成长是长期主义', 0, 1, 1, 'admin', NOW());

-- ------------------------------------------------------------
-- 4. 推荐位 portal_book_recommend（2 条，幂等：uk_book_position 去重）
-- ------------------------------------------------------------
INSERT IGNORE INTO `portal_book_recommend` (
    `book_id`, `position`, `sort`, `start_time`, `end_time`, `is_active`,
    `create_by`, `create_time`
) VALUES
(@book_id, 'home_hot', 1, NULL, NULL, 1, 'admin', NOW()),
(@book_id, 'discover_banner', 3, NULL, NULL, 1, 'admin', NOW());

-- ============================================================
-- 数据导入完成
-- 涉及表：portal_book(1) + portal_book_chapter(10) + portal_book_quote(6) + portal_book_recommend(2)
-- 总计：19 条记录
-- ============================================================
