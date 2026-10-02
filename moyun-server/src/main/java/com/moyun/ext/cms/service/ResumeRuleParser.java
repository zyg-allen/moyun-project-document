package com.moyun.ext.cms.service;

import com.moyun.ext.cms.domain.vo.ResumeParseVO;
import com.moyun.ext.cms.domain.vo.UserResumeVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 简历<b>规则解析引擎</b>（纯 Java，零 AI 依赖，无状态）
 *
 * <h3>设计目标</h3>
 * <p>把「格式不规范的简历」转成平台标准结构。用户上传的简历<b>格式无法控制</b>，
 * 因此不追求字段级 100% 准确，而是保证：</p>
 * <ol>
 *   <li><b>内容永不丢失</b> —— 抽不出字段时，整段原文仍保留在 {@code description} 中；</li>
 *   <li><b>宁缺勿错</b> —— 抽不准的字段留空，绝不猜测（错值比空值危害大：空值用户会补，错值用户会信）；</li>
 *   <li><b>结构正确优先</b> —— 先按章节分「大类」，再按日期锚点切「条目（1、2、3）」，
 *       每类条目形态统一为「名称 + 起止时间 + 内容」。</li>
 * </ol>
 *
 * <h3>解析管线</h3>
 * <pre>
 *  normalize（去零宽/全角归一/行切分）
 *    → 章节标题分桶（R1：词典驱动，见 portal_resume_parse_config.config_type=section）
 *    → 条目切分（R2 日期范围锚点 + R3 分段）
 *    → 字段分配（R4：日期行上方为名称，余下为内容）
 *    → 基本信息（R5：手机/邮箱/姓名/性别/出生日期/求职意向/自评）
 * </pre>
 *
 * <h3>与其他解析路径的关系</h3>
 * <p>本引擎是<b>底座</b>：毫秒级、离线可用、可单测、结果确定。AI（{@code parseByLlm}）在其之上
 * 作为<b>可选增强</b>，不再是必需依赖 —— AI 不可用时本引擎结果自身可用。</p>
 *
 * <p>词表来源：{@code portal_resume_parse_config}（后台「简历解析配置」可维护）；
 * 表为空/不可用时使用本类内置 {@code DEFAULT_*} 词典兜底，<b>配置问题绝不导致解析失败</b>。</p>
 *
 * @author moyun
 */
public class ResumeRuleParser {

    private static final Logger log = LoggerFactory.getLogger(ResumeRuleParser.class);

    // ==================== 正则 ====================

    private static final Pattern EMAIL = Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");
    private static final Pattern PHONE = Pattern.compile("1[3-9]\\d{9}");
    /** 出生日期：需带「出生/生日」上下文，避免误抓入学年月 */
    private static final Pattern BIRTH_LABELED = Pattern.compile(
            "(?:出生(?:日期|年月)?|生日)\\s*[:：]?\\s*(\\d{4})\\s*[年./-]\\s*(\\d{1,2})\\s*[月./-]?\\s*(\\d{1,2})?");
    private static final Pattern BIRTH_PLAIN = Pattern.compile(
            "(19\\d{2}|20[01]\\d)\\s*[年./-]\\s*(\\d{1,2})\\s*[月./-]?\\s*(\\d{1,2})?");
    /** 性别显式标签 */
    private static final Pattern GENDER_LABELED = Pattern.compile("性\\s*别\\s*[:：]?\\s*([男女])");
    /** 姓名显式标签 */
    private static final Pattern NAME_LABELED = Pattern.compile("姓\\s*名\\s*[:：]\\s*([\\u4e00-\\u9fa5]{2,6})");
    /** 纯中文姓名候选 */
    private static final Pattern NAME_CANDIDATE = Pattern.compile("^[\\u4e00-\\u9fa5]{2,4}$");

    /**
     * 日期区间（条目锚点）：
     * 2020.03-2023.06 / 2020年3月至今 / 2020/03—2023/06 / 2020-2023 / 2020.03 – 2023.06
     */
    private static final Pattern DATE_RANGE = Pattern.compile(
            "(\\d{4})\\s*[年./\\-]\\s*(\\d{1,2})?\\s*[月]?\\s*"
                    + "(?:[-—–~至到]|--)\\s*"
                    + "((\\d{4})\\s*[年./\\-]\\s*(\\d{1,2})?\\s*[月]?|至今|现在|今|present|Present|NOW|now)");

