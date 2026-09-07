package com.moyun.ext.cms.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moyun.common.config.RuoYiConfig;
import com.moyun.common.constant.Constants;
import com.moyun.common.exception.system.ServiceException;
import com.moyun.ext.cms.config.AiProperties;
import com.moyun.ext.cms.domain.vo.ResumeParseVO;
import com.moyun.ext.cms.domain.vo.UserResumeVO;
import com.moyun.portal.domain.entity.PortalUserResume;
import com.moyun.portal.mapper.PortalUserResumeMapper;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import org.apache.commons.io.IOUtils;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 简历附件解析服务（v10.23：支持 MinIO 优先 + 磁盘兜底）
 * <p>
 * 流程：附件（PDF/Word/TXT）→ 抽取纯文本 → LLM 结构化抽取（字段语义对齐在线简历表单）；
 * LLM 未启用/调用失败时回退到正则规则粗解析（仅邮箱/电话/技能等高置信字段）。
 * <p>
 * <strong>文件读取策略（v10.23 升级）</strong>：
 * <ol>
 *   <li>优先从 MinIO 读取（fileUrl 以 http:// 或 https:// 开头）</li>
 *   <li>MinIO 不可用/读取失败时，自动降级到磁盘读取（兜底）</li>
 * </ol>
 */
@Service
public class ResumeParseService {

    private static final Logger log = LoggerFactory.getLogger(ResumeParseService.class);

    /**
     * 附件大小上限 10MB（与前端限制一致）
     */
    private static final long MAX_SIZE = 10 * 1024 * 1024L;

    /**
     * 送入 LLM 的简历文本上限（字符）
     * 降至 6000：减少输入 token 加快 LLM 响应，避免 timeout
     */
    private static final int MAX_TEXT_CHARS = 6000;

    @Autowired
    private AiProperties aiProperties;

    @Autowired
    private LlmClient llmClient;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PortalUserResumeMapper portalUserResumeMapper;

    @Autowired(required = false)
    private MinioClient minioClient;

    @Value("${minio.bucketName:moyun}")
    private String bucketName;

    // ==================== 正则表达式（规则解析用） ====================

    private static final Pattern EMAIL = Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");
    private static final Pattern PHONE = Pattern.compile("1[3-9]\\d{9}");
    private static final Pattern BIRTH = Pattern.compile("(19\\d{2}|20[01]\\d)\\s*[年./-]\\s*(\\d{1,2})\\s*[月./-]?\\s*(\\d{1,2})?");

    // ==================== 公共方法 ====================

    /**
     * 快速创建附件简历记录（上传接口同步路径，不调 LLM）
     */
    public Long prepareAttachmentResume(Long userId, MultipartFile file, String fileUrl) {
        if (file == null || file.isEmpty()) {
            throw new ServiceException("请选择要解析的简历附件");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new ServiceException("附件不能超过 10MB");
        }
        String originalFileName = file.getOriginalFilename();
        if (!isSupportedType(originalFileName)) {
            throw new ServiceException("不支持的文件类型，仅支持 PDF / Word / TXT / Markdown");
        }

        PortalUserResume resume = new PortalUserResume();
        resume.setUserId(userId);
        resume.setSourceType("attachment");
        resume.setSourceFileUrl(fileUrl);
        resume.setSourceFileName(originalFileName);
        resume.setTitle(removeExtension(originalFileName));
        resume.setStatus("draft");
        resume.setVersionNo(1);
        resume.setCreateTime(LocalDateTime.now());
        resume.setUpdateTime(LocalDateTime.now());
        portalUserResumeMapper.insert(resume);
        return resume.getId();
    }

