# VIP 体系完整设计方案

> 文档版本：v2.1
> 创建日期：2026-09-17
> 文档路径：docs/05-方案设计-分模块/3-vip体系/VIP体系完整设计方案-v2 · 端级粒度 + 全局公共端 + 注解驱动.md
> 状态：已评审通过（评审结论见 §16）
> 关联模块：VIP、支付、用户、配置、权限、统计

---

## 一、背景与目标

### 1.1 现状问题

当前 VIP 体系存在四个核心问题：

1. VIP 按功能拆分（interviewVip、resumeOptimizeVip），用户需购买多个会员，体验混乱
2. 没有统一会员模型，每加一个收费功能就要加一套 VIP 逻辑，代码膨胀
3. 支付与会员脱节，收支无法按业务归集，财务难以统计
4. "端"概念隐式存在，各模块各一套表达，无法统一查询、统计、配置

### 1.2 设计目标

1. 一端一套：门户端、记账端、人格分析端各一套 VIP 体系
2. 端为公共概念：抽为全局维度，支付/配置/权限/统计全打通
3. 开发零负担：加接口只加 `@VipOnly` 注解
4. 后台可视化：启动扫描入库，运营可见可配
5. 渐进启用：`sys_config.vip.enabled` 开关，前期关闭全员免费
6. 支付闭环：支付成功自动发卡，到期自动失效
7. 和权限同构：照搬 RBAC 模型，团队零学习成本

### 1.3 设计原则

1. 端是公共概念，`sys_platform` 全局唯一定义
2. 一端一套，一个端一个 platform，一套等级权益
3. 代码声明，注解声明，不写业务判断
4. 扫描入库，启动自动扫描，后台可见
5. 配置驱动，改配置不改代码
6. 渐进启用，开关控制上线节奏

---

## 二、核心概念

### 2.1 端的定义

| platform_code | 端名称   | 类型  | 说明                      |
| ------------- | ----- | --- | ----------------------- |
| portal        | 门户端   | C端  | moyun-portal，求职/学习/成长   |
| ledger        | 记账端   | C端  | moyun-ledger-app，个人资产管理 |
| admin         | 管理端   | B端  | moyun-admin-vue，后台管理    |
| personality   | 人格分析端 | C端  | 未来扩展                    |

### 2.2 端作为公共概念

"端"是全系统公共维度，被以下模块引用：

| 模块  | 用途          |
| --- | ----------- |
| 用户  | 用户从哪个端来     |
| 支付  | 订单/分账归属端    |
| VIP | 会员卡/权益归属端   |
| 配置  | 全局配置 + 端级配置 |
| 菜单  | 菜单归属端       |
| 统计  | 按端分组统计      |

### 2.3 与权限体系对照

| 维度   | 权限体系           | VIP 体系                   |
| ---- | -------------- | ------------------------ |
| 注解   | @PreAuthorize  | @VipOnly                 |
| 服务   | @ss.hasPermi() | IVipService.hasBenefit() |
| 表    | sys_menu       | vip_benefit              |
| 角色   | sys_role       | vip_tier                 |
| 关联   | sys_role_menu  | vip_tier_benefit         |
| 用户关联 | sys_user_role  | vip_user_card            |
| 开关   | 无              | sys_config.vip.enabled   |
| 消耗   | 无              | consume=true 时计数         |
| 端维度  | 无              | platform_code            |

---

## 三、数据模型

### 3.1 表清单

| 表名                | 用途          | 层次  |
| ----------------- | ----------- | --- |
| sys_platform      | 端定义（全局）     | 公共  |
| vip_tier          | 等级定义        | VIP |
| vip_benefit       | 权益定义        | VIP |
| vip_tier_benefit  | 等级权益关联      | VIP |
| vip_user_card     | 用户会员卡       | VIP |
| vip_benefit_usage | 权益使用记录      | VIP |
| vip_api_registry  | 接口注册表（扫描生成） | VIP |

### 3.2 全局端定义表