    /** 单日期（用于区间退化场景） */
    private static final Pattern SINGLE_DATE = Pattern.compile("(\\d{4})\\s*[年./\\-]\\s*(\\d{1,2})?");

    // ==================== 内置默认词典（表为空时兜底） ====================

    /** 章节标题 → 大类（默认词典；后台配置优先） */
    private static final Map<String, String> DEFAULT_SECTION_KEYWORDS = new LinkedHashMap<>();

    static {
        put(DEFAULT_SECTION_KEYWORDS, "basic", "基本信息", "个人信息", "个人资料", "基本资料");
        put(DEFAULT_SECTION_KEYWORDS, "intention", "求职意向", "求职目标", "职业意向", "期望职位", "期望岗位", "目标岗位");
        put(DEFAULT_SECTION_KEYWORDS, "edu", "教育背景", "教育经历", "学习经历", "教育信息", "学历信息", "教育与培训");
        put(DEFAULT_SECTION_KEYWORDS, "work", "工作经历", "工作经验", "职业经历", "实习经历", "工作履历", "职业背景");
        put(DEFAULT_SECTION_KEYWORDS, "project", "项目经历", "项目经验", "项目实践", "项目业绩", "主要项目");
        put(DEFAULT_SECTION_KEYWORDS, "skill", "专业技能", "技能特长", "技能清单", "掌握技能", "技能专长", "IT技能", "计算机技能");
        put(DEFAULT_SECTION_KEYWORDS, "self", "自我评价", "个人评价", "自我介绍", "个人简介", "自我描述", "个人优势");
        put(DEFAULT_SECTION_KEYWORDS, "other", "荣誉奖项", "获奖情况", "证书", "资格证书", "校园经历", "校内职务",
                "培训经历", "语言能力", "兴趣爱好");
    }

    /** 技能词域默认（表为空时兜底；主词域由调用方用岗位必备技能补充） */
    private static final List<String> DEFAULT_SKILLS = List.of(
            "Java", "Python", "Go", "JavaScript", "TypeScript", "Vue", "React", "Angular",
            "Spring", "SpringBoot", "Spring Boot", "SpringCloud", "Spring Cloud", "MyBatis", "MyBatis-Plus",
            "MySQL", "PostgreSQL", "Oracle", "Redis", "MongoDB", "Elasticsearch",
            "Kafka", "RabbitMQ", "RocketMQ", "MQ",
            "Docker", "Kubernetes", "K8s", "Linux", "Shell", "Nginx", "Tomcat",
            "Maven", "Gradle", "Git", "SVN", "Jenkins", "CI/CD",
            "JVM", "JUC", "Netty", "Dubbo", "Nacos", "Zookeeper",
            "HTML", "CSS", "Sass", "Less", "Webpack", "Vite", "Node.js", "Node",
            "C++", "C#", "PHP", "Ruby", "Rust", "Scala", "Kotlin", "Swift",
            "Hadoop", "Spark", "Flink", "Hive", "TensorFlow", "PyTorch", "机器学习", "深度学习",
            "微服务", "分布式", "高并发", "高可用", "负载均衡", "缓存", "消息队列", "容器化",
            "系统设计", "性能优化", "单元测试", "敏捷开发", "项目管理", "需求分析", "数据分析",
            "产品设计", "Axure", "Figma", "Sketch", "Photoshop", "Visio", "Office", "Excel", "PPT", "SQL");

    /** 学历词默认（按长度优先匹配） */
    private static final List<String> DEFAULT_DEGREES = List.of(
            "博士研究生", "博士", "硕士研究生", "硕士", "研究生", "MBA",
            "大学本科", "本科", "学士", "大专", "专科", "高职", "中专", "高中");

    /** 常见岗位词默认（与 portal_job_template.name 互补） */
    private static final List<String> DEFAULT_POSITIONS = List.of(
            "Java开发", "Java工程师", "后端开发", "后端工程师", "前端开发", "前端工程师",
            "全栈工程师", "算法工程师", "测试开发", "测试工程师", "运维开发", "运维工程师",
            "产品经理", "项目经理", "UI设计", "交互设计", "数据分析师", "大数据开发",
            "架构师", "技术经理", "技术总监", "Android开发", "iOS开发");

    /** 章节标题行最大长度（超过则不视为标题，避免把正文误判为章节） */
    private static final int SECTION_TITLE_MAX_LEN = 20;

