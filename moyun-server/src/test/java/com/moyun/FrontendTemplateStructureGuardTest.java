package com.moyun;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 前端模板结构守卫（v13.60）。
 *
 * <p><b>为什么必须有它</b>：v13.59 修复了一个**潜伏的结构缺陷** —— 报告页的
 * 「发展方向」tab 被重复插入（新旧两版共存）、按钮被截断（`:class`/`@click`/文本全丢）、
 * 容器缺少闭合 `</div>`。而当时 <b>{@code vue-tsc -b} 通过、后端 465 例测试全绿</b>
 * —— Vue 模板对结构问题极其宽容，**所有现有质量门都放行了**。
 * 该缺陷最终是靠"统计 tab 数量得到 6（应为 5）"这一偶然盘点发现的。</p>
 *
 * <p>本守卫把那次的排查经验固化为可回归断言，覆盖三类真实发生过的问题：</p>
 * <ol>
 *   <li><b>tab 值重复</b>：同一页面的 {@code tab-group} 值集合与 {@code v-if} 集合必须一致
 *       （重复插入会让两个集合的元素个数不等）；</li>
 *   <li><b>多行标签被截断</b>：形如 <code>&lt;button</code> 结束的行，下一行必须是属性行
 *       （缩进更深的非空行）—— 原缺陷正是 `<button` 后直接跟注释块；</li>
 *   <li><b>容器闭合缺失</b>：关键容器（如 {@code report-tabs}）必须有配对的闭合标签，
 *       且闭合行缩进与开标签一致。</li>
 * </ol>
 *
 * <p><b>不做什么</b>：本守卫**不做通用 HTML/Vue 解析**
 * （那需要引入解析器且易产生误报）。它只针对"本项目实际踩过的坑"做**窄而确定**的检查
 * —— 宁可少查，不可误报（误报会让守卫被绕过，比没有守卫更糟）。</p>
 *
 * @author laomao
 */
class FrontendTemplateStructureGuardTest {

    /** 报告页（三 Tab 结构最复杂，v13.59 缺陷现场） */
    private static final Path REPORT_PAGE =
            Path.of("../moyun-portal/src/pages/interview/VoiceInterviewPage.vue");

    /** 管理端场景配置页（双下拉改造处） */
    private static final Path SCENE_ADMIN_PAGE =
            Path.of("../moyun-admin-vue/src/views/ai/scene/index.vue");

    /** 需要做"多行标签截断"检查的前端文件 */
    private static final List<Path> MULTILINE_TAG_FILES = List.of(REPORT_PAGE, SCENE_ADMIN_PAGE);

    // ========================================================================
    // 守卫 1：tab 值集合一致（能抓住"tab 被重复插入"）
    // ========================================================================

    @Test
    @DisplayName("报告页：tab 按钮值集合 与 tab 内容 v-if 集合 必须一致（含无重复）")
    void reportTabValuesAreConsistent() throws IOException {
        assertTrue(Files.exists(REPORT_PAGE), "报告页不存在：" + REPORT_PAGE.toAbsolutePath());
        String src = Files.readString(REPORT_PAGE, StandardCharsets.UTF_8);

        // 关键：tab 按钮 = **带 report-tab 类的元素**，不能只按 @click 匹配。
        // 教训（本守卫第一版即踩到）：页面里另有「跳转按钮」（概要区「查看逐题分析 →」）
        // 也写 @click="reportTab = 'analysis'"，裸匹配会把跳转按钮误认成 tab 按钮。
        List<String> tabButtonRaw = tabButtonValues(src);
        int contentCount = rawCapture(src, Pattern.compile("class=\"tab-content active\"")).size();

        assertFalse(tabButtonRaw.isEmpty(), "未解析到任何 tab 按钮 —— 解析口径可能已漂移");
        assertTrue(contentCount > 0, "未解析到任何 tab 内容 div —— 解析口径可能已漂移");

        // ① 无重复（v13.59 缺陷：insight tab 出现过两次）
        assertEquals(tabButtonRaw.stream().distinct().count(), (long) tabButtonRaw.size(),
                "tab 按钮值出现重复：" + tabButtonRaw + "（是否重复插入了同一 tab？）");

        // ② 内容 div 数 == 按钮数 + 1
        //    「对话回放」是**无按钮 tab**（由 finish 后自动切换展示，历史查看时呈现），
        //    故恒等式为：内容 = 可见按钮 + 1。v13.59 缺陷时该式不成立（按钮被截断→按钮数偏少）。
        assertEquals(tabButtonRaw.size() + 1, contentCount,
                "tab 内容 div 数应为『tab 按钮数 + 1（对话回放无按钮）』，实际：按钮 "
                        + tabButtonRaw.size() + " 个、内容 " + contentCount
                        + " 份 —— 不符即说明有 tab 被重复插入或按钮被截断/丢失");
        // 且「对话回放」必须恰有 1 份内容
        assertEquals(1, rawCapture(src, Pattern.compile("v-if=\"reportTab === 'dialog'\"")).size(),
                "「对话回放」内容分支应恰有 1 份（重复插入会产生多份）");

        // ③ 每个 tab 值都必须有同名 v-if 内容分支
        for (String v : tabButtonRaw) {
            assertTrue(src.contains("v-if=\"reportTab === '" + v + "'\""),
                    "tab 按钮 " + v + " 没有对应的内容分支 v-if=\"reportTab === '" + v + "'\"");
        }
    }