```sql
CREATE TABLE sys_platform (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    platform_code VARCHAR(50) NOT NULL COMMENT '端代码',
    platform_name VARCHAR(50) COMMENT '端名称',
    platform_type VARCHAR(20) COMMENT '端类型：c端/b端',
    description VARCHAR(200) COMMENT '描述',
    domain VARCHAR(100) COMMENT '绑定域名',
    icon VARCHAR(100) COMMENT '图标',
    sort_order INT DEFAULT 0 COMMENT '排序',
    status TINYINT DEFAULT 1 COMMENT '状态',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_platform_code (platform_code)
) COMMENT '端定义（全局公共）';

INSERT INTO sys_platform VALUES
(1, 'portal', '门户端', 'c端', '求职、学习、成长', 'www.xulin.com', '🌐', 1, 1, NOW()),
(2, 'ledger', '记账端', 'c端', '个人资产管理', 'ledger.xulin.com', '💰', 2, 1, NOW()),
(3, 'admin', '管理端', 'b端', '后台管理', 'admin.xulin.com', '⚙️', 3, 1, NOW()),
(4, 'personality', '人格分析端', 'c端', 'AI人格分析', 'me.xulin.com', '🔮', 4, 1, NOW());
```

### 3.3 VIP 核心表

```sql
-- 等级（类比 sys_role）
CREATE TABLE vip_tier (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    platform_code VARCHAR(50) NOT NULL COMMENT '端代码',
    tier_code VARCHAR(50) NOT NULL COMMENT '等级代码',
    tier_name VARCHAR(50) COMMENT '等级名称',
    duration_days INT COMMENT '有效天数（-1永久）',
    price DECIMAL(18,2) COMMENT '价格（元）',
    original_price DECIMAL(18,2) COMMENT '划线原价（元，可空）',
    popular TINYINT DEFAULT 0 COMMENT '是否推荐（1=推荐展示）',
    description VARCHAR(255) COMMENT '等级说明',
    sort_order INT DEFAULT 0,
    status TINYINT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_platform_tier (platform_code, tier_code)
) COMMENT 'VIP等级';

-- 权益（类比 sys_menu）
CREATE TABLE vip_benefit (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    platform_code VARCHAR(50) NOT NULL COMMENT '端代码',
    benefit_code VARCHAR(50) NOT NULL COMMENT '权益代码',
    benefit_name VARCHAR(100) COMMENT '权益名称',
    description VARCHAR(200) COMMENT '描述',
    sort_order INT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_platform_benefit (platform_code, benefit_code)
) COMMENT 'VIP权益';

-- 等级权益关联（类比 sys_role_menu）
CREATE TABLE vip_tier_benefit (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    platform_code VARCHAR(50) NOT NULL,
    tier_code VARCHAR(50) NOT NULL,
    benefit_code VARCHAR(50) NOT NULL,
    benefit_value VARCHAR(100) COMMENT 'unlimited / 数字',
    period VARCHAR(20) DEFAULT 'month' COMMENT 'day/month/year/unlimited',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_tier_benefit (platform_code, tier_code, benefit_code)
) COMMENT '等级权益关联';

-- 用户会员卡（类比 sys_user_role）
CREATE TABLE vip_user_card (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    platform_code VARCHAR(50) NOT NULL,
    tier_code VARCHAR(50) NOT NULL,
    order_id BIGINT COMMENT '关联支付订单',
    start_time DATETIME COMMENT '生效时间',
    expire_time DATETIME COMMENT '过期时间（永久为 NULL）',
    status TINYINT DEFAULT 1 COMMENT '1有效 0过期',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_platform (user_id, platform_code),
    INDEX idx_expire (expire_time)
) COMMENT '用户会员卡';

-- 权益使用记录
CREATE TABLE vip_benefit_usage (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    platform_code VARCHAR(50) NOT NULL,
    benefit_code VARCHAR(50) NOT NULL,
    usage_count INT DEFAULT 1,
    usage_date DATE COMMENT '使用日期',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_benefit_date (user_id, platform_code, benefit_code, usage_date),
    INDEX idx_user_date (user_id, usage_date)
) COMMENT '权益使用记录';

-- 接口注册表（扫描生成）
CREATE TABLE vip_api_registry (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    api_path VARCHAR(200) NOT NULL COMMENT '接口路径',
    http_method VARCHAR(10) COMMENT 'HTTP方法',
    controller_class VARCHAR(200) COMMENT 'Controller类',
    method_name VARCHAR(100) COMMENT '方法名',
    platform_code VARCHAR(50) COMMENT '端代码',
    benefit_code VARCHAR(50) COMMENT '权益代码',
    consume TINYINT DEFAULT 1 COMMENT '是否消耗次数',
    message VARCHAR(200) COMMENT '失败提示',
    api_desc VARCHAR(200) COMMENT '接口描述（运营填）',
    enabled TINYINT DEFAULT 1 COMMENT '是否启用校验',
    scan_time DATETIME COMMENT '扫描时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_api (api_path, http_method),
    INDEX idx_platform (platform_code),
    INDEX idx_benefit (benefit_code)
) COMMENT 'VIP接口注册表';
```