    /**
     * 章节关键词参与「前缀匹配」的最小长度。
     * <p>前缀匹配用于覆盖「教育背景 Education」这类中英混排标题；但短词危害大 ——
     * 例如 2 字词「技能」会把正文「技术栈：Spring Boot…」误判为 skill 章节，
     * 导致其后内容被整段切走（内容丢失）。故要求关键词至少 3 字。</p>
     */
    private static final int MIN_SECTION_KEYWORD_LEN = 3;

    /**
     * 职位/岗位关键词：用于把「公司 职位」标题拆开。
     * <p>这是拆分公司与职位的主判据 —— 比"公司名后缀"可靠得多，
     * 因为「阿里巴巴」「腾讯」这类知名公司名并不带「有限公司」后缀。</p>
     */
    private static final List<String> POSITION_KEYWORDS = List.of(
            "工程师", "开发", "研发", "架构师", "技术专家", "技术经理", "技术总监",
            "经理", "总监", "主管", "组长", "负责人", "专员", "助理", "顾问",
            "设计师", "产品", "运营", "测试", "运维", "算法", "数据分析", "实习",
            "Developer", "Engineer", "Manager", "Intern");

    /** 机构名后缀：用于把「学校 专业」标题拆开 */
    private static final List<String> ORG_SUFFIXES = List.of(
            "大学", "学院", "学校", "研究院", "研究所", "公司", "集团", "科技", "银行");

    // ==================== 对外入口 ====================

    /**
     * 规则解析主入口
     *
     * @param rawText          抽取出的简历纯文本
     * @param sectionKeywords  章节标题→大类 映射（来自后台配置；为空则用内置默认）
     * @param extraSkills      额外技能词域（如岗位必备技能；可为空）
     * @param degreeWords      学历词（来自后台配置；为空则用内置默认）
     * @param positionWords    岗位词（来自后台配置；为空则用内置默认）
     * @return 结构化结果（{@code aiPowered=false}）
     */
    public ResumeParseVO parse(String rawText,
                               Map<String, String> sectionKeywords,
                               List<String> extraSkills,
                               List<String> degreeWords,
                               List<String> positionWords) {
        ResumeParseVO vo = new ResumeParseVO();
        vo.setAiPowered(false);
        if (rawText == null || rawText.isBlank()) {
            return vo;
        }

        Map<String, String> sections = (sectionKeywords == null || sectionKeywords.isEmpty())
                ? DEFAULT_SECTION_KEYWORDS : sectionKeywords;
        List<String> skills = mergeSkills(extraSkills);
        List<String> degrees = (degreeWords == null || degreeWords.isEmpty()) ? DEFAULT_DEGREES : degreeWords;
        List<String> positions = (positionWords == null || positionWords.isEmpty()) ? DEFAULT_POSITIONS : positionWords;

        // ---------- R0 归一化 ----------
        String text = normalizeText(rawText);

        // ---------- R1 章节分桶 ----------
        Map<String, List<String>> buckets = splitBySections(text, sections);

        // ---------- R5 基本信息 ----------
        parseBasics(text, buckets, vo);

        // ---------- R2/R3/R4 条目类 ----------
        vo.setEducations(parseEntries(buckets.get("edu"), EntryKind.EDU, degrees));
        vo.setWorks(parseEntries(buckets.get("work"), EntryKind.WORK, degrees));
        vo.setProjects(parseEntries(buckets.get("project"), EntryKind.PROJECT, degrees));

        // ---------- R6 技能 ----------
        vo.setSkills(extractSkills(text, skills));

        // ---------- 自我评价 ----------
        List<String> selfLines = buckets.get("self");
        if (selfLines != null && !selfLines.isEmpty()) {
            vo.setSelfIntro(joinLines(selfLines).trim());
        }

        // ---------- 求职意向 ----------
        vo.setJobIntention(parseJobIntention(buckets.get("intention"), positions));

        return vo;
    }

    /** 便捷重载：仅用内置默认词典 */
    public ResumeParseVO parse(String rawText) {
        return parse(rawText, null, null, null, null);
    }

    // ==================== R0 归一化 ====================

    /**
     * 文本归一化：去零宽字符、统一换行、全角空格转半角、压缩连续空白行。
     * <p>这些字符是 PDF 抽取的常见噪声，会让章节标题匹配失效。</p>
     */
    public String normalizeText(String raw) {
        if (raw == null) {
            return "";
        }
        String t = raw
                .replace("\uFEFF", "")          // BOM
                .replace("\u200B", "")          // 零宽空格
                .replace("\u200C", "")
                .replace("\u200D", "")
                .replace("\u00A0", " ")         // 不换行空格
                .replace("\u3000", " ")         // 全角空格
                .replace("\r\n", "\n")
                .replace("\r", "\n");
        // 压缩 3 个以上连续换行 → 2 个（保留段落感，减少噪声行）
        t = t.replaceAll("\n{3,}", "\n\n");
        return t;
    }