    @Test
    @DisplayName("报告页：三层五 tab 的 tab 数量必须恰为 5（防再次重复/遗漏）")
    void reportHasExactlyFiveTabs() throws IOException {
        String src = Files.readString(REPORT_PAGE, StandardCharsets.UTF_8);
        List<String> contentValues = rawCapture(src,
                Pattern.compile("v-if=\"reportTab\\s*===\\s*'([a-zA-Z]+)'\""));
        assertEquals(5, contentValues.size(),
                "报告页应为三层五 tab（概要/问题分析/追问预测/发展方向/对话回放），实际：" + contentValues);

        // 且必须包含方案 V1.3 §6.1 定义的 5 个 tab 值
        for (String expected : List.of("summary", "analysis", "predict", "insight", "dialog")) {
            assertTrue(contentValues.contains(expected),
                    "缺少 tab：" + expected + "，实际：" + contentValues);
        }
    }

    // ========================================================================
    // 守卫 2：多行标签不得被截断（能抓住 v13.59 的"按钮属性丢失"）
    // ========================================================================

    @Test
    @DisplayName("前端页面：多行起始标签（<button/<el-* 等）的下一行必须是属性行，不得被注释/标签打断")
    void multilineTagsAreNotTruncated() throws IOException {
        List<String> violations = new ArrayList<>();
        for (Path file : MULTILINE_TAG_FILES) {
            if (!Files.exists(file)) {
                continue;
            }
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            for (int i = 0; i < lines.size() - 1; i++) {
                String cur = lines.get(i);
                String trimmed = cur.trim();
                // 只看"标签名后无其它内容"的多行起始标签。
                // 注意：不能用 "<tag$" 之类的正则去要求整行以 '>' 结尾 ——
                // 属性值里可能含 '>'（如 v-show="filteredTotal > 0"），会误判为截断。
                if (!trimmed.matches("<[a-zA-Z][a-zA-Z0-9-]*")) {
                    continue;
                }
                int curIndent = indentOf(cur);
                // 找下一个非空行
                int j = i + 1;
                while (j < lines.size() && lines.get(j).trim().isEmpty()) {
                    j++;
                }
                if (j >= lines.size()) {
                    continue;
                }
                String next = lines.get(j);
                String nextTrimmed = next.trim();
                // 合法：下一行缩进更深，且以属性/指令开头，且**不是注释**
                boolean deeper = indentOf(next) > curIndent;
                boolean looksLikeAttr = nextTrimmed.startsWith(":") || nextTrimmed.startsWith("@")
                        || nextTrimmed.startsWith("v-")
                        || nextTrimmed.matches(
                                "^(class|style|disabled|placeholder|type|value|slot|ref|label|name|method|url|"
                                        + "width|rows|size|filterable|clearable|required|multiple|key|id|src|alt)"
                                        + "[=\\s>].*$");
                boolean isComment = nextTrimmed.startsWith("<!--") || nextTrimmed.startsWith("//");
                if (!deeper || isComment || !looksLikeAttr) {
                    violations.add(file.getFileName() + ":" + (i + 1)
                            + " 多行起始标签 <" + trimmed.substring(1) + "> 的下一行不是属性行："
                            + (nextTrimmed.length() > 60 ? nextTrimmed.substring(0, 60) + "…" : nextTrimmed));
                }
            }
        }
        assertTrue(violations.isEmpty(),
                "发现被截断的多行起始标签（v13.59 缺陷形态：<button 后紧跟注释块，属性全丢）：\n"
                        + String.join("\n", violations));
    }

