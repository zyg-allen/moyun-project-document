package com.moyun.ext.cms.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.moyun.common.exception.system.ServiceException;
import com.moyun.ext.aigateway.support.AiSceneJsonClient;
import com.moyun.ext.ai.service.AiGlobalSwitch;
import com.moyun.ext.cms.domain.vo.ResumeParseVO;
import com.moyun.ext.cms.domain.vo.ResumePreviewVO;
import com.moyun.ext.cms.domain.vo.UserResumeVO;
import com.moyun.portal.domain.entity.PortalJobTemplate;
import com.moyun.portal.domain.entity.PortalUserResume;
import com.moyun.portal.mapper.PortalUserResumeMapper;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
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
import java.util.Map;
import java.util.UUID;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.moyun.ext.ai.enums.AiSceneEnum;

/**
 * 简历附件解析服务（<b>纯内存解析，不落盘、不进对象存储</b>）
 *
 * <h3>为什么不做文件持久化</h3>
 * <p>简历附件是<b>客户端一次性输入</b>：用户上传只为"让系统读出内容"，用完即弃。
 * 平台无法限制用户上传次数与体积，一旦落盘/进 MinIO，就会持续占用服务器空间且没有回收时机
 * （实测：同一份 400KB 简历被重复上传 4 次 → 存了 4 份副本 = 1.6MB 纯浪费）。
 * 因此本服务<b>只读取内容</b>：</p>
 * <ol>
 *   <li>上传请求内直接读 {@code MultipartFile} 字节并抽取纯文本（同步，纳秒级，仅占内存）；</li>
 *   <li>把<b>抽取后的文本</b>交给异步任务做 LLM 结构化解析；</li>
 *   <li>解析成功后<b>才创建</b>简历记录，只落<b>结构化字段</b>（姓名/技能/经历/教育/求职意向/自评）；</li>
 *   <li>原始文件（本地/对象存储）<b>一律不保留</b>——请求结束即随内存释放。</li>
 * </ol>
 * <p>流程：附件字节 → 抽取纯文本 → LLM 结构化抽取（字段语义对齐在线简历表单）；
 * LLM 未启用/调用失败时回退到正则规则粗解析（仅邮箱/电话/技能等高置信字段）。</p>
 *
 * <p><b>要规避的两个缺陷</b>：① 解析时按 {@code fileUrl} 回读文件——
 * local 模式下 fileUrl 是 {@code http://host/profile/...} 全 URL，既进不了 MinIO 分支
 * （连接失败），又因不以 {@code /profile} 开头被磁盘分支拒绝 → <b>必然解析失败</b>；
 * ② 上传接口<b>先建草稿再解析</b>，解析失败不回滚 → 每次失败都留一条空简历（脏数据）。
 * 现改为「先抽取 → 解析成功才建记录」，失败零残留。</p>
 */
@Service
public class ResumeParseService {
    /** 本服务所属 AI 场景代码（绑定见 ai_scene_config，业务不感知模型选择） */
    private static final String SCENE_RESUME_PARSE = AiSceneEnum.RESUME_PARSE.getCode();


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
    private AiGlobalSwitch aiGlobalSwitch;

        /** LLM 结构化解析统一走 AI 网关（注入防护/限流/成本熔断/日志全链路生效） */
    @Autowired
    private AiSceneJsonClient aiSceneJsonClient;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PortalUserResumeMapper portalUserResumeMapper;

    /** 规则解析词表来源（后台「简历解析配置」可维护；为空时引擎用内置默认词典兜底） */
    @Autowired
    private com.moyun.portal.service.IPortalResumeParseConfigService resumeParseConfigService;

    /** 岗位模板服务：技能词域自动聚合门户岗位必备技能（避免硬编码技能词典） */
    @Autowired
    private IPortalJobTemplateService jobTemplateService;

    /** 规则解析引擎（纯 Java，零 AI 依赖，毫秒级） */
    private final ResumeRuleParser ruleParser = new ResumeRuleParser();


    // ==================== 正则表达式（规则解析用） ====================