    // ==================== R1 章节分桶 ====================

    /**
     * 按章节标题把文本切成大类桶。
     *
     * <p>规则：逐行扫描，若某行（去空白后）<b>等于</b>或以某章节关键词<b>开头</b>、
     * 且长度不超过 {@link #SECTION_TITLE_MAX_LEN}，则视为章节标题并切换当前桶。
     * 标题前的所有内容归入 {@code basic}（简历顶部通常是基础信息）。</p>
     *
     * @return key=大类，value=该大类的原始行列表（<b>原文保留</b>）
     */
    public Map<String, List<String>> splitBySections(String text, Map<String, String> sectionKeywords) {
        Map<String, List<String>> buckets = new LinkedHashMap<>();
        List<String> current = new ArrayList<>();
        buckets.put("basic", current);   // 顶部内容默认归 basic

        for (String rawLine : text.split("\n")) {
            String line = rawLine.trim();
            if (line.isEmpty()) {
                current.add("");
                continue;
            }
            SectionMatch sm = matchSection(line, sectionKeywords);
            if (sm != null) {
                // 命中章节标题：切换桶（标题本身不入内容，避免污染字段抽取）
                current = buckets.computeIfAbsent(sm.section(), k -> new ArrayList<>());
                // 兼容「求职意向：Java开发工程师」这类标题与内容同行写法：
                // 冒号后的内容归入该章节，避免信息丢失
                if (sm.inlineContent() != null && !sm.inlineContent().isBlank()) {
                    current.add(sm.inlineContent());
                }
                continue;
            }
            current.add(line);
        }
        return buckets;
    }

    /**
     * 章节匹配结果
     *
     * @param section       目标大类
     * @param inlineContent 标题与内容同行时冒号后的内容
     *                      （如「求职意向：Java开发工程师」→「Java开发工程师」）；无则 null
     */
    public record SectionMatch(String section, String inlineContent) {
    }

    /**
     * 判断一行是否为章节标题，并分离同行内容
     *
     * <p>真实简历常见两种写法，都要支持：</p>
     * <ul>
     *   <li>标题独占一行：<code>教育背景</code></li>
     *   <li>标题与内容同行：<code>求职意向：Java开发工程师</code>（前缀允许「一、」「【】」「◆」等装饰）</li>
     * </ul>
     *
     * @return 匹配结果；非标题返回 null
     */
    public SectionMatch matchSection(String line, Map<String, String> sectionKeywords) {
        if (line == null || line.isBlank()) {
            return null;
        }
        String compact = line.replaceAll("\\s", "");
        // 前缀装饰符：一、/ 1. / 【 / ◆ 等
        String cleaned = compact.replaceAll("^[\\[【（(\\-—=*#·•◆■□○●\\d一二三四五六七八九十、.．)】\\]]+", "");
        // 定位冒号（中英文），分离「标题」与「同行内容」
        String title = cleaned;
        String inline = null;
        int colon = indexOfColon(cleaned);
        if (colon > 0) {
            title = cleaned.substring(0, colon);
            inline = cleaned.substring(colon + 1).trim();
        }
        // 尾部装饰符清理
        title = title.replaceAll("[\\[【（(\\-—=*#·◆■□○●)】\\]]+$", "").trim();
        if (title.isEmpty() || title.length() > SECTION_TITLE_MAX_LEN) {
            return null;
        }
        // 1 精确匹配标题（如「教育背景」）
        String exact = sectionKeywords.get(title);
        if (exact != null) {
            return new SectionMatch(exact, inline);
        }
        // 2 标题以关键词开头（覆盖「教育背景 Education」中英混排）；
        //   限制余量长度，避免把「工作经历丰富，负责过多个项目」这类正文误判为标题
        // 2 标题以关键词开头（覆盖「教育背景 Education」中英混排）。
        //   严格要求「无同行内容」—— 否则会把正文误判为章节标题，
        //   例如「技术栈：Spring Boot + MySQL」会被当成 skill 章节，导致后续内容被切走（内容丢失）。
        if (inline != null && !inline.isBlank()) {
            return null;
        }
        for (Map.Entry<String, String> e : sectionKeywords.entrySet()) {
            String kw = e.getKey();
            if (kw.isEmpty() || !title.startsWith(kw)) {
                continue;
            }
            String rest = title.substring(kw.length()).trim();
            // 余量必须是纯 ASCII（拉丁字母/数字/符号）：本分支只为覆盖
            // 「教育背景 Education」「项目经历 Project」这类中英混排标题。
            // 若余量含中文，说明匹配到的短关键词只是长词的一部分
            // （如 3 字关键词「技术栈」把正文「技术栈：Spring…」当成标题），
            // 这会整段切走内容 → 直接判为非标题（内容永不丢失优先）。
            if (!rest.isEmpty() && rest.length() <= 12 && isAsciiOnly(rest)) {
                return new SectionMatch(e.getValue(), null);
            }
        }
        return null;
    }