### 3.4 初始化数据

```sql
-- 门户端等级
INSERT INTO vip_tier (platform_code, tier_code, tier_name, duration_days, price, sort_order) VALUES
('portal', 'free', '免费', 0, 0, 1),
('portal', 'monthly', '月卡', 30, 49, 2),
('portal', 'yearly', '年卡', 365, 399, 3),
('portal', 'permanent', '永久', -1, 1299, 4);

-- 记账端等级
INSERT INTO vip_tier (platform_code, tier_code, tier_name, duration_days, price, sort_order) VALUES
('ledger', 'free', '免费', 0, 0, 1),
('ledger', 'yearly', '年卡', 365, 199, 2);

-- 门户端权益
INSERT INTO vip_benefit (platform_code, benefit_code, benefit_name, description) VALUES
('portal', 'interview_unlimited', '面试不限次', '不限次语音面试'),
('portal', 'resume_optimize', '简历优化', '每月简历优化次数'),
('portal', 'report_share', '报告分享', '面试报告分享'),
('portal', 'priority_queue', '优先队列', '面试优先调度'),
('portal', 'reading_unlimited', '读书不限', '读书空间无限阅读'),
('portal', 'article_paid', '付费文章', '免费阅读付费文章');

-- 记账端权益
INSERT INTO vip_benefit (platform_code, benefit_code, benefit_name, description) VALUES
('ledger', 'bill_parse', '账单识别', '每月账单截图识别次数'),
('ledger', 'ai_analysis', 'AI分析', 'AI财务分析');

-- 门户端等级权益
INSERT INTO vip_tier_benefit (platform_code, tier_code, benefit_code, benefit_value, period) VALUES
('portal', 'free', 'interview_unlimited', '2', 'unlimited'),
('portal', 'free', 'resume_optimize', '1', 'month'),
('portal', 'free', 'reading_unlimited', '3', 'month'),
('portal', 'monthly', 'interview_unlimited', 'unlimited', 'unlimited'),
('portal', 'monthly', 'resume_optimize', '5', 'month'),
('portal', 'monthly', 'report_share', 'unlimited', 'unlimited'),
('portal', 'monthly', 'reading_unlimited', 'unlimited', 'unlimited'),
('portal', 'monthly', 'article_paid', 'unlimited', 'unlimited'),
('portal', 'yearly', 'interview_unlimited', 'unlimited', 'unlimited'),
('portal', 'yearly', 'resume_optimize', '20', 'month'),
('portal', 'yearly', 'report_share', 'unlimited', 'unlimited'),
('portal', 'yearly', 'priority_queue', 'unlimited', 'unlimited'),
('portal', 'yearly', 'reading_unlimited', 'unlimited', 'unlimited'),
('portal', 'yearly', 'article_paid', 'unlimited', 'unlimited'),
('portal', 'permanent', 'interview_unlimited', 'unlimited', 'unlimited'),
('portal', 'permanent', 'resume_optimize', 'unlimited', 'unlimited'),
('portal', 'permanent', 'report_share', 'unlimited', 'unlimited'),
('portal', 'permanent', 'priority_queue', 'unlimited', 'unlimited'),
('portal', 'permanent', 'reading_unlimited', 'unlimited', 'unlimited'),
('portal', 'permanent', 'article_paid', 'unlimited', 'unlimited');

-- 记账端等级权益
INSERT INTO vip_tier_benefit (platform_code, tier_code, benefit_code, benefit_value, period) VALUES
('ledger', 'free', 'bill_parse', '5', 'month'),
('ledger', 'free', 'ai_analysis', '3', 'month'),
('ledger', 'yearly', 'bill_parse', '100', 'month'),
('ledger', 'yearly', 'ai_analysis', 'unlimited', 'unlimited');

-- sys_config 开关
INSERT INTO sys_config (config_key, config_value, config_name, remark, platform_code) VALUES
('vip.enabled', 'false', 'VIP体系开关', 'true启用 false关闭', NULL);
```

