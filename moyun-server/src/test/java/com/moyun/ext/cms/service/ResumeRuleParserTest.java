package com.moyun.ext.cms.service;

import com.moyun.ext.cms.domain.vo.ResumeParseVO;
import com.moyun.ext.cms.domain.vo.UserResumeVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 简历规则解析引擎单测
 *
 * <p>锁定 v13.38 方案的两条铁设计原则与核心规则：</p>
 * <ul>
 *   <li><b>内容永不丢失</b>：抽不出字段时整段原文仍须保留在 {@code description}；</li>
 *   <li><b>宁缺勿错</b>：抽不准的字段留空（尤其性别不得从上下文推断）；</li>
 *   <li>R1 章节分桶 / R2 日期锚点 / R3 条目切分 / R4 字段分配 / R5 基本信息 / R6 技能词域。</li>
 * </ul>
 *
 * @author moyun
 */
class ResumeRuleParserTest {

    private final ResumeRuleParser parser = new ResumeRuleParser();

    /** 一份典型的「标签 + 章节 + 日期锚点」中文简历 */
    private static final String SAMPLE = """
            钟永国
            性别：男    出生日期：1990年5月
            手机：13812345678    邮箱：zhongyg@example.com
            求职意向：Java开发工程师    期望城市：深圳

            教育背景
            北京大学 计算机科学与技术
            2012.09-2016.06
            主修课程：数据结构、操作系统、计算机网络
            本科

            工作经历
            阿里巴巴 Java开发工程师
            2019.03-2023.06
            负责订单系统的重构，QPS 从 2000 提升到 8000
            主导分布式事务方案落地

            腾讯 高级后端工程师
            2016.07-2019.02
            负责支付网关高可用改造

            项目经历
            订单中台重构
            2021.05-2022.12
            技术栈：Spring Boot + MySQL + Redis + Kafka
            拆分单体为微服务，引入消息队列削峰

            专业技能
            熟练掌握 Java、Spring Boot、MySQL、Redis、Kafka、Docker

            自我评价
            8 年后端开发经验，擅长高并发系统设计与性能优化。

            荣誉奖项
            2015 年 ACM 校赛一等奖
            """;

    @Test
    @DisplayName("R1 章节分桶：八个大类均能识别")
    void splitsIntoSections() {
        Map<String, List<String>> buckets = parser.splitBySections(
                parser.normalizeText(SAMPLE), ResumeRuleParser.defaultSectionKeywords());

        assertTrue(buckets.containsKey("edu"), "教育背景未识别");
        assertTrue(buckets.containsKey("work"), "工作经历未识别");
        assertTrue(buckets.containsKey("project"), "项目经历未识别");
        assertTrue(buckets.containsKey("skill"), "专业技能未识别");
        assertTrue(buckets.containsKey("self"), "自我评价未识别");
        assertTrue(buckets.containsKey("other"), "荣誉奖项未识别");
        assertTrue(buckets.containsKey("intention"), "求职意向未识别");

        // 章节标题本身不应进入内容桶（否则会污染字段抽取）
        assertFalse(String.join("\n", buckets.get("edu")).contains("教育背景"));
        assertFalse(String.join("\n", buckets.get("work")).contains("工作经历"));
    }

    @Test
    @DisplayName("R2 日期区间：多种写法均可解析，'至今'归一为 isCurrent")
    void parsesDateRanges() {
        ResumeRuleParser.DateRange r1 = parser.parseDateRange("2012.09-2016.06");
        assertNotNull(r1);
        assertEquals("2012-09", r1.startDate());
        assertEquals("2016-06", r1.endDate());
        assertFalse(r1.isCurrent());

        ResumeRuleParser.DateRange r2 = parser.parseDateRange("2019年3月 - 至今");
        assertNotNull(r2);
        assertEquals("2019-03", r2.startDate());
        assertNull(r2.endDate(), "至今不应有结束时间");
        assertTrue(r2.isCurrent());

        ResumeRuleParser.DateRange r3 = parser.parseDateRange("2016/07—2019/02");
        assertNotNull(r3);
        assertEquals("2016-07", r3.startDate());
        assertEquals("2019-02", r3.endDate());

        assertNull(parser.parseDateRange("负责订单系统重构"), "无日期的行不应解析出区间");
    }

    @Test
    @DisplayName("R5 基本信息：手机/邮箱/姓名/性别/出生日期来自显式标签")
    void parsesBasicInfo() {
        ResumeParseVO vo = parser.parse(SAMPLE);

        assertEquals("13812345678", vo.getPhone());
        assertEquals("zhongyg@example.com", vo.getEmail());
        assertEquals("钟永国", vo.getName());
        assertEquals("男", vo.getGender());
        assertEquals("1990-05-01", vo.getBirthDate());
        assertFalse(vo.getAiPowered(), "规则解析结果必须标记为非 AI");
    }

