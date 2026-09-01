package com.moyun.ext.cms.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moyun.common.exception.system.ServiceException;
import com.moyun.ext.cms.config.AiProperties;
import com.moyun.ext.cms.domain.vo.ResumeParseVO;
import com.moyun.ext.cms.domain.vo.UserResumeVO;
import com.moyun.ext.file.domain.entity.SysFile;
import com.moyun.ext.file.service.ISysFileService;
import com.moyun.portal.domain.entity.PortalUserResume;
import com.moyun.portal.mapper.PortalUserResumeMapper;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

    /**
     * 附件大小上限 10MB（与前端限制一致）
     */
    private static final long MAX_SIZE = 10 * 1024 * 1024L;

    /**
     * v10.22：送入 LLM 的简历文本上限（字符）
     * 降至 6000：减少输入 token 加快 LLM 响应，避免 timeout（简历核心信息集中在前 6000 字）
     */
    private static final int MAX_TEXT_CHARS = 6000;

    @Autowired
    private AiProperties aiProperties;

    @Autowired
    private LlmClient llmClient;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ISysFileService sysFileService;

    @Autowired
    private PortalUserResumeMapper portalUserResumeMapper;

    /**
     * 解析简历附件（统一入口，兼容旧调用方）
     *
     * @param file 上传的附件（pdf/doc/docx/txt/md）
     * @return 结构化解析结果（aiPowered=false 时为规则粗解析）
     */
    public ResumeParseVO parse(MultipartFile file) {
        return parse(file, null);
    }

    /**
     * 解析简历附件（v10.22 新增：带 userId，登录用户上传时同步保存源文件并创建附件简历记录）
     *
     * @param file   上传的附件（pdf/doc/docx/txt/md）
     * @param userId 当前登录用户ID，非空时同步保存源文件并创建 source_type=attachment 的简历记录
     * @return 结构化解析结果（含 attachmentResumeId/sourceFileUrl/sourceFileName）
     */
    public ResumeParseVO parse(MultipartFile file, Long userId) {
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

        // v10.22：登录用户上传时保存源文件并创建附件简历记录
        if (userId != null) {
            try {
                // 1. 保存源文件
                SysFile sysFile = sysFileService.uploadFileForPortal(file, "resume_attachment", null);
                String sourceFileUrl = sysFile.getFileUrl();
                String originalFileName = file.getOriginalFilename();

                vo.setSourceFileUrl(sourceFileUrl);
                vo.setSourceFileName(originalFileName);

                // 2. 创建附件简历记录
                PortalUserResume resume = new PortalUserResume();
                resume.setUserId(userId);
                resume.setSourceType("attachment");
                resume.setSourceFileUrl(sourceFileUrl);
                resume.setSourceFileName(originalFileName);

                // 标题：LLM 解析出 name 用 name，否则用文件名去扩展名
                String title;
                if (vo.getName() != null && !vo.getName().isBlank()) {
                    title = vo.getName() + "的简历";
                } else {
                    title = removeExtension(originalFileName);
                }
                resume.setTitle(title);

                resume.setStatus("draft");
                resume.setVersionNo(1);
                resume.setCreateTime(LocalDateTime.now());
                resume.setUpdateTime(LocalDateTime.now());

                // 填充结构化字段（从解析结果）
                resume.setName(vo.getName());
                resume.setGender(vo.getGender());
                if (vo.getBirthDate() != null && !vo.getBirthDate().isBlank()) {
                    try {
                        resume.setBirthDate(LocalDate.parse(vo.getBirthDate(), DateTimeFormatter.ofPattern("yyyy-MM-dd")));
                    } catch (Exception ignore) {
                        // 日期格式异常则不填
                    }
                }
                resume.setPhone(vo.getPhone());
                resume.setEmail(vo.getEmail());
                resume.setSelfIntro(vo.getSelfIntro());

                // JSON 字段序列化
                resume.setJobIntention(toJson(vo.getJobIntention()));
                resume.setEducations(toJson(vo.getEducations()));
                resume.setWorks(toJson(vo.getWorks()));
                resume.setProjects(toJson(vo.getProjects()));
                resume.setSkills(toJson(vo.getSkills()));

                // 拼接全文纯文本
                resume.setFullText(buildFullTextFromParseVO(vo));

                portalUserResumeMapper.insert(resume);
                vo.setAttachmentResumeId(resume.getId());
            } catch (Exception e) {
                log.error("[ResumeParse] 保存附件简历记录失败：{}", e.getMessage(), e);
                // 不影响解析结果返回，仅记录错误
            }
        }

        return vo;
    }

    /**
     * 文件名去扩展名
     */
    private String removeExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) return "附件简历";
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(0, dot) : fileName;
    }

    /**
     * JSON 序列化（ResumeParseService 内部用）
     */
    private String toJson(Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.warn("JSON 序列化失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * v10.22：将 ResumeParseVO 拼接为纯文本（供附件简历 full_text 字段使用）
     */
    private String buildFullTextFromParseVO(ResumeParseVO vo) {
        StringBuilder sb = new StringBuilder();
        if (vo.getName() != null) sb.append("姓名：").append(vo.getName()).append("\n");
        if (vo.getGender() != null) sb.append("性别：").append(vo.getGender()).append("\n");
        if (vo.getBirthDate() != null) sb.append("出生日期：").append(vo.getBirthDate()).append("\n");
        if (vo.getPhone() != null) sb.append("电话：").append(vo.getPhone()).append("\n");
        if (vo.getEmail() != null) sb.append("邮箱：").append(vo.getEmail()).append("\n");
        if (vo.getJobIntention() != null) {
            UserResumeVO.JobIntention ji = vo.getJobIntention();
            sb.append("\n【求职意向】\n");
            if (ji.getPosition() != null) sb.append("期望职位：").append(ji.getPosition()).append("\n");
            if (ji.getCity() != null) sb.append("期望城市：").append(ji.getCity()).append("\n");
            if (ji.getSalaryMin() != null && ji.getSalaryMax() != null)
                sb.append("期望薪资：").append(ji.getSalaryMin()).append("-").append(ji.getSalaryMax()).append("万/月\n");
            if (ji.getJobType() != null) sb.append("工作性质：").append(ji.getJobType()).append("\n");
        }
        if (vo.getEducations() != null && !vo.getEducations().isEmpty()) {
            sb.append("\n【教育经历】\n");
            for (int i = 0; i < vo.getEducations().size(); i++) {
                UserResumeVO.EducationItem e = vo.getEducations().get(i);
                sb.append(i + 1).append(". ");
                if (e.getSchool() != null) sb.append(e.getSchool());
                if (e.getMajor() != null) sb.append(" · ").append(e.getMajor());
                if (e.getDegree() != null) sb.append("（").append(e.getDegree()).append("）");
                sb.append("  ").append(e.getStartDate() != null ? e.getStartDate() : "")
                        .append(" - ").append(e.getEndDate() != null ? e.getEndDate() : "").append("\n");
                if (e.getDescription() != null) sb.append("   ").append(e.getDescription()).append("\n");
            }
        }
        if (vo.getWorks() != null && !vo.getWorks().isEmpty()) {
            sb.append("\n【工作经历】\n");
            for (int i = 0; i < vo.getWorks().size(); i++) {
                UserResumeVO.WorkItem w = vo.getWorks().get(i);
                sb.append(i + 1).append(". ");
                if (w.getCompany() != null) sb.append(w.getCompany());
                if (w.getPosition() != null) sb.append(" · ").append(w.getPosition());
                sb.append("  ").append(w.getStartDate() != null ? w.getStartDate() : "")
                        .append(" - ").append(w.getEndDate() != null ? w.getEndDate() : "").append("\n");
                if (w.getDescription() != null) sb.append("   ").append(w.getDescription()).append("\n");
            }
        }
        if (vo.getProjects() != null && !vo.getProjects().isEmpty()) {
            sb.append("\n【项目经历】\n");
            for (int i = 0; i < vo.getProjects().size(); i++) {
                UserResumeVO.ProjectItem p = vo.getProjects().get(i);
                sb.append(i + 1).append(". ");
                if (p.getName() != null) sb.append(p.getName());
                if (p.getRole() != null) sb.append("（").append(p.getRole()).append("）");
                sb.append("  ").append(p.getStartDate() != null ? p.getStartDate() : "")
                        .append(" - ").append(p.getEndDate() != null ? p.getEndDate() : "").append("\n");
                if (p.getDescription() != null) sb.append("   ").append(p.getDescription()).append("\n");
                if (p.getUrl() != null) sb.append("   链接：").append(p.getUrl()).append("\n");
            }
        }
        if (vo.getSkills() != null && !vo.getSkills().isEmpty()) {
            sb.append("\n【专业技能】\n");
            for (UserResumeVO.SkillItem s : vo.getSkills()) {
                sb.append("- ").append(s.getName());
                if (s.getLevel() != null) sb.append("（").append(s.getLevel()).append("）");
                sb.append("\n");
            }
        }
        if (vo.getSelfIntro() != null) {
            sb.append("\n【自我评价】\n").append(vo.getSelfIntro()).append("\n");
        }
        return sb.toString().trim();
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

    /**
     * TXT 编码探测：UTF-8 优先，失败回退 GBK
     */
    private String readTextWithCharsetDetect(byte[] bytes) {
        try {
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (Exception ignore) {
            return new String(bytes, Charset.forName("GBK"));
        }
    }

    // ==================== LLM 结构化解析 ====================

    private ResumeParseVO parseByLlm(String text) throws Exception {
        // v10.22：精简 prompt 降低 token、加快响应，避免 timeout
        String systemPrompt = "从简历原文抽取结构化JSON。字段：name,gender(男/女),birthDate(yyyy-MM-dd),"
                + "phone,email,title,jobIntention{position,city,salaryMin,salaryMax,jobType,availableTime},"
                + "educations[{school,major,degree,startDate(yyyy-MM),endDate(yyyy-MM),description}],"
                + "works[{company,position,startDate,endDate,description}],"
                + "projects[{name,role,startDate,endDate,description,url}],"
                + "skills[{name,level(精通/熟练/了解)}],selfIntro。"
                + "规则：只抽取原文存在的信息，缺失返回null或空数组，禁止编造。"
                + "只输出JSON本体，禁止markdown代码块。";
        String response = llmClient.chat(systemPrompt, text);
        ResumeParseVO vo = objectMapper.readValue(extractJson(response), ResumeParseVO.class);
        vo.setAiPowered(true);
        return vo;
    }

    /**
     * 剥离 markdown 围栏并截取 JSON 本体（与 ResumeAiAdviceService 同策略）
     */
    private String extractJson(String llmResponse) {
        if (llmResponse == null) {
            return "";
        }
        String s = llmResponse.trim();
        if (s.startsWith("```")) {
            int firstLineEnd = s.indexOf(' ');
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
    private static final Pattern BIRTH = Pattern.compile("(19\\d{2}|20[01]\\d)\s*[年./-]\s*(\\d{1,2})\s*[月./-]?\s*(\\d{1,2})?");

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
        for (String line : text.split(" ")) {
            String t = line.replaceAll("\s", "").replace("姓名：", "").replace("姓名:", "");
            if (t.length() >= 2 && t.length() <= 4 && t.matches("[\u4e00-\u9fa5]{2,4}")) {
                vo.setName(t);
                break;
            }
        }
        // 技能关键词粗匹配（常见技术栈小词表）
        List<UserResumeVO.SkillItem> skills = new ArrayList<>();
        String[] known = {"Java", "Python", "Go", "JavaScript", "TypeScript", "Vue", "React", "Spring Boot",
                "MySQL", "Redis", "Kafka", "RabbitMQ", "Docker", "Kubernetes", "Linux", "MyBatis", "Node.js"};
        List<String> known2 = new ArrayList(List.of(known));
        for (String k : known2) {
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

    /**
     * 日期归一化到 yyyy-MM-dd；无法解析返回 null（前端保留原值）
     */
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