---

## 四、注解设计

### 4.1 注解定义

```java
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface VipOnly {

    /** 端代码 */
    String platform();

    /** 权益代码 */
    String benefit();

    /** 是否消耗次数 */
    boolean consume() default true;

    /** 校验失败提示 */
    String message() default "权益不足，请开通会员";
}
```

### 4.2 使用示例

```java
// 门户 - 面试
@VipOnly(platform = "portal", benefit = "interview_unlimited")
@PostMapping("/portal/interview/voice/start")
public AjaxResult start(...) { ... }

// 门户 - 简历优化
@VipOnly(platform = "portal", benefit = "resume_optimize")
@PostMapping("/portal/resume/optimize")
public AjaxResult optimize(...) { ... }

// 门户 - 读书
@VipOnly(platform = "portal", benefit = "reading_unlimited")
@GetMapping("/portal/reading/book/{id}")
public AjaxResult readBook(...) { ... }

// 记账 - 账单识别
@VipOnly(platform = "ledger", benefit = "bill_parse")
@PostMapping("/portal/ledger/bill/parse")
public AjaxResult parseBill(...) { ... }
```

### 4.3 和权限注解叠加

```java
@PreAuthorize("@ss.hasPermi('portal:interview:voice')")
@VipOnly(platform = "portal", benefit = "interview_unlimited")
@PostMapping("/start")
public AjaxResult start(...) { ... }
```

---

## 五、启动扫描

### 5.1 扫描器

```java
@Component
@Slf4j
public class VipApiScanner implements ApplicationRunner {

    @Autowired private VipApiRegistryMapper registryMapper;
    @Autowired private RequestMappingHandlerMapping handlerMapping;

    @Override
    public void run(ApplicationArguments args) {
        try {
            scan();
        } catch (Exception e) {
            log.error("[VipApiScanner] 扫描失败，不影响启动", e);
        }
    }

    public int scan() {
        Map<RequestMappingInfo, HandlerMethod> handlers = 
            handlerMapping.getHandlerMethods();
        int count = 0;

        for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : handlers.entrySet()) {
            HandlerMethod method = entry.getValue();

            VipOnly anno = AnnotatedElementUtils.findMergedAnnotation(
                method.getMethod(), VipOnly.class);
            if (anno == null) {
                anno = AnnotatedElementUtils.findMergedAnnotation(
                    method.getBeanType(), VipOnly.class);
            }
            if (anno == null) continue;

            VipApiRegistry registry = new VipApiRegistry();
            registry.setApiPath(extractPath(entry.getKey()));
            registry.setHttpMethod(extractHttpMethod(entry.getKey()));
            registry.setControllerClass(method.getBeanType().getName());
            registry.setMethodName(method.getMethod().getName());
            registry.setPlatformCode(anno.platform());
            registry.setBenefitCode(anno.benefit());
            registry.setConsume(anno.consume() ? 1 : 0);
            registry.setMessage(anno.message());
            registry.setScanTime(LocalDateTime.now());

            registryMapper.upsert(registry);
            count++;
        }

        log.info("[VipApiScanner] 扫描完成，共 {} 个 VIP 接口", count);
        return count;
    }

    private String extractPath(RequestMappingInfo info) {
        return info.getPatternValues().stream().findFirst().orElse("");
    }

    private String extractHttpMethod(RequestMappingInfo info) {
        return info.getMethodsCondition().getMethods().stream()
            .findFirst().map(Enum::name).orElse("ALL");
    }
}
```

