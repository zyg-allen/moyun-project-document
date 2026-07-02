package com.moyun.ext.cms.service;

import com.moyun.common.config.RuoYiConfig;
import com.moyun.common.constant.Constants;
import com.moyun.ext.cms.domain.vo.UserResumeVO;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 简历 PDF 导出器（面试空间第2期）
 * <p>
 * 基于已引入的 Apache PDFBox 3.0.3 将结构化简历渲染为多页 PDF。
 * 中文支持依赖系统 CJK 字体（自动扫描常见路径）；未找到时回退 Helvetica（仅 ASCII）。
 *
 * @author moyun
 */
@Component
public class ResumePdfExporter {

    private static final Logger log = LoggerFactory.getLogger(ResumePdfExporter.class);

    /** A4 页面参数（单位：PDF 点，1pt = 1/72 inch） */
    private static final float PAGE_WIDTH = PDRectangle.A4.getWidth();   // 595
    private static final float PAGE_HEIGHT = PDRectangle.A4.getHeight(); // 842
    private static final float MARGIN = 50f;
    private static final float CONTENT_WIDTH = PAGE_WIDTH - 2 * MARGIN;   // 495

    /** CJK 字体注册后的 fontFamily 名称 */
    private static final String CJK_FONT_FAMILY = "MoyunCJK";

    /** 内置 CJK 字体在 classpath 下的位置（随 jar 打包，开箱即用）
     *  使用 TrueType 轮廓的 NotoSansSC（可变字体的 Thin 实例），
     *  PDFBox 3.0.3 对 CFF 轮廓的 OTF 解析有 bug（'loca' table is mandatory），
     *  因此必须使用 TrueType 轮廓的 .ttf 文件。 */
    private static final String BUILTIN_CJK_FONT_PATH = "fonts/NotoSansSC.ttf";

    /** 常见 CJK 字体文件名片段（按优先级） */
    private static final String[] CJK_FONT_CANDIDATES = {
            "NotoSansCJKsc", "NotoSansCJK", "NotoSansSC",
            "SourceHanSansSC", "SourceHanSansCN", "SourceHanSans",
            "wqy-microhei", "wqy-zenhei",
            "DroidSansFallback", "msyh", "msyhbd",
            "simsun", "simhei"
    };

    /** 常见字体目录 */
    private static final String[] FONT_DIRS = {
            "/usr/share/fonts",
            "/usr/local/share/fonts",
            "/usr/share/fonts/truetype",
            "/usr/share/fonts/opentype",
            "C:/Windows/Fonts"
    };

    /** 系统扫描到的可用 CJK 字体路径缓存（仅缓存加载成功的路径） */
    private volatile String cachedSystemCjkFontPath = null;
    private volatile boolean systemFontScanned = false;