    // ========================================================================
    // 守卫 3：关键容器必须有配对闭合标签且缩进一致
    // ========================================================================

    @Test
    @DisplayName("报告页：report-tabs 容器必须有配对闭合 </div>（v13.59 缺陷：闭合标签丢失）")
    void reportTabsContainerIsClosed() throws IOException {
        List<String> lines = Files.readAllLines(REPORT_PAGE, StandardCharsets.UTF_8);
        int open = -1;
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).contains("class=\"report-tabs\"")) {
                open = i;
                break;
            }
        }
        assertTrue(open >= 0, "未找到 report-tabs 容器 —— 正则/结构可能已漂移");

        // 其闭合应在合理距离内（该容器只含 tab 按钮，不会很长）
        int openIndent = indentOf(lines.get(open));
        int close = -1;
        for (int i = open + 1; i < Math.min(lines.size(), open + 40); i++) {
            String t = lines.get(i).trim();
            if (t.equals("</div>") && indentOf(lines.get(i)) == openIndent) {
                close = i;
                break;
            }
        }
        assertTrue(close > 0,
                "report-tabs 容器（L" + (open + 1) + "）缺少同缩进的闭合 </div> —— "
                        + "v13.59 缺陷正是此处闭合标签被插入操作吞掉");
        // 容器内应至少有 3 个 tab 按钮（若按钮被截断，这里也会显著减少）
        // 计数口径：:class 绑定里出现的 "['report-tab'" 才是 tab 按钮；
        // 容器自身的 class="report-tabs" 与分组标签 class="report-tabs-group" 都会被排除
        // 计数口径与守卫 1 一致（按 <button> 元素 + report-tab 类），避免"按行计数"把
        // 容器 class="report-tabs"、分组 class="report-tabs-group" 也算进来。
        long buttons = tabButtonValues(String.join("\n", lines.subList(open, close + 1))).size();
        // 期望 4 而非 5：「对话回放」没有 tab 按钮 —— 它由 finish 后自动切换展示，
        // 仅历史查看/报告完整时以内容分支呈现。此口径由本守卫钉死，防止按钮被误删。
        assertEquals(4L, buttons,
                "report-tabs 内 tab 按钮数异常（应恰为 4，实际 " + buttons + "）—— 是否有按钮被截断/丢失？");
    }

    // ========================================================================
    // 工具
    // ========================================================================


    /**
     * 提取全部匹配（**保留重复**，不去重）。
     *
     * <p>有捕获组时取第 1 组，否则取整体匹配 —— 避免对"只判断存在性"的正则
     * （如 {@code class="tab-content active"}）调用 {@code group(1)} 抛
     * {@code IndexOutOfBoundsException}。</p>
     */
    private static List<String> rawCapture(String src, Pattern p) {
        List<String> out = new ArrayList<>();
        Matcher m = p.matcher(src);
        while (m.find()) {
            out.add(m.groupCount() >= 1 && m.group(1) != null ? m.group(1) : m.group());
        }
        return out;
    }

    /**
     * 提取"tab 按钮"的值：沿每一行找带 {@code report-tab} 类的元素，
     * 并在其后若干行内找 {@code @click="reportTab = 'xxx'"}。
     */
    private static List<String> tabButtonValues(String src) {
        List<String> out = new ArrayList<>();
        Pattern buttonEl = Pattern.compile("<button\\b([^>]*)>(.*?)</button>", Pattern.DOTALL);
        Pattern click = Pattern.compile("@click=\"reportTab\\s*=\\s*'([a-zA-Z]+)'\"");
        Matcher el = buttonEl.matcher(src);
        while (el.find()) {
            String body = el.group(1) + el.group(2);
            if (!body.contains("'report-tab'")) {
                continue; // 非 tab 按钮（如 class="practice-btn" 的跳转按钮）
            }
            Matcher c = click.matcher(body);
            if (c.find()) {
                out.add(c.group(1));
            }
        }
        return out;
    }

    private static int indentOf(String line) {
        int n = 0;
        while (n < line.length() && (line.charAt(n) == ' ' || line.charAt(n) == '\t')) {
            n++;
        }
        return n;
    }
}