    /** 取首个冒号（中文或英文）位置；无则 -1 */
    private int indexOfColon(String s) {
        int a = s.indexOf('：');
        int b = s.indexOf(':');
        if (a < 0) {
            return b;
        }
        if (b < 0) {
            return a;
        }
        return Math.min(a, b);
    }

    /** 是否仅含 ASCII 字符（用于判断章节标题的中英混排余量） */
    private boolean isAsciiOnly(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) > 127) {
                return false;
            }
        }
        return true;
    }

    // ==================== R5 基本信息 ====================

    private void parseBasics(String text, Map<String, List<String>> buckets, ResumeParseVO vo) {
        // 邮箱 / 手机：全文匹配（格式严格，误判率极低）
        Matcher m = EMAIL.matcher(text);
        if (m.find()) {
            vo.setEmail(m.group());
        }
        m = PHONE.matcher(text);
        if (m.find()) {
            vo.setPhone(m.group());
        }

        // 出生日期：优先带「出生/生日」标签；退化时取前 12 行内的裸日期（避免抓到工作经历里的年份）
        m = BIRTH_LABELED.matcher(text);
        if (m.find()) {
            vo.setBirthDate(formatDate(m.group(1), m.group(2), m.group(3)));
        } else {
            List<String> basicLines = buckets.get("basic");
            if (basicLines != null) {
                String head = joinLines(basicLines.subList(0, Math.min(basicLines.size(), 12)));
                m = BIRTH_PLAIN.matcher(head);
                if (m.find() && !isLikelyEducationYear(head, m.start())) {
                    vo.setBirthDate(formatDate(m.group(1), m.group(2), m.group(3)));
                }
            }
        }

        // 性别：只认显式标签，绝不推断
        m = GENDER_LABELED.matcher(text);
        if (m.find()) {
            vo.setGender("男".equals(m.group(1)) ? "男" : "女");
        } else {
            // 退化：「男 | 28岁 | 本科」这类混排行（前 6 行内，独立出现的男/女）
            List<String> basicLines = buckets.get("basic");
            if (basicLines != null) {
                int scanned = 0;
                for (String line : basicLines) {
                    if (line.isBlank()) {
                        continue;
                    }
                    if (++scanned > 6) {
                        break;
                    }
                    if (line.matches(".*(^|[\\s|/·,，])男([\\s|/·,，]|$).*")) {
                        vo.setGender("男");
                        break;
                    }
                    if (line.matches(".*(^|[\\s|/·,，])女([\\s|/·,·,，]|$).*")) {
                        vo.setGender("女");
                        break;
                    }
                }
            }
        }

        // 姓名：优先显式标签；否则在顶部若干行内找纯中文短行
        m = NAME_LABELED.matcher(text);
        if (m.find()) {
            vo.setName(m.group(1));
        } else {
            vo.setName(guessName(buckets.get("basic")));
        }
    }

    /** 出生日期误判保护：该位置附近出现学校/学历词则视为入学年份，不当作出生日期 */
    private boolean isLikelyEducationYear(String head, int pos) {
        int from = Math.max(0, pos - 12);
        int to = Math.min(head.length(), pos + 12);
        String around = head.substring(from, to);
        for (String w : List.of("大学", "学院", "学校", "入学", "毕业", "教育", "学历")) {
            if (around.contains(w)) {
                return true;
            }
        }
        return false;
    }

    /** 姓名猜测：顶部前若干行中首个 2-4 字纯中文且非章节词、非标签词的行 */
    private String guessName(List<String> basicLines) {
        if (basicLines == null) {
            return null;
        }
        int scanned = 0;
        for (String line : basicLines) {
            String t = line.trim();
            if (t.isEmpty()) {
                continue;
            }
            if (++scanned > 8) {
                break;
            }
            // 去掉「姓名：」前缀
            String candidate = t.replaceAll("^姓\\s*名\\s*[:：]?\\s*", "").trim();
            // 含数字/邮箱/电话特征的行直接跳过
            if (candidate.matches(".*\\d.*") || candidate.contains("@") || candidate.contains("：") || candidate.contains(":")) {
                continue;
            }
            if (NAME_CANDIDATE.matcher(candidate).matches() && !isSectionWord(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private boolean isSectionWord(String word) {
        for (String kw : DEFAULT_SECTION_KEYWORDS.keySet()) {
            if (word.equals(kw) || word.contains(kw)) {
                return true;
            }
        }
        return false;
    }

    // ==================== R2 日期范围 ====================

    /**
     * 日期区间解析结果
     *
     * @param startDate  起始（yyyy-MM 或 yyyy-MM-dd；只有年份时为 yyyy-01）
     * @param endDate    结束（至今时为 null）
     * @param isCurrent  是否"至今"
     */
    public record DateRange(String startDate, String endDate, boolean isCurrent) {
    }

    /**
     * R2：从一行文本里解析日期区间（条目锚点）
     *
     * @return 解析结果；无区间返回 null
     */
    public DateRange parseDateRange(String line) {
        if (line == null || line.isBlank()) {
            return null;
        }
        Matcher m = DATE_RANGE.matcher(line);
        if (!m.find()) {
            return null;
        }
        String start = formatMonth(m.group(1), m.group(2));
        String endRaw = m.group(3);
        boolean current = endRaw != null && endRaw.matches(".*(至今|现在|今|present|Present|NOW|now).*");
        String end = current ? null : formatMonth(m.group(4), m.group(5));
        if (start == null) {
            return null;
        }
        return new DateRange(start, end, current);
    }

    /** 该行是否含日期区间（用于条目切分） */
    private boolean hasDateRange(String line) {
        return line != null && DATE_RANGE.matcher(line).find();
    }

    private String formatMonth(String year, String month) {
        if (year == null || year.isBlank()) {
            return null;
        }
        int mo = 1;
        if (month != null && !month.isBlank()) {
            try {
                mo = Integer.parseInt(month.trim());
            } catch (NumberFormatException ignore) {
                mo = 1;
            }
        }
        if (mo < 1 || mo > 12) {
            mo = 1;
        }
        return String.format("%s-%02d", year.trim(), mo);
    }

    private String formatDate(String year, String month, String day) {
        if (year == null || year.isBlank()) {
            return null;
        }
        try {
            int y = Integer.parseInt(year.trim());
            if (y < 1940 || y > 2100) {
                return null;
            }
            int mo = (month == null || month.isBlank()) ? 1 : Integer.parseInt(month.trim());
            int d = (day == null || day.isBlank()) ? 1 : Integer.parseInt(day.trim());
            if (mo < 1 || mo > 12 || d < 1 || d > 31) {
                return String.format("%04d-01-01", y);
            }
            return String.format("%04d-%02d-%02d", y, mo, d);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ==================== R3/R4 条目切分与字段分配 ====================

    private enum EntryKind {EDU, WORK, PROJECT}

    /**
     * R3+R4：把某个大类的行列表切成条目，并分配「名称 / 起止时间 / 内容」
     *
     * <p>切分算法：日期区间行 = 条目锚点（起点）；下一条日期行 = 上一条目终点。
     * 锚点<b>上方紧邻的非空行</b>视为条目名称（简历惯例：名称在时间上方）。
     * 条目的其余文本原样进 {@code description}（<b>内容永不丢失</b>）。</p>
     */
    private <T> List<T> parseEntries(List<String> lines, EntryKind kind, List<String> degrees) {
        List<T> result = new ArrayList<>();
        if (lines == null || lines.isEmpty()) {
            return result;
        }
        // 1) 找出所有锚点（日期行）下标
        List<Integer> anchors = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            if (hasDateRange(lines.get(i))) {
                anchors.add(i);
            }
        }

        if (anchors.isEmpty()) {
            // 无日期锚点：退化为「整块一个条目」，内容全保留在 description（永不丢失）
            String block = joinLines(lines).trim();
            if (!block.isEmpty()) {
                result.add(buildEntry(kind, null, null, null, block, degrees));
            }
            return result;
        }

        for (int a = 0; a < anchors.size(); a++) {
            int anchorIdx = anchors.get(a);
            int nextAnchor = (a + 1 < anchors.size()) ? anchors.get(a + 1) : lines.size();

            // 名称：锚点上方紧邻的非空行（且不属于上一个条目的内容区）
            String title = null;
            int titleIdx = -1;
            int prevBoundary = (a == 0) ? 0 : anchors.get(a - 1) + 1;
            for (int i = anchorIdx - 1; i >= prevBoundary; i--) {
                String l = lines.get(i);
                if (l != null && !l.isBlank()) {
                    title = l.trim();
                    titleIdx = i;
                    break;
                }
            }

            // 内容：锚点行 + 锚点之后到下一锚点之前的行（排除标题行本身）
            List<String> body = new ArrayList<>();
            String anchorLine = lines.get(anchorIdx).trim();
            body.add(anchorLine);
            for (int i = anchorIdx + 1; i < nextAnchor; i++) {
                body.add(lines.get(i));
            }
            String description = joinLines(body).trim();
            // 标题若被并入内容，剔除（避免名称在内容里重复）
            if (title != null && titleIdx >= 0 && description.startsWith(title) && titleIdx != anchorIdx) {
                description = description.substring(title.length()).trim();
            }

            DateRange range = parseDateRange(anchorLine);
            result.add(buildEntry(kind,
                    title,
                    range == null ? null : range.startDate(),
                    range == null ? null : (range.isCurrent() ? "至今" : range.endDate()),
                    description,
                    degrees));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private <T> T buildEntry(EntryKind kind, String title, String start, String end,
                             String description, List<String> degrees) {
        String name = title == null ? "" : title.trim();
        switch (kind) {
            case EDU -> {
                UserResumeVO.EducationItem e = new UserResumeVO.EducationItem();
                // 学校 / 专业：标题里若能按分隔符切两段，则第二段作专业
                String[] parts = splitTitle(name);
                e.setSchool(parts[0]);
                e.setMajor(parts.length > 1 ? parts[1] : null);
                e.setDegree(matchWord(name + " " + nvl(description), degrees));
                e.setStartDate(start);
                e.setEndDate(end);
                e.setDescription(description);
                return (T) e;
            }
            case WORK -> {
                UserResumeVO.WorkItem w = new UserResumeVO.WorkItem();
                String[] parts = splitTitle(name);
                w.setCompany(parts[0]);
                w.setPosition(parts.length > 1 ? parts[1] : null);
                w.setStartDate(start);
                w.setEndDate(end);
                w.setDescription(description);
                return (T) w;
            }
            case PROJECT -> {
                UserResumeVO.ProjectItem p = new UserResumeVO.ProjectItem();
                String[] parts = splitTitle(name);
                p.setName(parts[0]);
                p.setRole(parts.length > 1 ? parts[1] : null);
                p.setStartDate(start);
                p.setEndDate(end);
                p.setDescription(description);
                return (T) p;
            }
            default -> throw new IllegalStateException("未知条目类型: " + kind);
        }
    }

    /**
     * 标题切分：把「公司 + 职位」「学校 + 专业」拆开
     * <p>仅在分隔符明确时切分，切不出则整体作为名称、第二段留空（宁缺勿错）。</p>
     */
    private String[] splitTitle(String title) {
        if (title == null || title.isBlank()) {
            return new String[]{null};
        }
        String t = title.trim();
        // 1 显式分隔符优先
        for (String sep : new String[]{" | ", "|", "  ", "\t", " · ", "·", " / ", "／"}) {
            int idx = t.indexOf(sep);
            if (idx > 0) {
                String left = t.substring(0, idx).trim();
                String right = t.substring(idx + sep.length()).trim();
                if (!left.isEmpty() && !right.isEmpty() && right.length() <= 20) {
                    return new String[]{left, right};
                }
            }
        }
        // 2 单空格分隔：在「机构/公司」与「专业/职位」之间断开。
        //   判据（宁缺勿错，切不出就整体作名称）：
        //     2.1 右侧以职位/岗位关键词开头（覆盖「阿里巴巴 Java开发工程师」这类无后缀公司名）
        //     2.2 或左侧以机构后缀结尾（「北京大学 计算机科学与技术」）
        int sp = t.lastIndexOf(' ');
        if (sp > 0 && sp < t.length() - 1) {
            String left = t.substring(0, sp).trim();
            String right = t.substring(sp + 1).trim();
            if (!left.isEmpty() && !right.isEmpty() && !right.contains(" ")
                    && (startsWithPositionKeyword(right) || endsWithOrgSuffix(left))) {
                return new String[]{left, right};
            }
        }
        return new String[]{t};
    }

    /** 职位/岗位关键词判据：用于把「公司 职位」拆开 */
    private boolean startsWithPositionKeyword(String s) {
        if (s == null || s.isBlank()) {
            return false;
        }
        for (String kw : POSITION_KEYWORDS) {
            if (s.startsWith(kw) || s.contains(kw)) {
                return true;
            }
        }
        return false;
    }

    /** 机构名后缀判据：用于把「学校 专业」拆开 */
    private boolean endsWithOrgSuffix(String s) {
        if (s == null || s.isBlank()) {
            return false;
        }
        for (String suffix : ORG_SUFFIXES) {
            if (s.endsWith(suffix)) {
                return true;
            }
        }
        return false;
    }

    /** 在文本中匹配第一个命中的词（按词长降序，避免「研究生」误配「研究生院」类问题） */
    private String matchWord(String text, List<String> words) {
        if (text == null || text.isBlank() || words == null) {
            return null;
        }
        String best = null;
        for (String w : words) {
            if (w != null && !w.isBlank() && text.contains(w)) {
                if (best == null || w.length() > best.length()) {
                    best = w;
                }
            }
        }
        return best;
    }

    // ==================== R6 技能 ====================

    /** 技能抽取：用合并后的词域做全文匹配（去重、保持词域顺序） */
    private List<UserResumeVO.SkillItem> extractSkills(String text, List<String> skills) {
        List<UserResumeVO.SkillItem> out = new ArrayList<>();
        if (text == null || text.isBlank() || skills == null) {
            return out;
        }
        String lower = text.toLowerCase();
        List<String> added = new ArrayList<>();
        for (String k : skills) {
            if (k == null || k.isBlank()) {
                continue;
            }
            String key = k.trim();
            if (added.contains(key)) {
                continue;
            }
            if (lower.contains(key.toLowerCase())) {
                UserResumeVO.SkillItem s = new UserResumeVO.SkillItem();
                s.setName(key);
                out.add(s);
                added.add(key);
            }
        }
        return out;
    }

    /** 合并词域：额外（岗位必备技能）优先，其后为默认通识技能；去重 */
    private List<String> mergeSkills(List<String> extra) {
        List<String> merged = new ArrayList<>();
        if (extra != null) {
            for (String s : extra) {
                if (s != null && !s.isBlank() && !merged.contains(s.trim())) {
                    merged.add(s.trim());
                }
            }
        }
        for (String s : DEFAULT_SKILLS) {
            if (!merged.contains(s)) {
                merged.add(s);
            }
        }
        return merged;
    }

    // ==================== 求职意向 ====================

    private UserResumeVO.JobIntention parseJobIntention(List<String> lines, List<String> positions) {
        if (lines == null || lines.isEmpty()) {
            return null;
        }
        String block = joinLines(lines);
        if (block.isBlank()) {
            return null;
        }
        UserResumeVO.JobIntention ji = new UserResumeVO.JobIntention();
        ji.setPosition(matchWord(block, positions));
        // 城市：常见「期望城市/意向城市：深圳」
        Matcher m = Pattern.compile("(?:期望|意向|目标)?(?:城市|地点|工作地)\\s*[:：]?\\s*([\\u4e00-\\u9fa5]{2,10})").matcher(block);
        if (m.find()) {
            ji.setCity(m.group(1));
        }
        if (ji.getPosition() == null && ji.getCity() == null) {
            return null;
        }
        return ji;
    }

    // ==================== 工具 ====================

    private static void put(Map<String, String> map, String target, String... keywords) {
        for (String k : keywords) {
            map.put(k, target);
        }
    }

    private String joinLines(List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (String l : lines) {
            sb.append(l == null ? "" : l).append('\n');
        }
        return sb.toString();
    }

    private String nvl(String s) {
        return s == null ? "" : s;
    }

    /** 暴露默认章节词典（供单测与文档核对） */
    public static Map<String, String> defaultSectionKeywords() {
        return new LinkedHashMap<>(DEFAULT_SECTION_KEYWORDS);
    }

    /** 暴露默认技能词域 */
    public static List<String> defaultSkills() {
        return new ArrayList<>(DEFAULT_SKILLS);
    }

    /** 暴露默认学历词 */
    public static List<String> defaultDegrees() {
        return new ArrayList<>(DEFAULT_DEGREES);
    }

    /** 暴露默认岗位词 */
    public static List<String> defaultPositions() {
        return new ArrayList<>(DEFAULT_POSITIONS);
    }
}