    @Test
    @DisplayName("R3/R4 条目化：教育/工作/项目切出正确条数与起止时间")
    void parsesEntries() {
        ResumeParseVO vo = parser.parse(SAMPLE);

        List<UserResumeVO.EducationItem> edus = vo.getEducations();
        assertEquals(1, edus.size(), "教育经历条数");
        assertEquals("北京大学", edus.get(0).getSchool());
        assertEquals("计算机科学与技术", edus.get(0).getMajor());
        assertEquals("2012-09", edus.get(0).getStartDate());
        assertEquals("2016-06", edus.get(0).getEndDate());
        assertEquals("本科", edus.get(0).getDegree());

        List<UserResumeVO.WorkItem> works = vo.getWorks();
        assertEquals(2, works.size(), "工作经历应为 2 条");
        assertEquals("阿里巴巴", works.get(0).getCompany());
        assertEquals("Java开发工程师", works.get(0).getPosition());
        assertEquals("2019-03", works.get(0).getStartDate());
        assertEquals("2023-06", works.get(0).getEndDate());
        assertEquals("腾讯", works.get(1).getCompany());
        assertEquals("2016-07", works.get(1).getStartDate());

        List<UserResumeVO.ProjectItem> projects = vo.getProjects();
        assertEquals(1, projects.size(), "项目经历应为 1 条");
        assertEquals("订单中台重构", projects.get(0).getName());
        assertEquals("2021-05", projects.get(0).getStartDate());
        assertEquals("2022-12", projects.get(0).getEndDate());
    }

    @Test
    @DisplayName("铁原则·内容永不丢失：条目内容必须保留在 description")
    void neverLosesContent() {
        ResumeParseVO vo = parser.parse(SAMPLE);

        String work1Desc = vo.getWorks().get(0).getDescription();
        assertNotNull(work1Desc);
        assertTrue(work1Desc.contains("订单系统的重构"), "工作内容丢失：" + work1Desc);
        assertTrue(work1Desc.contains("分布式事务"), "工作内容丢失：" + work1Desc);

        assertTrue(vo.getProjects().get(0).getDescription().contains("微服务"), "项目内容丢失");
        assertEquals("8 年后端开发经验，擅长高并发系统设计与性能优化。", vo.getSelfIntro());
    }

    @Test
    @DisplayName("铁原则·内容永不丢失：无日期锚点时退化为整块一个条目，内容不丢")
    void noDateAnchorStillKeepsContent() {
        String text = """
                教育背景
                某某大学 软件工程 本科
                主修课程：编译原理、数据库原理
                """;
        ResumeParseVO vo = parser.parse(text);

        assertEquals(1, vo.getEducations().size(), "无日期时应退化为整块一个条目");
        UserResumeVO.EducationItem e = vo.getEducations().get(0);
        assertNull(e.getStartDate(), "无日期时起止时间应为空（宁缺勿错）");
        assertTrue(e.getDescription().contains("编译原理"), "内容丢失：" + e.getDescription());
    }

    @Test
    @DisplayName("铁原则·宁缺勿错：性别不得从上下文推断")
    void doesNotInferGender() {
        String text = """
                张三
                手机：13900001111
                工作经历
                某公司 后端工程师
                2019.03-2021.06
                负责男装电商平台的订单模块开发
                """;
        ResumeParseVO vo = parser.parse(text);
        assertNull(vo.getGender(), "无显式性别标签时必须留空，不得从'男装'推断");
    }

    @Test
    @DisplayName("出生日期误判保护：教育经历里的年份不得当作出生日期")
    void doesNotMistakeEducationYearAsBirth() {
        String text = """
                李四
                手机：13700002222
                教育背景
                清华大学 计算机
                2013.09-2017.06
                """;
        ResumeParseVO vo = parser.parse(text);
        assertNull(vo.getBirthDate(), "教育年份被误判为出生日期：" + vo.getBirthDate());
    }

    @Test
    @DisplayName("R6 技能：词域匹配且去重")
    void extractsSkills() {
        ResumeParseVO vo = parser.parse(SAMPLE);
        List<String> names = vo.getSkills().stream().map(UserResumeVO.SkillItem::getName).toList();

        assertTrue(names.contains("Java"), "技能未命中 Java：" + names);
        assertTrue(names.contains("MySQL"), "技能未命中 MySQL：" + names);
        assertTrue(names.contains("Redis"), "技能未命中 Redis：" + names);
        assertTrue(names.contains("Docker"), "技能未命中 Docker：" + names);
        assertEquals(names.size(), names.stream().distinct().count(), "技能出现重复");
    }

    @Test
    @DisplayName("R0 归一化：零宽字符/全角空格不得破坏章节识别")
    void normalizesNoise() {
        String noisy = "教育\u200B背景\n北京大学 计算机\n2012.09-2016.06\n";
        ResumeParseVO vo = parser.parse(noisy);
        assertEquals(1, vo.getEducations().size(), "含零宽字符时章节识别失败");
        assertEquals("北京大学", vo.getEducations().get(0).getSchool());
    }

    @Test
    @DisplayName("求职意向：从意向区块提取岗位与城市")
    void parsesJobIntention() {
        ResumeParseVO vo = parser.parse(SAMPLE);
        assertNotNull(vo.getJobIntention());
        assertEquals("深圳", vo.getJobIntention().getCity());
        assertNotNull(vo.getJobIntention().getPosition(), "岗位未命中");
    }

    @Test
    @DisplayName("配置优先：后台章节词典覆盖内置默认")
    void configOverridesDefaults() {
        String text = """
                我的履历
                某公司 工程师
                2019.01-2020.01
                做了很多事情
                """;
        Map<String, String> custom = Map.of("我的履历", "work");
        ResumeParseVO vo = parser.parse(text, custom, null, null, null);
        assertEquals(1, vo.getWorks().size(), "自定义章节词典未生效");
    }
}