    /**
     * 异步执行附件简历解析（MinIO 优先 + 磁盘兜底）
     */
    public ResumeParseVO executeParse(Long userId, Long resumeId, String fileUrl, String fileName) {
        if (resumeId == null || resumeId <= 0) {
            throw new ServiceException("任务参数缺失：resumeId 必填");
        }
        if (fileUrl == null || fileUrl.isBlank()) {
            throw new ServiceException("任务参数缺失：fileUrl 必填");
        }

        // 1. 校验简历归属
        PortalUserResume resume = portalUserResumeMapper.selectById(resumeId);
        if (resume == null || !resume.getUserId().equals(userId)) {
            throw new ServiceException("简历不存在或无权访问");
        }

        // 2. 读取文件字节（优先 MinIO，磁盘兜底）
        byte[] bytes = readFileBytes(fileUrl);

        // 3. 抽取纯文本
        String text = extractText(fileName, bytes);
        if (text == null || text.trim().isEmpty()) {
            throw new ServiceException("未能从附件中抽取到文本内容（可能是扫描件图片型 PDF，请换文本版简历）");
        }
        if (text.length() > MAX_TEXT_CHARS) {
            text = text.substring(0, MAX_TEXT_CHARS);
        }

        // 4. LLM 结构化解析（失败回退规则解析）
        ResumeParseVO vo;
        boolean llmParsed;
        if (aiProperties.isEnabled() && aiProperties.isResumeAdviceEnabled() && llmClient.isEnabled()) {
            try {
                vo = parseByLlm(text);
                llmParsed = true;
            } catch (Exception e) {
                log.warn("[ResumeParse] LLM 解析失败，回退规则解析：{}", e.getMessage());
                vo = parseByRule(text);
                llmParsed = false;
            }
        } else {
            vo = parseByRule(text);
            llmParsed = false;
        }
        vo.setTextLength(text.length());
        normalize(vo);

        // 5. 回填附件简历记录
        PortalUserResume upd = new PortalUserResume();
        upd.setId(resumeId);
        upd.setName(vo.getName());
        upd.setGender(vo.getGender());
        if (vo.getBirthDate() != null && !vo.getBirthDate().isBlank()) {
            try {
                upd.setBirthDate(LocalDate.parse(vo.getBirthDate(), DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            } catch (Exception ignore) {
                // 日期格式异常则不填
            }
        }
        upd.setPhone(vo.getPhone());
        upd.setEmail(vo.getEmail());
        upd.setSelfIntro(vo.getSelfIntro());
        upd.setJobIntention(toJson(vo.getJobIntention()));
        upd.setEducations(toJson(vo.getEducations()));
        upd.setWorks(toJson(vo.getWorks()));
        upd.setProjects(toJson(vo.getProjects()));
        upd.setSkills(toJson(vo.getSkills()));
        if (vo.getName() != null && !vo.getName().isBlank()) {
            upd.setTitle(vo.getName() + "的简历");
        }
        upd.setFullText(buildFullTextFromParseVO(vo));
        // v11.x：解析置信度回填（LLM 结构化=85 / 规则兜底=60），供简历深挖出题与追问策略参考
        upd.setParseConfidence(llmParsed ? 85 : 60);
        upd.setUpdateTime(LocalDateTime.now());
        portalUserResumeMapper.updateById(upd);

        vo.setAttachmentResumeId(resumeId);
        vo.setSourceFileUrl(resume.getSourceFileUrl());
        vo.setSourceFileName(resume.getSourceFileName());
        return vo;
    }

    // ==================== 文件读取（MinIO 优先 + 磁盘兜底） ====================

    /**
     * 读取文件字节（优先 MinIO，磁盘兜底）
     */
    private byte[] readFileBytes(String fileUrl) {
        // 1. 如果是 MinIO URL，优先从 MinIO 读取
        if (isMinioUrl(fileUrl)) {
            try {
                log.debug("尝试从 MinIO 读取文件: {}", fileUrl);
                return readFromMinio(fileUrl);
            } catch (Exception e) {
                log.warn("MinIO 读取失败，降级到磁盘读取: {}", e.getMessage());
                // 继续走磁盘兜底
            }
        }

        // 2. 磁盘兜底
        return readFromDisk(fileUrl);
    }

    /**
     * 判断是否为 MinIO URL
     */
    private boolean isMinioUrl(String fileUrl) {
        return fileUrl != null && (fileUrl.startsWith("http://") || fileUrl.startsWith("https://"));
    }

    /**
     * 从 MinIO 读取文件
     */
    private byte[] readFromMinio(String fileUrl) {
        if (minioClient == null) {
            throw new ServiceException("MinIO 客户端未初始化");
        }

        try {
            // 从 URL 中提取 objectName
            // http://127.0.0.1:9001/moyun/2026/09/06/xxx.pdf
            // → objectName: 2026/09/06/xxx.pdf
            URL url = new URL(fileUrl);
            String path = url.getPath();
            if (path.startsWith("/")) {
                path = path.substring(1);
            }

            // 去掉 bucket 前缀
            if (path.startsWith(bucketName + "/")) {
                path = path.substring(bucketName.length() + 1);
            }

            log.debug("MinIO objectName: {}", path);

            try (InputStream stream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(path)
                            .build())) {
                byte[] bytes = IOUtils.toByteArray(stream);
                log.info("MinIO 读取成功: {}, size={} bytes", fileUrl, bytes.length);
                return bytes;
            }
        } catch (Exception e) {
            log.error("MinIO 读取失败: {}", e.getMessage());
            throw new ServiceException("从 MinIO 读取文件失败: " + e.getMessage());
        }
    }

    /**
     * 从磁盘读取文件（兜底）
     */
    private byte[] readFromDisk(String fileUrl) {
        try {
            String diskPath = fileUrl.startsWith(Constants.RESOURCE_PREFIX)
                    ? RuoYiConfig.getProfile() + fileUrl.substring(Constants.RESOURCE_PREFIX.length())
                    : fileUrl;

            File source = new File(diskPath);
            if (!source.exists() || !source.isFile()) {
                throw new ServiceException("附件源文件不存在或已被删除: " + diskPath);
            }
            if (source.length() > MAX_SIZE) {
                throw new ServiceException("附件不能超过 10MB");
            }

            byte[] bytes = Files.readAllBytes(source.toPath());
            log.info("磁盘读取成功: {}, size={} bytes", diskPath, bytes.length);
            return bytes;

        } catch (Exception e) {
            log.error("磁盘读取失败: {}", e.getMessage());
            throw new ServiceException("读取附件源文件失败: " + e.getMessage());
        }
    }

    // ==================== 文件类型判断与文本抽取 ====================

    private boolean isSupportedType(String fileName) {
        String n = fileName == null ? "" : fileName.toLowerCase();
        return n.endsWith(".pdf") || n.endsWith(".docx") || n.endsWith(".doc")
                || n.endsWith(".txt") || n.endsWith(".md");
    }

    private String removeExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) return "附件简历";
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(0, dot) : fileName;
    }

    private String extractText(String fileName, byte[] bytes) {
        String filename = fileName == null ? "" : fileName.toLowerCase();
        try {
            if (filename.endsWith(".pdf")) {
                try (PDDocument doc = Loader.loadPDF(bytes)) {
                    return new PDFTextStripper().getText(doc);
                }
            }
            if (filename.endsWith(".docx")) {
                try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(bytes));
                     XWPFWordExtractor extractor = new XWPFWordExtractor(doc)) {
                    return extractor.getText();
                }
            }
            if (filename.endsWith(".doc")) {
                try (HWPFDocument doc = new HWPFDocument(new ByteArrayInputStream(bytes));
                     WordExtractor extractor = new WordExtractor(doc)) {
                    return extractor.getText();
                }
            }
            if (filename.endsWith(".txt") || filename.endsWith(".md")) {
                return readTextWithCharsetDetect(bytes);
            }
            throw new ServiceException("不支持的文件类型，仅支持 PDF / Word / TXT / Markdown");
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("[ResumeParse] 文本抽取失败：{}", filename, e);
            throw new ServiceException("附件解析失败：" + e.getMessage());
        }
    }

    private String readTextWithCharsetDetect(byte[] bytes) {
        try {
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (Exception ignore) {
            return new String(bytes, Charset.forName("GBK"));
        }
    }

    // ==================== JSON 工具 ====================

    private String toJson(Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.warn("JSON 序列化失败: {}", e.getMessage());
            return null;
        }
    }

    // ==================== LLM 结构化解析 ====================

    private ResumeParseVO parseByLlm(String text) throws Exception {
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

    // ==================== 规则粗解析（LLM 失败兜底） ====================

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
        // 姓名粗抽取
        for (String line : text.split("\n")) {
            String t = line.replaceAll("\\s", "").replace("姓名：", "").replace("姓名:", "");
            if (t.length() >= 2 && t.length() <= 4 && t.matches("[\u4e00-\u9fa5]{2,4}")) {
                vo.setName(t);
                break;
            }
        }
        // 技能关键词粗匹配
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

    // ==================== FullText 拼接 ====================

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
}