    /**
     * 将简历渲染为 PDF 文件，返回可访问的 URL 路径。
     *
     * @param vo 简历内容
     * @return PDF 文件 URL（如 /profile/upload/resume/2024/01/01/xxx.pdf），失败返回 null
     */
    public String exportToPdfUrl(UserResumeVO vo) {
        if (vo == null) {
            return null;
        }

        // 输出目录：{profile}/upload/resume/{datePath}/{uuid}.pdf
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        File dir = new File(RuoYiConfig.getUploadPath() + "/resume/" + datePath);
        if (!dir.exists() && !dir.mkdirs()) {
            log.error("创建简历 PDF 输出目录失败: {}", dir.getAbsolutePath());
            return null;
        }
        String fileName = "resume_" + vo.getId() + "_" + java.util.UUID.randomUUID().toString().replace("-", "") + ".pdf";
        File pdfFile = new File(dir, fileName);

        try (PDDocument doc = new PDDocument(); OutputStream os = new FileOutputStream(pdfFile)) {
            PDFont cjkFont = loadCjkFont(doc);
            if (cjkFont == null) {
                // 字体缺失，删除空文件，返回 null 让上层抛出明确异常
                pdfFile.delete();
                return null;
            }
            PdfCanvas canvas = new PdfCanvas(doc, cjkFont);

            // 标题：姓名
            String displayName = isNotBlank(vo.getName()) ? vo.getName()
                    : (isNotBlank(vo.getTitle()) ? vo.getTitle() : "我的简历");
            canvas.writeCentered(displayName, 20f, MARGIN + 20f);

            // 联系方式行
            StringBuilder contact = new StringBuilder();
            if (isNotBlank(vo.getGender())) contact.append(vo.getGender()).append("  ");
            if (vo.getBirthDate() != null) contact.append(vo.getBirthDate()).append("  ");
            if (isNotBlank(vo.getPhone())) contact.append(vo.getPhone()).append("  ");
            if (isNotBlank(vo.getEmail())) contact.append(vo.getEmail());
            if (contact.length() > 0) {
                canvas.writeCentered(contact.toString().trim(), 9f, 12f);
            }
            canvas.newLine(6f);
            canvas.horizontalLine();
            canvas.newLine(10f);

            // 求职意向
            if (vo.getJobIntention() != null) {
                UserResumeVO.JobIntention ji = vo.getJobIntention();
                canvas.sectionTitle("求职意向");
                if (isNotBlank(ji.getPosition())) canvas.kv("期望职位", ji.getPosition());
                if (isNotBlank(ji.getCity())) canvas.kv("期望城市", ji.getCity());
                if (ji.getSalaryMin() != null && ji.getSalaryMax() != null) {
                    canvas.kv("期望薪资", ji.getSalaryMin() + "-" + ji.getSalaryMax() + " 万/月");
                }
                if (isNotBlank(ji.getJobType())) canvas.kv("工作性质", ji.getJobType());
                if (isNotBlank(ji.getAvailableTime())) canvas.kv("到岗时间", ji.getAvailableTime());
                canvas.newLine(6f);
            }

            // 教育经历
            if (vo.getEducations() != null && !vo.getEducations().isEmpty()) {
                canvas.sectionTitle("教育经历");
                for (UserResumeVO.EducationItem e : vo.getEducations()) {
                    canvas.entryTitle(e.getSchool(), joinDate(e.getStartDate(), e.getEndDate()));
                    String sub = joinLine(e.getMajor(), e.getDegree());
                    if (isNotBlank(sub)) canvas.writeBody(sub, 9f, MARGIN + 8f);
                    if (isNotBlank(e.getDescription())) canvas.writeBody(e.getDescription(), 9f, MARGIN + 8f);
                    canvas.newLine(4f);
                }
                canvas.newLine(6f);
            }

            // 工作经历
            if (vo.getWorks() != null && !vo.getWorks().isEmpty()) {
                canvas.sectionTitle("工作经历");
                for (UserResumeVO.WorkItem w : vo.getWorks()) {
                    canvas.entryTitle(w.getCompany(), joinDate(w.getStartDate(), w.getEndDate()));
                    if (isNotBlank(w.getPosition())) canvas.writeBody(w.getPosition(), 9f, MARGIN + 8f);
                    if (isNotBlank(w.getDescription())) canvas.writeBody(w.getDescription(), 9f, MARGIN + 8f);
                    canvas.newLine(4f);
                }
                canvas.newLine(6f);
            }

            // 项目经历
            if (vo.getProjects() != null && !vo.getProjects().isEmpty()) {
                canvas.sectionTitle("项目经历");
                for (UserResumeVO.ProjectItem p : vo.getProjects()) {
                    canvas.entryTitle(p.getName(), joinDate(p.getStartDate(), p.getEndDate()));
                    if (isNotBlank(p.getRole())) canvas.writeBody(p.getRole(), 9f, MARGIN + 8f);
                    if (isNotBlank(p.getDescription())) canvas.writeBody(p.getDescription(), 9f, MARGIN + 8f);
                    canvas.newLine(4f);
                }
                canvas.newLine(6f);
            }

            // 技能
            if (vo.getSkills() != null && !vo.getSkills().isEmpty()) {
                canvas.sectionTitle("技能列表");
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < vo.getSkills().size(); i++) {
                    UserResumeVO.SkillItem s = vo.getSkills().get(i);
                    if (i > 0) sb.append("  ·  ");
                    sb.append(s.getName());
                    if (isNotBlank(s.getLevel())) sb.append("（").append(s.getLevel()).append("）");
                }
                canvas.writeBody(sb.toString(), 9f, MARGIN);
                canvas.newLine(6f);
            }

            // 自我介绍
            if (isNotBlank(vo.getSelfIntro())) {
                canvas.sectionTitle("自我介绍");
                canvas.writeBody(vo.getSelfIntro(), 9f, MARGIN);
            }

            canvas.finish();

            doc.save(os);

            // 返回可访问的 URL（与 RuoYi 文件上传一致的路径前缀）
            return Constants.RESOURCE_PREFIX + "/upload/resume/" + datePath + "/" + fileName;
        } catch (Exception e) {
            log.error("简历 PDF 导出失败, resumeId={}", vo.getId(), e);
            // 失败时删除半成品文件
            if (pdfFile.exists()) {
                //noinspection ResultOfMethodCallIgnored
                pdfFile.delete();
            }
            return null;
        }
    }

    // ==================== 字体加载 ====================

    /**
     * 加载 CJK 字体，优先级：
     * 1. classpath 内置字体（随 jar 打包，开箱即用）
     * 2. 系统安装的 CJK 字体（扫描常见目录，验证可加载后缓存）
     *
     * 加载失败会跳过该候选继续尝试下一个，避免因单个损坏字体导致整个功能不可用。
     */
    private PDFont loadCjkFont(PDDocument doc) {
        // 1. 优先加载内置字体
        PDFont builtin = loadBuiltinFont(doc);
        if (builtin != null) {
            return builtin;
        }
        // 2. 回退扫描系统字体
        String fontPath = findSystemCjkFontPath();
        if (fontPath != null) {
            try {
                return PDType0Font.load(doc, new File(fontPath));
            } catch (Exception e) {
                // 理论上不会到这里（扫描时已验证），兜底处理
                log.error("系统 CJK 字体加载失败 path={}, err={}", fontPath, e.getMessage());
            }
        }
        // 不回退 Helvetica：PDFBox 3.0 对中文字符会抛 IllegalArgumentException，导出必然失败。
        log.error("未找到可用 CJK 字体，简历 PDF 导出不可用。请在 src/main/resources/fonts/ 下放置 CJK 字体，或安装 fonts-noto-cjk。");
        return null;
    }

    /**
     * 加载 classpath 内置字体（jar 内置，开箱即用）
     */
    private PDFont loadBuiltinFont(PDDocument doc) {
        try {
            ClassPathResource res = new ClassPathResource(BUILTIN_CJK_FONT_PATH);
            if (!res.exists()) {
                return null;
            }
            try (InputStream is = res.getInputStream()) {
                // PDType0Font.load 需要可重复读取，先读到内存
                byte[] bytes = is.readAllBytes();
                return PDType0Font.load(doc, new java.io.ByteArrayInputStream(bytes));
            }
        } catch (Exception e) {
            log.warn("内置 CJK 字体加载失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 查找系统 CJK 字体（仅缓存加载成功的路径，避免损坏文件被永久缓存）
     */
    private String findSystemCjkFontPath() {
        if (systemFontScanned) {
            return cachedSystemCjkFontPath;
        }
        synchronized (this) {
            if (systemFontScanned) {
                return cachedSystemCjkFontPath;
            }
            String found = null;
            for (String dirPath : FONT_DIRS) {
                File dir = new File(dirPath);
                if (!dir.exists() || !dir.isDirectory()) {
                    continue;
                }
                File f = scanCjkFont(dir);
                if (f != null) {
                    found = f.getAbsolutePath();
                    break;
                }
            }
            cachedSystemCjkFontPath = found;
            systemFontScanned = true;
            if (found != null) {
                log.info("简历 PDF 导出使用系统 CJK 字体: {}", found);
            }
            return found;
        }
    }

    /**
     * 递归扫描目录，返回首个文件头有效且文件名匹配候选的字体文件
     */
    private File scanCjkFont(File dir) {
        File[] files = dir.listFiles();
        if (files == null) return null;
        // 先递归子目录
        for (File f : files) {
            if (f.isDirectory()) {
                File sub = scanCjkFont(f);
                if (sub != null) return sub;
            }
        }
        // 同级文件按候选优先级匹配
        for (File f : files) {
            if (f.isFile() && isFontFile(f.getName())) {
                String lower = f.getName().toLowerCase();
                for (String candidate : CJK_FONT_CANDIDATES) {
                    if (lower.contains(candidate.toLowerCase())) {
                        // 验证文件头是否为有效 TTF/OTF/TTC，避免误匹配损坏文件
                        if (isValidFontFile(f)) {
                            return f;
                        } else {
                            log.warn("跳过损坏的字体文件: {}", f.getAbsolutePath());
                        }
                    }
                }
            }
        }
        return null;
    }

    private boolean isFontFile(String name) {
        String lower = name.toLowerCase();
        return lower.endsWith(".ttf") || lower.endsWith(".otf") || lower.endsWith(".ttc");
    }

    /**
     * 验证文件头是否为有效字体（TTF/OTF/TTC）
     * TTF: 0x00010000 或 'true'
     * OTF: 'OTTO'
     * TTC: 'ttcf'
     */
    private boolean isValidFontFile(File f) {
        if (f.length() < 12) return false;
        try (java.io.RandomAccessFile raf = new java.io.RandomAccessFile(f, "r")) {
            int b0 = raf.read() & 0xFF;
            int b1 = raf.read() & 0xFF;
            int b2 = raf.read() & 0xFF;
            int b3 = raf.read() & 0xFF;
            // 0x00010000 (TTF)
            if (b0 == 0x00 && b1 == 0x01 && b2 == 0x00 && b3 == 0x00) return true;
            // 'OTTO' (OTF)
            if (b0 == 0x4F && b1 == 0x54 && b2 == 0x54 && b3 == 0x4F) return true;
            // 'ttcf' (TTC)
            if (b0 == 0x74 && b1 == 0x74 && b2 == 0x63 && b3 == 0x66) return true;
            // 'true' (Apple TTF)
            if (b0 == 0x74 && b1 == 0x72 && b2 == 0x75 && b3 == 0x65) return true;
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    // ==================== 工具方法 ====================

    private String joinDate(String start, String end) {
        if (isNotBlank(start) && isNotBlank(end)) return start + " ~ " + end;
        if (isNotBlank(start)) return start + " ~ 至今";
        if (isNotBlank(end)) return end;
        return "";
    }

    private String joinLine(String a, String b) {
        if (isNotBlank(a) && isNotBlank(b)) return a + " · " + b;
        if (isNotBlank(a)) return a;
        return isNotBlank(b) ? b : "";
    }

    private static boolean isNotBlank(String s) {
        return s != null && !s.trim().isEmpty();
    }

    /** 当前时间格式化（导出时间展示用，保留以备将来扩展） */
    @SuppressWarnings("unused")
    private String nowFormatted() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }

    // ==================== 内部：PDF 画布（处理分页与换行） ====================

    /**
     * 封装多页 PDF 绘制逻辑：维护当前页、内容流、Y 游标。
     * 每次写入前检查剩余空间，不足时自动开新页。
     */
    private static class PdfCanvas {
        private final PDDocument doc;
        private final PDFont font;
        private PDPage page;
        private PDPageContentStream cs;
        private float y;

        PdfCanvas(PDDocument doc, PDFont font) throws Exception {
            this.doc = doc;
            this.font = font;
            this.page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            this.cs = new PDPageContentStream(doc, page);
            this.y = PAGE_HEIGHT - MARGIN;
        }

        /** 确保剩余高度充足，否则开新页 */
        private void ensureSpace(float needed) throws Exception {
            if (y - needed < MARGIN) {
                cs.close();
                page = new PDPage(PDRectangle.A4);
                doc.addPage(page);
                cs = new PDPageContentStream(doc, page);
                y = PAGE_HEIGHT - MARGIN;
            }
        }

        void writeCentered(String text, float fontSize, float gapAfter) throws Exception {
            if (!isNotBlank(text)) return;
            float textWidth = safeWidth(text, fontSize);
            float x = (PAGE_WIDTH - textWidth) / 2f;
            ensureSpace(fontSize + gapAfter);
            cs.beginText();
            cs.setFont(font, fontSize);
            cs.newLineAtOffset(x, y - fontSize);
            cs.showText(text);
            cs.endText();
            y -= (fontSize + gapAfter);
        }

        void writeBody(String text, float fontSize, float indent) throws Exception {
            if (!isNotBlank(text)) return;
            // 按换行拆分
            String[] lines = text.split("\n");
            for (String line : lines) {
                // 自动换行
                List<String> wrapped = wrapLine(line, fontSize, CONTENT_WIDTH - (indent - MARGIN));
                for (String w : wrapped) {
                    ensureSpace(fontSize + 4f);
                    cs.beginText();
                    cs.setFont(font, fontSize);
                    cs.newLineAtOffset(indent, y - fontSize);
                    cs.showText(w);
                    cs.endText();
                    y -= (fontSize + 4f);
                }
            }
        }

        void sectionTitle(String title) throws Exception {
            newLine(4f);
            ensureSpace(16f);
            cs.beginText();
            cs.setFont(font, 13f);
            cs.newLineAtOffset(MARGIN, y - 13f);
            cs.showText(title);
            cs.endText();
            y -= 18f;
            // 标题下短横线
            horizontalLine();
            newLine(4f);
        }

        void entryTitle(String title, String date) throws Exception {
            if (!isNotBlank(title)) title = "";
            ensureSpace(14f);
            float titleWidth = safeWidth(title, 11f);
            cs.beginText();
            cs.setFont(font, 11f);
            cs.newLineAtOffset(MARGIN, y - 11f);
            cs.showText(title);
            cs.endText();
            // 右侧日期
            if (isNotBlank(date)) {
                float dateWidth = safeWidth(date, 9f);
                cs.beginText();
                cs.setFont(font, 9f);
                cs.newLineAtOffset(PAGE_WIDTH - MARGIN - dateWidth, y - 11f);
                cs.showText(date);
                cs.endText();
            }
            y -= 14f;
        }

        void kv(String k, String v) throws Exception {
            String line = k + "：" + v;
            writeBody(line, 9f, MARGIN);
        }

        void horizontalLine() throws Exception {
            ensureSpace(2f);
            cs.setLineWidth(0.5f);
            cs.moveTo(MARGIN, y);
            cs.lineTo(PAGE_WIDTH - MARGIN, y);
            cs.stroke();
            y -= 2f;
        }

        void newLine(float gap) {
            y -= gap;
        }

        void finish() throws Exception {
            cs.close();
        }

        private float safeWidth(String text, float fontSize) {
            try {
                return font.getStringWidth(text) / 1000f * fontSize;
            } catch (Exception e) {
                return text.length() * fontSize * 0.5f;
            }
        }

        /** 简单字符级换行（按可用宽度切分） */
        private List<String> wrapLine(String line, float fontSize, float maxWidth) {
            java.util.List<String> result = new java.util.ArrayList<>();
            if (line == null || line.isEmpty()) {
                result.add("");
                return result;
            }
            if (safeWidth(line, fontSize) <= maxWidth) {
                result.add(line);
                return result;
            }
            StringBuilder cur = new StringBuilder();
            for (int i = 0; i < line.length(); i++) {
                cur.append(line.charAt(i));
                if (safeWidth(cur.toString(), fontSize) > maxWidth) {
                    // 回退一个字符
                    cur.deleteCharAt(cur.length() - 1);
                    result.add(cur.toString());
                    cur = new StringBuilder();
                    cur.append(line.charAt(i));
                }
            }
            if (cur.length() > 0) result.add(cur.toString());
            return result;
        }

        private static boolean isNotBlank(String s) {
            return s != null && !s.trim().isEmpty();
        }
    }
}
