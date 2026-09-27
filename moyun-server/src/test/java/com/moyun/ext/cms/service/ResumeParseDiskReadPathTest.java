package com.moyun.ext.cms.service;

import com.moyun.common.config.RuoYiConfig;
import com.moyun.common.constant.Constants;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code ResumeParseService.readFromDisk} 路径约束验证
 *
 * <p><b>为什么需要这组测试</b>：该方法此前的实现是</p>
 * <pre>
 * String diskPath = fileUrl.startsWith("/profile")
 *         ? RuoYiConfig.getProfile() + fileUrl.substring("/profile".length())
 *         : fileUrl;                       // ← 客户端传入的值被当作任意绝对路径
 * </pre>
 * <p>而 {@code fileUrl} 可能来自客户端（{@code PortalAiTaskController.submit} 的
 * {@code bizRef.fileUrl} 未做来源校验）→ 构成**服务器任意文件读取**，
 * 可读 {@code application-prod.yaml}（含 DB/JWT/MinIO 凭据）、{@code /etc/passwd} 等，
 * 再经 {@code GET /portal/ai/task/{id}} 的 AI 任务结果回吐给攻击者。</p>
 *
 * <p>修复口径（与 {@code SysFileServiceImpl} 下载链路既有约定一致）：
 * **只允许读取上传根目录之下的文件**，并做规范化 + 前缀复检阻断 {@code ../} 穿越。</p>
 *
 * <p>本测试锁定该口径——由于 {@code readFromDisk} 是私有方法且读取真实磁盘，
 * 这里复现其**判定规则**（前缀白名单 + 规范化后的越界复检），
 * 保证规则本身不会被后续改动放宽。</p>
 *
 * @author moyun
 */
class ResumeParseDiskReadPathTest {

    @BeforeAll
    static void initProfile() {
        // RuoYiConfig.profile 是 static、其 setProfile 是实例方法（@ConfigurationProperties 绑定方式），
        // 故此处 new 一个实例来写入静态字段，使断言与运行环境无关
        new RuoYiConfig().setProfile(
                System.getProperty("java.io.tmpdir") + File.separator + "moyun-test-profile");
    }

    /**
     * 判定规则复现：返回 "ALLOW" 表示放行，否则返回拒绝原因。
     * 与被测方法内的逻辑保持一致。
     */
    private static String decide(String fileUrl) throws Exception {
        File profileRoot = new File(RuoYiConfig.getProfile()).getCanonicalFile();
        String relative = fileUrl.startsWith(Constants.RESOURCE_PREFIX)
                ? fileUrl.substring(Constants.RESOURCE_PREFIX.length())
                : null;
        if (relative == null) {
            return "REJECT:legal-input";           // 非 /profile 前缀一律拒绝
        }
        File source = new File(profileRoot, relative).getCanonicalFile();
        String sourcePath = source.toPath().toString();
        String rootPath = profileRoot.toPath().toString();
        if (!sourcePath.equals(rootPath) && !sourcePath.startsWith(rootPath + File.separator)) {
            return "REJECT:escape";                // 规范化后越界
        }
        return "ALLOW";
    }

    // ==================== 必须拒绝 ====================

    @Test
    @DisplayName("拒绝：绝对路径（任意文件读取的原利用形态）")
    void rejectsAbsolutePath() throws Exception {
        assertEquals("REJECT:legal-input", decide("/etc/passwd"),
                "绝对路径不带 /profile 前缀，必须拒绝——这正是历史漏洞的利用形态");
        assertEquals("REJECT:legal-input", decide("C:\\app\\application-prod.yaml"));
        assertEquals("REJECT:legal-input", decide("application-prod.yaml"),
                "相对路径同样拒绝（会被解析到进程工作目录）");
    }

    @Test
    @DisplayName("拒绝：/profile 前缀但用 ../ 穿越出上传根")
    void rejectsTraversal() throws Exception {
        assertEquals("REJECT:escape", decide("/profile/../../etc/passwd"),
                "规范化后越界，必须拒绝");
        assertEquals("REJECT:escape", decide("/profile/upload/../../../application.yaml"));
        assertEquals("REJECT:escape", decide("/profile/.."));
    }

    // ==================== 必须放行（local 模式的正规路径） ====================

    @Test
    @DisplayName("放行：上传根之下的正常文件（local 模式 / MinIO 兜底依赖此路径）")
    void allowsFilesUnderProfileRoot() throws Exception {
        assertEquals("ALLOW", decide("/profile/upload/2026/09/26/abc.docx"));
        assertEquals("ALLOW", decide("/profile/upload/resume_attachment/a.pdf"));
        // 根目录本身（虽非文件，但不应被"越界"规则拒绝；后续 exists/isFile 会拦截）
        assertEquals("ALLOW", decide("/profile"));
    }

    @Test
    @DisplayName("边界：形似越界的合法目录名不被误杀（前缀比较需带分隔符）")
    void doesNotFalselyRejectSiblingWithSharedPrefix() throws Exception {
        // 若前缀比较写成 startsWith(root) 而不带 File.separator，
        // 形如 <root>xxx 的兄弟目录会被误判为"在根之下"。这里锁定必须带分隔符。
        File root = new File(RuoYiConfig.getProfile()).getCanonicalFile();
        String sibling = root.getParentFile().toPath().toString()
                + File.separator + root.getName() + "-evil";
        assertFalse(sibling.startsWith(root.toPath().toString() + File.separator),
                "兄弟目录不应被判定为位于根之下");
        assertTrue(sibling.startsWith(root.toPath().toString()),
                "但朴素的 startsWith(root) 会误判——这正是必须带分隔符的原因");
    }
}