    private static final Pattern EMAIL = Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");
    private static final Pattern PHONE = Pattern.compile("1[3-9]\\d{9}");
    private static final Pattern BIRTH = Pattern.compile("(19\\d{2}|20[01]\\d)\\s*[年./-]\\s*(\\d{1,2})\\s*[月./-]?\\s*(\\d{1,2})?");

    // ==================== 公共方法 ====================

    /**
     * 上传阶段句柄：只承载<b>内存中抽取出的文本</b>与文件名，不含任何文件引用。
     *
     * @param text     抽取出的纯文本（已按 {@link #MAX_TEXT_CHARS} 截断）
     * @param fileName 原始文件名（仅用于页面展示与日志，不用于磁盘定位）
     */
    public record ParseHandle(String text, String fileName) {
    }

    /**
     * 【同步·纯内存】校验上传附件并抽取纯文本；<b>不做任何持久化</b>。
     *
     * <p>在上传请求内完成，只保留文本；文件字节随请求结束释放，不落盘、不进对象存储。
     * 附件大小/类型校验也在此处 fail-fast，避免无效任务入队。</p>
     *
     * @param file 上传的简历附件（PDF/Word/TXT/Markdown）
     * @return 文本句柄（供异步 LLM 解析使用）
     */
    public ParseHandle extractFromUpload(MultipartFile file) {
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
        byte[] bytes;
        try {
            // Spring 的 MultipartFile#getBytes 只在请求生命周期内有效；此处同步读完即用，不落盘
            bytes = file.getBytes();
        } catch (Exception e) {
            throw new ServiceException("读取上传附件失败：" + e.getMessage());
        }
        String text = extractText(originalFileName, bytes);
        if (text == null || text.trim().isEmpty()) {
            throw new ServiceException("未能从附件中抽取到文本内容（可能是扫描件图片型 PDF，请换文本版简历）");
        }
        if (text.length() > MAX_TEXT_CHARS) {
            text = text.substring(0, MAX_TEXT_CHARS);
        }
        log.info("[ResumeParse] 附件已就地抽取文本（不落盘）: file={}, size={}KB, textLength={}",
                originalFileName, bytes.length / 1024, text.length());
        return new ParseHandle(text, originalFileName);
    }

    // ==================== 预览 → 确认 两步式解析 ====================

    /** 预览令牌有效期（毫秒）：10 分钟。超时后需重新上传（避免内存长期驻留原文） */
    private static final long PREVIEW_TTL_MS = 10 * 60 * 1000L;

    /** 预览缓存上限（防御性：异常情况下也不会无限增长） */
    private static final int PREVIEW_CACHE_MAX = 200;

    /**
     * 预览缓存条目。
     *
     * <p><b>为什么放内存</b>：附件与原文都是「客户端一次性输入」，用完即弃；
     * 落库/落盘都会造成空间浪费。内存 + TTL 自动过期最贴合语义，
     * 且服务重启后令牌自然失效 → 前端提示重新上传即可（不做持久化）。</p>
     */
    private record PreviewEntry(Long userId, String fileName, String rawText,
                                ResumeParseVO parsed, long createdAt) {
    }