### 5.2 幂等 upsert

```xml
<insert id="upsert">
    INSERT INTO vip_api_registry 
        (api_path, http_method, controller_class, method_name,
         platform_code, benefit_code, consume, message, scan_time)
    VALUES 
        (#{apiPath}, #{httpMethod}, #{controllerClass}, #{methodName},
         #{platformCode}, #{benefitCode}, #{consume}, #{message}, #{scanTime})
    ON DUPLICATE KEY UPDATE
        platform_code = VALUES(platform_code),
        benefit_code = VALUES(benefit_code),
        consume = VALUES(consume),
        message = VALUES(message),
        scan_time = VALUES(scan_time)
</insert>
```

---

## 六、运行时切面

```java
@Aspect
@Component
@Slf4j
public class VipOnlyAspect {

    @Autowired private IVipService vipService;
    @Autowired private VipApiRegistryMapper registryMapper;

    @Around("@annotation(vipOnly)")
    public Object check(ProceedingJoinPoint pjp, VipOnly vipOnly) throws Throwable {
        // 1. 开关关闭 → 放行
        if (!vipService.isVipEnabled(vipOnly.platform())) {
            return pjp.proceed();
        }

        // 2. 查注册表，看是否被后台禁用
        String apiPath = getApiPath(pjp);
        String httpMethod = getHttpMethod(pjp);
        VipApiRegistry registry = registryMapper.selectByPathAndMethod(apiPath, httpMethod);
        if (registry != null && registry.getEnabled() == 0) {
            return pjp.proceed();
        }

        // 3. 取用户
        Long userId = PortalSecurityUtils.getUserId();
        if (userId == null) {
            throw new ServiceException(401, "请先登录");
        }

        // 4. 校验权益
        boolean pass;
        if (vipOnly.consume()) {
            pass = vipService.consumeBenefit(userId, 
                vipOnly.platform(), vipOnly.benefit());
        } else {
            pass = vipService.hasBenefit(userId,
                vipOnly.platform(), vipOnly.benefit());
        }

        if (!pass) {
            throw new ServiceException(402, vipOnly.message());
        }

        return pjp.proceed();
    }
}
```

---

## 七、VipService

```java
public interface IVipService {
    boolean isVipEnabled(String platformCode);
    boolean isVip(Long userId, String platformCode);
    boolean hasBenefit(Long userId, String platformCode, String benefitCode);
    boolean consumeBenefit(Long userId, String platformCode, String benefitCode);
    List<UserVipCardVO> listUserCards(Long userId);
    UserVipDetailVO getVipDetail(Long userId, String platformCode);
    List<VipTierVO> listTiers(String platformCode);
}
```

核心实现逻辑：

1. isVipEnabled：优先查端级配置，回退全局
2. hasBenefit：查用户会员卡 → 查等级权益 → 判断次数
3. consumeBenefit：hasBenefit 通过后，Redis 原子计数 + 异步落库

---

## 八、支付体系改造

### 8.1 表字段说明

> pay_order **已有** platform 字段（v11.79 起，值为 portal/ledger），直接复用，不重复加列。
> pay_order **已有** biz_type/biz_no（bizType=vip，bizNo=`platform:tier`），与网关既有幂等复用逻辑对齐验证。

```sql
ALTER TABLE pay_ledger_entry ADD COLUMN platform VARCHAR(50) COMMENT '归属端';
```

### 8.2 支付回调

```java
@Component
public class VipPayCallbackHandler implements PayCallbackHandler {

    @Override
    public String getBizType() {
        return "vip";
    }

    @Override
    public void onPaySuccess(PayOrder order) {
        String[] parts = order.getBizNo().split(":");
        String platformCode = parts[0];
        String tierCode = parts[1];

        VipUserCard card = new VipUserCard();
        card.setUserId(order.getUserId());
        card.setPlatformCode(platformCode);
        card.setTierCode(tierCode);
        card.setOrderId(order.getId());
        card.setStartTime(LocalDateTime.now());

        VipTier tier = tierMapper.selectByCode(platformCode, tierCode);
        if (tier.getDurationDays() == -1) {
            card.setExpireTime(null);
        } else {
            card.setExpireTime(LocalDateTime.now().plusDays(tier.getDurationDays()));
        }

        cardMapper.insert(card);
    }
}
```

