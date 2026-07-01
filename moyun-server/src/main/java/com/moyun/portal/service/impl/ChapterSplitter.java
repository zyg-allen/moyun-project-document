package com.moyun.portal.service.impl;

import com.moyun.portal.domain.query.ChapterSplitRule;
import com.moyun.portal.domain.vo.ParsedChapter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 章节分章引擎
 *
 * <p>支持四种模式：auto/regex/heading/fixed</p>
 *
 * @author moyun
 */
@Component
public class ChapterSplitter {

    /**
     * 中文章节标题正则（按优先级排序）
     * 匹配：第一章、第1章、第123章、第一回、第一节、第1节、序章、楔子、引子、尾声
     */
    private static final Pattern[] CN_CHAPTER_PATTERNS = {
            Pattern.compile("^\\s*第[零一二三四五六七八九十百千万0-9]+[章节回部篇]\\s*[\\s\\S]*?$"),
            Pattern.compile("^\\s*(序章|楔子|引子|前言|序言|后记|尾声|番外篇?|引言)\\s*$")
    };

    /** 英文章节标题正则 */
    private static final Pattern[] EN_CHAPTER_PATTERNS = {
            Pattern.compile("^\\s*Chapter\\s+(\\d+|[IVXLCDM]+)\\s*[:：.\\-—]?\\s*.*$", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^\\s*Part\\s+\\d+\\s*[:：.\\-—]?\\s*.*$", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^\\s*(Prologue|Epilogue|Preface)\\s*$", Pattern.CASE_INSENSITIVE)
    };

    /** 分卷识别正则 */
    private static final Pattern[] VOLUME_PATTERNS = {
            Pattern.compile("^\\s*第[零一二三四五六七八九十百千万0-9]+[卷部篇]\\s*[\\s\\S]*?$"),
            Pattern.compile("^\\s*(上卷|中卷|下卷|上部|中部|下部|上篇|中篇|下篇)\\s*$")
    };

    /** Markdown 标题正则 */
    private static final Pattern MD_HEADING_PATTERN = Pattern.compile("^(#{1,6})\\s+(.+)$");

    /**
     * 按规则分章
     */
    public List<ParsedChapter> split(String text, ChapterSplitRule rule) {
        if (rule == null) {
            rule = new ChapterSplitRule();
        }
        String mode = rule.getMode() == null ? "auto" : rule.getMode();
        switch (mode) {
            case "regex":
                return splitByRegex(text, rule);
            case "heading":
                return splitByHeading(text, rule);
            case "fixed":
                return splitByFixed(text, rule);
            case "auto":
            default:
                return splitAuto(text, rule);
        }
    }

    /**
     * 自动识别模式：依次尝试中文→英文→Markdown标题，若都没匹配到则按固定字数切
     */
    private List<ParsedChapter> splitAuto(String text, ChapterSplitRule rule) {
        // 先尝试中文/英文章节正则
        List<ParsedChapter> chapters = splitByPatterns(text, rule, CN_CHAPTER_PATTERNS, EN_CHAPTER_PATTERNS);
        if (chapters != null && !chapters.isEmpty()) {
            return mergeShortChapters(chapters, rule);
        }
        // 再尝试 Markdown 标题
        chapters = splitByHeading(text, rule);
        if (chapters != null && !chapters.isEmpty()) {
            return mergeShortChapters(chapters, rule);
        }
        // 都没识别到，按固定字数切
        return splitByFixed(text, rule);
    }

    /**
     * 自定义正则模式
     */
    private List<ParsedChapter> splitByRegex(String text, ChapterSplitRule rule) {
        if (rule.getRegex() == null || rule.getRegex().isEmpty()) {
            return splitAuto(text, rule);
        }
        Pattern p;
        try {
            p = Pattern.compile(rule.getRegex(), Pattern.MULTILINE);
        } catch (Exception e) {
            // 正则非法，退回 auto
            return splitAuto(text, rule);
        }
        Matcher m = p.matcher(text);
        List<int[]> positions = new ArrayList<>();
        List<String> titles = new ArrayList<>();
        while (m.find()) {
            positions.add(new int[]{m.start(), m.end()});
            titles.add(m.group().trim());
        }
        return buildChapters(text, positions, titles, rule);
    }

    /**
     * 按 Markdown 标题级别切分
     */
    private List<ParsedChapter> splitByHeading(String text, ChapterSplitRule rule) {
        int targetLevel = parseHeadingLevel(rule.getHeadingLevel());
        if (targetLevel <= 0) targetLevel = 2; // 默认按 H2 切

        String[] lines = text.split("\\r?\\n", -1);
        List<int[]> positions = new ArrayList<>();
        List<String> titles = new ArrayList<>();

        int offset = 0;
        for (String line : lines) {
            Matcher m = MD_HEADING_PATTERN.matcher(line);
            if (m.matches() && m.group(1).length() == targetLevel) {
                positions.add(new int[]{offset, offset + line.length()});
                titles.add(m.group(2).trim());
            }
            offset += line.length() + 1; // +1 为换行符
        }
        return buildChapters(text, positions, titles, rule);
    }

