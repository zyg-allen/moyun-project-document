package com.moyun.ext.cms.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moyun.ext.cms.config.AiProperties;
import com.moyun.ext.cms.domain.vo.ResumeParseVO;
import com.moyun.ext.cms.domain.vo.UserResumeVO;
import com.moyun.common.exception.system.ServiceException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 简历附件解析服务（v10.12）
 * <p>
 * 流程：附件（PDF/Word/TXT）→ 抽取纯文本 → LLM 结构化抽取（字段语义对齐在线简历表单）；
 * LLM 未启用/调用失败时回退到正则规则粗解析（仅邮箱/电话/技能等高置信字段）。
 * <p>
 * LLM 抽取结果经 {@link #normalize(ResumeParseVO)} 归一化：
 * 日期统一 yyyy-MM-dd、性别"男/女"、脏值置 null 由前端保留原表单值。
 *
 * @author moyun
 */
@Service
public class ResumeParseService {

    private static final Logger log = LoggerFactory.getLogger(ResumeParseService.class);

    /** 附件大小上限 10MB（与前端限制一致） */
    private static final long MAX_SIZE = 10 * 1024 * 1024L;

    /** 送入 LLM 的简历文本上限（字符），避免超上下文 */
    private static final int MAX_TEXT_CHARS = 12000;

    @Autowired
    private AiProperties aiProperties;

    @Autowired
    private LlmClient llmClient;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 解析简历附件（统一入口）
     *
     * @param file 上传的附件（pdf/doc/docx/txt/md）
     * @return 结构化解析结果（aiPowered=false 时为规则粗解析）
     */
    public ResumeParseVO parse(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ServiceException("请选择要解析的简历附件");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new ServiceException("附件不能超过 10MB");
        }
        String text = extractText(file);
        if (text == null || text.trim().isEmpty()) {
            throw new ServiceException("未能从附件中抽取到文本内容（可能是扫描件图片型 PDF，请换文本版简历）");
        }
        if (text.length() > MAX_TEXT_CHARS) {
            text = text.substring(0, MAX_TEXT_CHARS);
        }

        ResumeParseVO vo;
        if (aiProperties.isEnabled() && aiProperties.isResumeAdviceEnabled() && llmClient.isEnabled()) {
            try {
                vo = parseByLlm(text);
            } catch (Exception e) {
                log.warn("[ResumeParse] LLM 解析失败，回退规则解析：{}", e.getMessage());
                vo = parseByRule(text);
            }
        } else {
            vo = parseByRule(text);
        }
        vo.setTextLength(text.length());
        normalize(vo);
        return vo;
    }

    // ==================== 文本抽取 ====================

    /**
     * 按文件类型抽取纯文本
     * 支持：pdf（PDFBox 3.x）/ docx（POI XWPF）/ doc（POI HWPF）/ txt·md（UTF-8 或 GBK 探测）
     */
    private String extractText(MultipartFile file) {
        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        try {
            if (filename.endsWith(".pdf")) {
                try (PDDocument doc = Loader.loadPDF(file.getBytes())) {
                    return new PDFTextStripper().getText(doc);
                }
            }
            if (filename.endsWith(".docx")) {
                try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(file.getBytes()));
                     XWPFWordExtractor extractor = new XWPFWordExtractor(doc)) {
                    return extractor.getText();
                }
            }
            if (filename.endsWith(".doc")) {
                try (HWPFDocument doc = new HWPFDocument(new ByteArrayInputStream(file.getBytes()));
                     WordExtractor extractor = new WordExtractor(doc)) {
                    return extractor.getText();
                }
            }
            if (filename.endsWith(".txt") || filename.endsWith(".md")) {
                return readTextWithCharsetDetect(file.getBytes());
            }
            throw new ServiceException("不支持的文件类型，仅支持 PDF / Word / TXT / Markdown");
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("[ResumeParse] 文本抽取失败：{}", filename, e);
            throw new ServiceException("附件解析失败：" + e.getMessage());
        }
    }

    /** TXT 编码探测：UTF-8 优先，失败回退 GBK */
    private String readTextWithCharsetDetect(byte[] bytes) {
        try {
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (Exception ignore) {
            return new String(bytes, Charset.forName("GBK"));
        }
    }

    // ==================== LLM 结构化解析 ====================

    private ResumeParseVO parseByLlm(String text) throws Exception {
        String systemPrompt = "你是一名简历解析引擎，从用户提供的简历原文中抽取结构化信息并返回 JSON。"
                + "字段：name(姓名), gender(男/女), birthDate(yyyy-MM-dd), phone, email, title(简历标题), "
                + "jobIntention{position,city,salaryMin,salaryMax,jobType,availableTime}, "
                + "educations[{school,major,degree,startDate(yyyy-MM),endDate(yyyy-MM),description}], "
                + "works[{company,position,startDate, endDate,description}], "
                + "projects[{name,role,startDate,endDate,description,url}], "
                + "skills[{name,level(精通/熟练/了解)}], selfIntro(自我评价原文)。"
                + "规则：只抽取原文明确存在的信息，缺失字段返回 null 或空数组，禁止编造；"
                + "日期统一格式化（如 1995.7 → 1995-07-01）；薪资单位统一为千元/月（k）；"
                + "只输出 JSON 本体，禁止使用 markdown 代码块（```）包裹，禁止在 JSON 前后添加任何说明文字。";
        String response = llmClient.chat(systemPrompt, "简历原文如下：\n" + text);
        ResumeParseVO vo = objectMapper.readValue(extractJson(response), ResumeParseVO.class);
        vo.setAiPowered(true);
        return vo;
    }

    /** 剥离 markdown 围栏并截取 JSON 本体（与 ResumeAiAdviceService 同策略） */
    private String extractJson(String llmResponse) {
        if (llmResponse == null) {
            return "";
        }
        String s = llmResponse.trim();
        if (s.startsWith("```")) {
            int firstLineEnd = s.indexOf('\n');
            if (firstLineEnd > 0) {
                s = s.substring(firstLineEnd + 1).trim();
            }
            int fenceEnd = s.lastIndexOf("```");
            if (fenceEnd >= 0) {
                s = s.substring(0, fenceEnd).trim();
            }
        }
        int start = s.indexOf('{');
        int end = s.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return s.substring(start, end + 1);
        }
        return s;
    }

    // ==================== 规则粗解析（LLM 未启用/失败兜底） ====================

    private static final Pattern EMAIL = Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");
    private static final Pattern PHONE = Pattern.compile("1[3-9]\\d{9}");
    private static final Pattern BIRTH = Pattern.compile("(19\\d{2}|20[01]\\d)\\s*[年./-]\\s*(\\d{1,2})\\s*[月./-]?\\s*(\\d{1,2})?");

    private ResumeParseVO parseByRule(String text) {
        ResumeParseVO vo = new ResumeParseVO();
        vo.setAiPowered(false);

        Matcher m = EMAIL.matcher(text);
        if (m.find()) {
            vo.setEmail(m.group());
        }
        m = PHONE.matcher(text);
        if (m.find()) {
            vo.setPhone(m.group());
        }
        m = BIRTH.matcher(text);
        if (m.find()) {
            int year = Integer.parseInt(m.group(1));
            int month = Integer.parseInt(m.group(2));
            int day = m.group(3) != null ? Integer.parseInt(m.group(3)) : 1;
            if (month >= 1 && month <= 12 && day >= 1 && day <= 31 && year >= 1940) {
                vo.setBirthDate(String.format("%04d-%02d-%02d", year, month, day));
            }
        }
        // 姓名粗抽取：前 5 行内 2-4 个连续汉字且独占一行
        for (String line : text.split("\n")) {
            String t = line.replaceAll("\\s", "").replace("姓名：", "").replace("姓名:", "");
            if (t.length() >= 2 && t.length() <= 4 && t.matches("[\\u4e00-\\u9fa5]{2,4}")) {
                vo.setName(t);
                break;
            }
        }
        // 技能关键词粗匹配（常见技术栈小词表）
        List<UserResumeVO.SkillItem> skills = new ArrayList<>();
        String[] known = {"Java", "Python", "Go", "JavaScript", "TypeScript", "Vue", "React", "Spring Boot",
                "MySQL", "Redis", "Kafka", "RabbitMQ", "Docker", "Kubernetes", "Linux", "MyBatis", "Node.js"};
        for (String k : known) {
            if (text.toLowerCase().contains(k.toLowerCase())) {
                UserResumeVO.SkillItem s = new UserResumeVO.SkillItem();
                s.setName(k);
                s.setLevel("了解");
                skills.add(s);
            }
        }
        vo.setSkills(skills);
        return vo;
    }

    // ==================== 归一化 ====================

    /**
     * LLM 输出归一化：
     * <ul>
     *     <li>birthDate → yyyy-MM-dd（缺日补 01；非法置 null）</li>
     *     <li>gender → 男/女（"男性"→"男"；非法置 null）</li>
     *     <li>薪资非法值（0/负数）置 null</li>
     *     <li>空字符串统一转 null（前端 null 判断保留原表单值）</li>
     * </ul>
     */
    private void normalize(ResumeParseVO vo) {
        if (vo == null) {
            return;
        }
        vo.setBirthDate(normalizeDate(vo.getBirthDate()));
        if (vo.getGender() != null) {
            String g = vo.getGender().replace("性", "").trim();
            vo.setGender(g.equals("男") || g.equals("女") ? g : null);
        }
        vo.setName(blankToNull(vo.getName()));
        vo.setPhone(blankToNull(vo.getPhone()));
        vo.setEmail(blankToNull(vo.getEmail()));
        vo.setTitle(blankToNull(vo.getTitle()));
        vo.setSelfIntro(blankToNull(vo.getSelfIntro()));
        UserResumeVO.JobIntention ji = vo.getJobIntention();
        if (ji != null) {
            ji.setPosition(blankToNull(ji.getPosition()));
            ji.setCity(blankToNull(ji.getCity()));
            ji.setJobType(blankToNull(ji.getJobType()));
            ji.setAvailableTime(blankToNull(ji.getAvailableTime()));
            if (ji.getSalaryMin() != null && ji.getSalaryMin() <= 0) {
                ji.setSalaryMin(null);
            }
            if (ji.getSalaryMax() != null && ji.getSalaryMax() <= 0) {
                ji.setSalaryMax(null);
            }
            if (ji.getPosition() == null && ji.getCity() == null && ji.getSalaryMin() == null
                    && ji.getSalaryMax() == null && ji.getJobType() == null && ji.getAvailableTime() == null) {
                vo.setJobIntention(null);
            }
        }
    }

    /** 日期归一化到 yyyy-MM-dd；无法解析返回 null（前端保留原值） */
    private String normalizeDate(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        Matcher m = BIRTH.matcher(raw.trim());
        if (!m.find()) {
            return null;
        }
        int year = Integer.parseInt(m.group(1));
        int month = Integer.parseInt(m.group(2));
        int day = m.group(3) != null ? Integer.parseInt(m.group(3)) : 1;
        if (month < 1 || month > 12 || day < 1 || day > 31 || year < 1940 || year > 2025) {
            return null;
        }
        return String.format("%04d-%02d-%02d", year, month, day);
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank() || "null".equalsIgnoreCase(s.trim())) ? null : s.trim();
    }
}