### 8.3 收支统计 SQL

```sql
SELECT 
    platform,
    COUNT(*) AS order_count,
    SUM(amount) AS total_income,
    SUM(CASE WHEN status='refunded' THEN amount ELSE 0 END) AS refund
FROM pay_order
WHERE biz_type = 'vip'
GROUP BY platform;
```

---

## 九、配置体系改造

### 9.1 表字段新增

```sql
ALTER TABLE sys_config ADD COLUMN platform_code VARCHAR(50) DEFAULT NULL COMMENT '归属端（NULL为全局）';
```

### 9.2 读取逻辑

优先查端级配置，查不到回退全局配置。

### 9.3 配置示例

```sql
-- 全局
INSERT INTO sys_config (config_key, config_value, platform_code) VALUES
('vip.enabled', 'false', NULL);

-- 门户端启用
INSERT INTO sys_config (config_key, config_value, platform_code) VALUES
('vip.enabled', 'true', 'portal');
```

---

## 十、用户体系改造

```sql
ALTER TABLE portal_user ADD COLUMN platform_code VARCHAR(50) DEFAULT 'portal' COMMENT '注册来源端';
```

---

## 十一、后台管理

### 11.1 菜单结构

系统设置
├── 端管理
└── VIP 管理
    ├── 等级管理
    ├── 权益管理
    ├── 等级权益配置
    ├── 接口注册管理
    ├── 用户会员卡
    └── 权益使用统计

### 11.2 接口注册管理页

展示所有 VIP 接口，支持筛选、编辑、启用/禁用、手动重新扫描。

### 11.3 收支统计页

按端分组展示订单数、收入、退款、净收入、占比。

---

## 十二、12 个欠考虑点及方案

| #   | 问题        | 方案                         |
| --- | --------- | -------------------------- |
| 1   | 注解加类还是方法  | 方法优先，类兜底                   |
| 2   | 路径匹配      | 取第一个路径，{id} 保留；多路径映射接口（@GetMapping({"/a","/b"})）需遍历全部 pattern 逐个注册 |
| 3   | 重启覆盖运营配置  | upsert 只更新代码字段             |
| 4   | 类上注解继承    | AnnotatedElementUtils 支持继承 |
| 5   | 热部署不重扫    | 后台提供手动重扫按钮                 |
| 6   | 扫描失败      | try-catch 不阻塞启动            |
| 7   | 注解与注册表不一致 | 重启更新代码字段                   |
| 8   | 消耗事务边界    | 切面外层，消耗独立事务                |
| 9   | 并发消耗      | Redis 原子计数                 |
| 10  | 开关缓存      | 30 分钟 TTL + 清缓存接口          |
| 11   | 旧三套 VIP 体系处置 | 开发阶段直接删除（见 §15.1），不做迁移兼容 |
| 12  | 权益码规范     | 统一 {action}，端级隔离           |

---

## 十三、实施路线

第一阶段（2 天）：模型 + 扫描
第二阶段（2 天）：运行时校验
第三阶段（2 天）：公共端改造
第四阶段（1 天）：后台管理
第五阶段（1 天）：支付关联
第六阶段（持续）：扩展

---

## 十四、关键决策记录

| 决策                | 目的         |
| ----------------- | ---------- |
| 端作为公共概念           | 统一收支、配置、统计 |
| sys_platform 全局唯一 | 各模块引用同一套定义 |
| @VipOnly 命名       | 短、醒目、风格一致  |
| 注解放接口层            | 和权限注解同层    |
| 启动扫描入库            | 后台可见，运营可配  |
| 幂等 upsert         | 重启不覆盖运营配置  |
| 切面查注册表            | 后台禁用能生效    |
| Redis 计数          | 并发安全       |
| 独立事务消耗            | 业务回滚不退次数   |
| 端级开关 + 全局开关       | 灰度上线       |
| 一端一套              | 收支清晰，扩展简单  |