    private final java.util.concurrent.ConcurrentHashMap<String, PreviewEntry> previewCache =
            new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * 【同步】解析附件并返回<b>预览</b>（不落库）。
     *
     * <p>流程：请求内就地抽取文本 → <b>规则解析</b>（毫秒级，离线可用）→ 缓存预览待确认。</p>
     *
     * <p>设计要点：</p>
     * <ul>
     *   <li><b>纯规则解析</b>：同步路径不调 LLM，保证毫秒级响应、不受 AI 可用性影响；</li>
     *   <li><b>不落库</b>：解析失败/用户放弃都不产生任何记录（杜绝空简历脏数据）；</li>
     *   <li><b>返回原文</b>：供前端左右对照校对（准确性的真正保障）。</li>
     * </ul>
     *
     * @param userId 当前用户
     * @param file   上传附件
     * @return 预览视图（含 previewToken 与 rawText）
     */
    public ResumePreviewVO previewFromUpload(Long userId, MultipartFile file) {
        ParseHandle handle = extractFromUpload(file);
        String text = handle.text();

        // 规则解析：词表来自后台配置（为空则用内置默认词典）
        ResumeParseVO parsed = parseByRuleEngine(text);
        parsed.setTextLength(text.length());

        // 缓存待确认
        evictExpiredPreviews();
        String token = UUID.randomUUID().toString().replace("-", "");
        previewCache.put(token, new PreviewEntry(userId, handle.fileName(), text, parsed,
                System.currentTimeMillis()));
        log.info("[ResumeParse] 预览已生成 token={} userId={} file={} textLength={} sections={}",
                token, userId, handle.fileName(), text.length(), countSections(text));

        ResumePreviewVO vo = new ResumePreviewVO();
        vo.setPreviewToken(token);
        vo.setFileName(handle.fileName());
        vo.setRawText(text);
        vo.setTextLength(text.length());
        vo.setAiPowered(false);
        vo.setName(parsed.getName());
        vo.setGender(parsed.getGender());
        vo.setBirthDate(parsed.getBirthDate());
        vo.setPhone(parsed.getPhone());
        vo.setEmail(parsed.getEmail());
        vo.setJobIntention(parsed.getJobIntention());
        vo.setEducations(parsed.getEducations());
        vo.setWorks(parsed.getWorks());
        vo.setProjects(parsed.getProjects());
        vo.setSkills(parsed.getSkills());
        vo.setSelfIntro(parsed.getSelfIntro());
        int sections = countSections(text);
        vo.setSectionCount(sections);
        vo.setSectionDetectFailed(sections == 0);
        vo.setScannedLike(false);
        return vo;
    }