    /**
     * 按固定字数切段
     */
    private List<ParsedChapter> splitByFixed(String text, ChapterSplitRule rule) {
        int fixed = rule.getFixedWordCount() == null ? 3000 : rule.getFixedWordCount();
        if (fixed <= 0) fixed = 3000;
        List<ParsedChapter> chapters = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return chapters;
        }
        int total = text.length();
        int chapterNo = 0;
        for (int i = 0; i < total; i += fixed) {
            chapterNo++;
            int end = Math.min(i + fixed, total);
            String chunk = text.substring(i, end);
            String title = "第" + chapterNo + "章";
            ParsedChapter c = new ParsedChapter(chapterNo, title, chunk);
            chapters.add(c);
        }
        return chapters;
    }

    /**
     * 通用：按多个 Pattern 数组识别章节标题
     */
    private List<ParsedChapter> splitByPatterns(String text, ChapterSplitRule rule,
                                                 Pattern[]... patternGroups) {
        String[] lines = text.split("\\r?\\n", -1);
        List<int[]> positions = new ArrayList<>();
        List<String> titles = new ArrayList<>();

        int offset = 0;
        for (String line : lines) {
            boolean matched = false;
            for (Pattern[] group : patternGroups) {
                for (Pattern p : group) {
                    if (p.matcher(line).matches()) {
                        positions.add(new int[]{offset, offset + line.length()});
                        titles.add(line.trim());
                        matched = true;
                        break;
                    }
                }
                if (matched) break;
            }
            offset += line.length() + 1;
        }
        if (positions.isEmpty()) return null;
        return buildChapters(text, positions, titles, rule);
    }

    /**
     * 根据识别到的章节标题位置，构建章节列表
     *
     * @param text      原始文本
     * @param positions 每个章节标题的 [start, end) 位置
     * @param titles    章节标题
     * @param rule      规则
     */
    private List<ParsedChapter> buildChapters(String text, List<int[]> positions, List<String> titles,
                                              ChapterSplitRule rule) {
        List<ParsedChapter> chapters = new ArrayList<>();
        if (positions.isEmpty()) {
            return chapters;
        }

        // 标题前若有内容，作为"序言/楔子"
        int firstStart = positions.get(0)[0];
        if (firstStart > 0) {
            String preface = text.substring(0, firstStart).trim();
            if (!preface.isEmpty() && preface.length() > 20) {
                ParsedChapter c = new ParsedChapter(0, "序章", preface);
                chapters.add(c);
            }
        }

        boolean detectVolume = rule.getDetectVolume() == null ? true : rule.getDetectVolume();
        int chapterNo = 0;
        for (int i = 0; i < positions.size(); i++) {
            int titleStart = positions.get(i)[0];
            int contentStart = positions.get(i)[1];
            int contentEnd = (i + 1 < positions.size()) ? positions.get(i + 1)[0] : text.length();
            String title = titles.get(i);
            String content = text.substring(contentStart, contentEnd).trim();

            // 识别分卷（不作为独立章节，只标记）
            if (detectVolume && isVolume(title)) {
                // 分卷标题作为下一章的开头（或单独标记）
                chapterNo++;
                ParsedChapter c = new ParsedChapter(chapterNo, title, content);
                c.setIsVolume(true);
                chapters.add(c);
                continue;
            }

            chapterNo++;
            // 规整标题：去掉多余空白
            title = title.replaceAll("\\s+", " ").trim();
            ParsedChapter c = new ParsedChapter(chapterNo, title, content);
            chapters.add(c);
        }
        return chapters;
    }

    /**
     * 合并过短章节到上一章
     */
    private List<ParsedChapter> mergeShortChapters(List<ParsedChapter> chapters, ChapterSplitRule rule) {
        int minWords = rule.getMinChapterWords() == null ? 100 : rule.getMinChapterWords();
        if (minWords <= 0 || chapters.size() <= 1) {
            return chapters;
        }
        List<ParsedChapter> result = new ArrayList<>();
        for (ParsedChapter c : chapters) {
            if (result.isEmpty()) {
                result.add(c);
                continue;
            }
            ParsedChapter last = result.get(result.size() - 1);
            // 分卷或字数过少时合并到上一章
            if (Boolean.TRUE.equals(c.getIsVolume()) ||
                    (c.getWordCount() != null && c.getWordCount() < minWords)) {
                last.setContent(last.getContent() + "\n\n" + c.getTitle() + "\n\n" + c.getContent());
                last.setWordCount(ParsedChapter.countWords(last.getContent()));
            } else {
                result.add(c);
            }
        }
        // 重新编号
        int no = 0;
        for (ParsedChapter c : result) {
            no++;
            c.setChapterNo(no);
        }
        return result;
    }

    private boolean isVolume(String title) {
        if (title == null) return false;
        for (Pattern p : VOLUME_PATTERNS) {
            if (p.matcher(title).matches()) return true;
        }
        return false;
    }

    private int parseHeadingLevel(String level) {
        if (level == null || level.isEmpty()) return 0;
        String s = level.toLowerCase().replace("h", "").replace("#", "");
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return 0;
        }
    }
}