---

## 十五、总结

### 15.1 旧三套 VIP 体系清理（开发阶段直接删除，不做迁移）

**用户裁决（2026-09-17）**：当前处于开发设计阶段，旧表旧数据无保留价值，直接删除或清表；仅 sys_menu 等配置级表删除需检查引用。免费体验剩余次数不保留。

**删除清单（DROP TABLE）**：

| 表 | 说明 |
| --- | --- |
| portal_interview_vip_package / portal_interview_vip_order | 面试会员两表 |
| portal_resume_optimize_package / portal_resume_optimize_order | 简历优化会员两表 |
| ledger_vip_package / ledger_vip_order | 记账 VIP 两表 |
| portal_free_trial | 免费体验表（由 free tier 权益计数完全替代） |

**删除代码**：

| 类型 | 内容 |
| --- | --- |
| 回调 Handler | InterviewVipPayCallbackHandler / ResumeOptimizeVipPayCallbackHandler / LedgerVipPayCallbackHandler |
| 后端 | 三套 Controller（PortalInterviewVipController 等）+ 级联 Service/Mapper/Entity + PortalFreeTrialService |
| 门户前端 | /interview/vip 与 /resume/vip 两个订阅页 → 合并为门户端一个会员页（等级选择 + 权益清单） |
| admin 前端 | interviewVip / resumeOptimizeVip / ledger vipPackage 三个套餐管理页 → 一个 VIP 管理模块 |
| 菜单 | 旧会员/套餐菜单 SQL 增量清理（配置级表，UPDATE/DELETE 方式） |

**替代关系**：

- 免费体验（原 portal_free_trial 每场景 2 次）→ free tier 的 vip_tier_benefit 计数（如 interview_unlimited='2'）单一轨道
- 三套 bizType（interview_vip / resume_optimize / ledger_vip）→ 统一 bizType=vip
- 三处 isVip 硬编码调用 → @VipOnly 注解 + IVipService

**闭环设计**：

开发加 @VipOnly → 启动扫描 → 后台可见 → 运营配置 → 运行时切面 → 校验权益 → 消耗次数 → 支付发卡 → 权益生效 → 到期失效 → 按端统计 → 收支清晰

**核心价值**：

1. 统一概念："端"全局唯一定义
2. 收支清晰：按端分组，财务直接可用
3. 开发零负担：只加注解
4. 运营可视化：全站 VIP 接口一目了然
5. 配置灵活：改配置不改代码
6. 渐进启用：开关控制
7. 扩展简单：加端只加数据
8. 和权限同构：团队学习成本最低

---

## 十六、评审结论（v2.1，2026-09-17）

对照当前代码（v11.99）评审，方向与目标架构满足，v2.1 修订以下脱节点后通过：

| # | 脱节点 | 修订 |
| --- | --- | --- |
| 1 | 存量三套 VIP 无迁移方案 | §15.1 开发阶段直接删除，不做迁移兼容 |
| 2 | pay_order 加 platform_code 重复 | 复用已有 platform 字段，仅 pay_ledger_entry 补列 |
| 3 | 免费体验（portal_free_trial）双轨冲突 | free tier 权益计数完全替代，portal_free_trial 删除 |
| 4 | vip_tier 缺运营字段 | 补 original_price / popular / description |
| 5 | 前台页面合并未设计 | 门户一个会员页 + admin 一个 VIP 管理 |
| 6 | sys_config 端级维度兼容 | platform_code NULL=全局，现有全局读取逻辑兼容 |
| 7 | 扫描器多路径映射漏注册 | §12-2 遍历全部 pattern |
| 8 | 金额精度 | DECIMAL(18,2) 对齐全项目金额铁律 |
| 9 | 402 语义 | 与现免费体验引导开通一致 |
| 10 | Redis 可用性 | 项目已有 RedisCache/RateLimiterAspect，无障碍 |

**实施前置确认**：已确认（旧体系直接删除、免费体验直接替换、不保留旧数据）。

---

文档维护：旭林知行开发团队
最后更新：2026-09-17（v2.1 评审修订）