    /**
     * 【同步】确认预览并落库（创建简历记录）。
     *
     * <p>只有本方法会写库 —— 保证「解析不落库、确认才落库」，
     * 因此解析失败或用户放弃都不会产生脏数据。</p>
     *
     * @param userId       当前用户
     * @param previewToken 预览令牌
     * @param override     用户在前端校对后的修正值（可为 null；为 null 时用解析结果原值）
     * @return 新建的简历记录 ID
     */
    public Long confirmPreview(Long userId, String previewToken, ResumePreviewVO override) {
        if (previewToken == null || previewToken.isBlank()) {
            throw new ServiceException("预览令牌缺失，请重新上传附件");
        }
        PreviewEntry entry = previewCache.get(previewToken);
        if (entry == null || !entry.userId().equals(userId)) {
            throw new ServiceException("预览已过期或无效，请重新上传附件");
        }
        if (System.currentTimeMillis() - entry.createdAt() > PREVIEW_TTL_MS) {
            previewCache.remove(previewToken);
            throw new ServiceException("预览已过期，请重新上传附件");
        }

        // 用户校对后的值优先（前端可改任意字段），未提供则用解析结果
        ResumeParseVO base = entry.parsed();
        ResumeParseVO effective = mergeOverride(base, override);

        PortalUserResume resume = new PortalUserResume();
        resume.setUserId(userId);
        resume.setSourceType("attachment");
        resume.setSourceFileName(entry.fileName());
        resume.setStatus("draft");
        resume.setVersionNo(1);
        resume.setCreateTime(LocalDateTime.now());
        resume.setName(effective.getName());
        resume.setGender(effective.getGender());
        if (effective.getBirthDate() != null && !effective.getBirthDate().isBlank()) {
            try {
                resume.setBirthDate(LocalDate.parse(effective.getBirthDate(),
                        DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            } catch (Exception ignore) {
                // 日期格式异常则不填（宁缺勿错）
            }
        }
        resume.setPhone(effective.getPhone());
        resume.setEmail(effective.getEmail());
        resume.setSelfIntro(effective.getSelfIntro());
        resume.setJobIntention(toJson(effective.getJobIntention()));
        resume.setEducations(toJson(effective.getEducations()));
        resume.setWorks(toJson(effective.getWorks()));
        resume.setProjects(toJson(effective.getProjects()));
        resume.setSkills(toJson(effective.getSkills()));
        resume.setTitle((effective.getName() != null && !effective.getName().isBlank())
                ? effective.getName() + "的简历"
                : removeExtension(entry.fileName()));
        // 全文保留：面试/岗位匹配链路以原文为上下文（无损），故必须写入
        resume.setFullText(buildFullTextFromParseVO(effective));
        // 规则解析置信度 70（低于 LLM 的 85，高于纯规则兜底的 60）：提示下游"经规则抽取"
        resume.setParseConfidence(70);
        resume.setUpdateTime(LocalDateTime.now());
        portalUserResumeMapper.insert(resume);

        previewCache.remove(previewToken);
        log.info("[ResumeParse] 预览已确认落库 resumeId={} userId={}", resume.getId(), userId);
        return resume.getId();
    }

    /** 合并用户校对值：前端传了非空值就用前端值，否则保留解析值 */
    private ResumeParseVO mergeOverride(ResumeParseVO base, ResumePreviewVO o) {
        if (o == null) {
            return base;
        }
        ResumeParseVO r = new ResumeParseVO();
        r.setAiPowered(false);
        r.setTextLength(base.getTextLength());
        r.setName(pick(o.getName(), base.getName()));
        r.setGender(pick(o.getGender(), base.getGender()));
        r.setBirthDate(pick(o.getBirthDate(), base.getBirthDate()));
        r.setPhone(pick(o.getPhone(), base.getPhone()));
        r.setEmail(pick(o.getEmail(), base.getEmail()));
        r.setSelfIntro(pick(o.getSelfIntro(), base.getSelfIntro()));
        r.setJobIntention(o.getJobIntention() != null ? o.getJobIntention() : base.getJobIntention());
        r.setEducations(o.getEducations() != null ? o.getEducations() : base.getEducations());
        r.setWorks(o.getWorks() != null ? o.getWorks() : base.getWorks());
        r.setProjects(o.getProjects() != null ? o.getProjects() : base.getProjects());
        r.setSkills(o.getSkills() != null ? o.getSkills() : base.getSkills());
        return r;
    }

    private String pick(String override, String fallback) {
        return (override != null && !override.isBlank()) ? override : fallback;
    }

    /**
     * 用规则引擎解析（词表来自后台配置；配置为空则用内置默认词典）。
     *
     * <p>技能词域 = 后台配置的通识技能 ∪ <b>岗位模板必备技能</b>（自动聚合，避免硬编码）。</p>
     */
    private ResumeParseVO parseByRuleEngine(String text) {
        Map<String, String> sections = null;
        List<String> skills = null;
        List<String> degrees = null;
        List<String> positions = null;
        try {
            sections = resumeParseConfigService.loadSectionKeywordMap();
            skills = resumeParseConfigService.loadKeywords(
                    com.moyun.portal.service.IPortalResumeParseConfigService.TYPE_SKILL);
            degrees = resumeParseConfigService.loadKeywords(
                    com.moyun.portal.service.IPortalResumeParseConfigService.TYPE_DEGREE);
            positions = resumeParseConfigService.loadKeywords(
                    com.moyun.portal.service.IPortalResumeParseConfigService.TYPE_POSITION);
        } catch (Exception e) {
            log.warn("[ResumeParse] 读取解析配置失败，使用内置默认词典：{}", e.getMessage());
        }
        // 岗位必备技能并入技能词域（零硬编码：复用岗位模板已治理的数据）
        List<String> mergedSkills = new ArrayList<>();
        try {
            for (PortalJobTemplate jt : jobTemplateService.listActive()) {
                if (jt.getRequiredSkills() != null && !jt.getRequiredSkills().isBlank()) {
                    List<String> arr = objectMapper.readValue(jt.getRequiredSkills(),
                            objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
                    for (String s : arr) {
                        if (s != null && !s.isBlank() && !mergedSkills.contains(s.trim())) {
                            mergedSkills.add(s.trim());
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[ResumeParse] 聚合岗位必备技能失败（不阻断解析）：{}", e.getMessage());
        }
        if (skills != null) {
            for (String s : skills) {
                if (!mergedSkills.contains(s)) {
                    mergedSkills.add(s);
                }
            }
        }
        return ruleParser.parse(text, sections, mergedSkills, degrees, positions);
    }

    /** 统计识别到的章节大类数量（用于提示"切分是否成功"） */
    private int countSections(String text) {
        try {
            Map<String, String> sections = resumeParseConfigService.loadSectionKeywordMap();
            if (sections == null || sections.isEmpty()) {
                sections = ResumeRuleParser.defaultSectionKeywords();
            }
            Map<String, List<String>> buckets =
                    ruleParser.splitBySections(ruleParser.normalizeText(text), sections);
            int n = 0;
            for (Map.Entry<String, List<String>> e : buckets.entrySet()) {
                if (!"basic".equals(e.getKey()) && e.getValue() != null && !e.getValue().isEmpty()) {
                    n++;
                }
            }
            return n;
        } catch (Exception e) {
            return 0;
        }
    }

    /** 惰性清理过期预览（无需定时器；每次生成预览时顺带清理） */
    private void evictExpiredPreviews() {
        long now = System.currentTimeMillis();
        previewCache.entrySet().removeIf(e -> now - e.getValue().createdAt() > PREVIEW_TTL_MS);
        if (previewCache.size() > PREVIEW_CACHE_MAX) {
            // 超限时清掉最旧的一批（正常不会触发，纯防御）
            previewCache.entrySet().stream()
                    .sorted(java.util.Comparator.comparingLong(e -> e.getValue().createdAt()))
                    .limit(previewCache.size() - PREVIEW_CACHE_MAX)
                    .map(Map.Entry::getKey)
                    .toList()
                    .forEach(previewCache::remove);
        }
    }

    /**
     * 【异步】对已抽取文本做 LLM 结构化解析，并在<b>成功后才创建</b>简历记录。
     *
     * <p>不再有「先建草稿再解析」——
     * 解析失败（异常抛出）时<b>不产生任何记录</b>，杜绝空简历脏数据。</p>
     *
     * @param userId   当前用户
     * @param text     上传阶段就地抽取的纯文本
     * @param fileName 原始文件名（写入 sourceFileName 供页面展示）
     */
    public ResumeParseVO executeParse(Long userId, String text, String fileName) {
        if (text == null || text.isBlank()) {
            throw new ServiceException("任务参数缺失：解析文本为空");
        }
        String safeFileName = (fileName == null || fileName.isBlank()) ? "附件简历" : fileName;

        // LLM 结构化解析（失败回退规则解析）
        ResumeParseVO vo;
        boolean llmParsed;
        if (aiGlobalSwitch.isEnabled() && aiGlobalSwitch.isResumeAdviceEnabled()) {
            try {
                vo = parseByLlm(userId, text);
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

        // 5. 解析成功 → 新建简历记录（只落结构化字段；解析失败会抛异常，届时零残留）
        PortalUserResume upd = new PortalUserResume();
        upd.setUserId(userId);
        upd.setSourceType("attachment");
        // 不再保存源文件：sourceFileName 仅作展示，sourceFileUrl 留空（文件未持久化）
        upd.setSourceFileName(safeFileName);
        upd.setStatus("draft");
        upd.setVersionNo(1);
        upd.setCreateTime(LocalDateTime.now());
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
        upd.setTitle((vo.getName() != null && !vo.getName().isBlank())
                ? vo.getName() + "的简历"
                : removeExtension(safeFileName));
        upd.setFullText(buildFullTextFromParseVO(vo));
        // 解析置信度（LLM 结构化=85 / 规则兜底=60），供简历深挖出题与追问策略参考
        upd.setParseConfidence(llmParsed ? 85 : 60);
        upd.setUpdateTime(LocalDateTime.now());
        portalUserResumeMapper.insert(upd);

        Long resumeId = upd.getId();
        vo.setAttachmentResumeId(resumeId);
        // 源文件未持久化：不回传 URL
        vo.setSourceFileUrl(null);
        vo.setSourceFileName(safeFileName);
        log.info("[ResumeParse] 解析成功并落库 resumeId={} aiPowered={} textLength={}",
                resumeId, llmParsed, text.length());
        return vo;
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
                    // 必须按位置排序抽取。
                    // 默认模式下 PDFBox 按内容流顺序输出，**双栏/表格简历会文字交错**
                    // （左右栏内容互相穿插），下游无论规则还是 LLM 都会拿到脏文本。
                    PDFTextStripper stripper = new PDFTextStripper();
                    stripper.setSortByPosition(true);
                    // 保留段落感：默认行分隔已足够，这里统一 TAB 为空格避免干扰章节匹配
                    return stripper.getText(doc).replace("\t", " ");
                }
            }
            if (filename.endsWith(".docx")) {
                try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(bytes))) {
                    // 遍历 body 元素，**保留表格的行结构**。
                    // XWPFWordExtractor 会把表格拍平为无分隔文本，
                    // 而简历大量使用表格排版（如「公司 | 时间」），拍平后行/列关系丢失，
                    // 章节与日期锚点都会失效。此处表格单元格用 TAB 连接成一行。
                    return extractDocxWithTables(doc);
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

    /**
     * DOCX 文本抽取（<b>保留表格行结构</b>）
     *
     * <p>为什么不用 {@code XWPFWordExtractor}：它会把表格拍平成一串无分隔文本，
     * 而简历普遍用表格做「公司 | 起止时间」这类排版，拍平后行与列的对应关系丢失，
     * 章节标题识别与日期锚点切分都会失效。</p>
     *
     * <p>本实现遍历 {@code doc.getBodyElements()}：段落原样输出；
     * 表格按「行 → 单元格用 TAB 连接」输出，从而保留「同一行的字段属于同一条目」这一关键信息。</p>
     */
    private String extractDocxWithTables(XWPFDocument doc) {
        StringBuilder sb = new StringBuilder();
        for (org.apache.poi.xwpf.usermodel.IBodyElement el : doc.getBodyElements()) {
            if (el instanceof org.apache.poi.xwpf.usermodel.XWPFParagraph p) {
                String line = p.getText();
                sb.append(line == null ? "" : line).append('\n');
            } else if (el instanceof org.apache.poi.xwpf.usermodel.XWPFTable table) {
                for (org.apache.poi.xwpf.usermodel.XWPFTableRow row : table.getRows()) {
                    StringBuilder rowSb = new StringBuilder();
                    for (org.apache.poi.xwpf.usermodel.XWPFTableCell cell : row.getTableCells()) {
                        String cellText = cell.getText();
                        if (cellText != null && !cellText.isBlank()) {
                            if (rowSb.length() > 0) {
                                rowSb.append('\t');
                            }
                            rowSb.append(cellText.replace("\n", " ").trim());
                        }
                    }
                    if (rowSb.length() > 0) {
                        sb.append(rowSb).append('\n');
                    }
                }
            }
        }
        return sb.toString();
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

    /**
     * 业务收口：经统一网关执行 resume_parse 场景。
     * Handler 提示词与本方法原提示词逐字一致（全字段 Schema），结果从 structured
     * 反序列化为 ResumeParseVO——切换前后解析行为不变。
     */
    private ResumeParseVO parseByLlm(Long userId, String text) {
        // LinkedHashMap 可变 Map（Map.of 不可变集合会被网关输入清洗路径击穿）
        java.util.Map<String, Object> input = new java.util.LinkedHashMap<>();
        input.put("text", text);
        JsonNode node = aiSceneJsonClient.executeForJson(SCENE_RESUME_PARSE, input, userId);
        if (node == null) {
            throw new IllegalStateException("AI网关解析失败");
        }
        ResumeParseVO vo = objectMapper.convertValue(node, ResumeParseVO.class);
        vo.setAiPowered(true);
        return vo;
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