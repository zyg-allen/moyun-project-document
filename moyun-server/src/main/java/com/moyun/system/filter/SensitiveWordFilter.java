package com.moyun.system.filter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.moyun.system.domain.entity.SysSensitiveWord;
import com.moyun.system.mapper.SysSensitiveWordMapper;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

/**
 * 敏感词过滤器（DFA 词树实现）
 * <p>启动时从 sys_sensitive_word 加载启用词到内存 DFA 树，
 * 词库变更后调用 {@link #reload()} 触发刷新。检测 O(n) 复杂度。</p>
 *
 * <p>设计要点：
 * <ul>
 *   <li>线程安全：通过 AtomicReference 整体替换词树，读路径无锁。</li>
 *   <li>忽略大小写：统一转小写构建树与匹配。</li>
 *   <li>支持中英文混合、跳过空白符，避免常见绕过（如 "敏感 词"）。</li>
 * </ul>
 *
 * @author moyun
 */
@Slf4j
@Component
public class SensitiveWordFilter {

    /**
     * DFA 词树根节点：key 为字符，value 为子节点
     * <p>结束标记使用 {@link #END_MARKER} 作为 key，{@link Boolean#TRUE} 作为 value，
     * 与 Map 的 Character 键类型保持一致，避免混用 String key 造成类型不一致。
     */
    private final AtomicReference<Map<Character, Object>> rootRef = new AtomicReference<>(new HashMap<>());

    /** 当前加载的敏感词数量（仅用于观测） */
    private volatile int wordCount = 0;

    /**
     * 词尾结束标记：使用空字符 '\0' 作为 key
     * <p>敏感词均为正常文本字符，不会包含空字符，保证无冲突。
     * 值使用 {@link Boolean#TRUE}，便于通过 {@code Boolean.TRUE.equals(...)} 安全判断。
     */
    private static final Character END_MARKER = '\0';

    @Autowired
    private SysSensitiveWordMapper sensitiveWordMapper;

    /**
     * 启动时加载词库
     */
    @PostConstruct
    public void init() {
        reload();
    }

    /**
     * 重新加载词库（管理后台维护后调用）
     */
    public synchronized void reload() {
        LambdaQueryWrapper<SysSensitiveWord> qw = new LambdaQueryWrapper<>();
        qw.eq(SysSensitiveWord::getStatus, "0");
        List<SysSensitiveWord> words = sensitiveWordMapper.selectList(qw);
        Map<Character, Object> newRoot = buildTree(words);
        rootRef.set(newRoot);
        wordCount = words.size();
        log.info("敏感词词库加载完成，共 {} 个启用词", wordCount);
    }

    /**
     * 获取当前加载的敏感词数量
     */
    public int getWordCount() {
        return wordCount;
    }

    /**
     * 检测内容是否包含敏感词
     *
     * @param content 原始内容（null 或空返回 false）
     * @return true 表示命中
     */
    public boolean contains(String content) {
        return !find(content).isEmpty();
    }

    /**
     * 查找内容中命中的敏感词
     *
     * @param content 原始内容
     * @return 命中敏感词列表（去重，保持命中顺序），无命中返回空列表
     */
    @SuppressWarnings("unchecked")
    public List<String> find(String content) {
        List<String> hits = new ArrayList<>();
        if (content == null || content.isEmpty()) {
            return hits;
        }
        // 跳过空白符，统一小写，避免 "敏感 词" 绕过
        String normalized = content.replaceAll("\\s+", "").toLowerCase();
        Map<Character, Object> root = rootRef.get();
        if (root.isEmpty()) {
            return hits;
        }

        int len = normalized.length();
        Set<String> dedup = new java.util.HashSet<>();
        for (int i = 0; i < len; i++) {
            Map<Character, Object> current = root;
            int j = i;
            StringBuilder matched = new StringBuilder();
            while (j < len) {
                char ch = normalized.charAt(j);
                Object next = current.get(ch);
                if (next == null) {
                    break;
                }
                matched.append(ch);
                current = (Map<Character, Object>) next;
                if (Boolean.TRUE.equals(current.get(END_MARKER))) {
                    String word = matched.toString();
                    if (dedup.add(word)) {
                        hits.add(word);
                    }
                    // 继续尝试更长匹配（覆盖"敏感词"与"敏感词库"）
                }
                j++;
            }
        }
        return hits;
    }

    /**
     * 脱敏替换：将命中的敏感词替换为 *
     * <p>保留原文空白与大小写，仅在命中区间替换为 *，无命中返回原文。</p>
     *
     * @param content 原始内容
     * @return 脱敏后的内容（无命中返回原文）
     */
    @SuppressWarnings("unchecked")
    public String mask(String content) {
        if (content == null || content.isEmpty()) {
            return content;
        }
        Map<Character, Object> root = rootRef.get();
        if (root.isEmpty()) {
            return content;
        }

        int len = content.length();
        StringBuilder result = new StringBuilder(len);
        int i = 0;
        boolean masked = false; // 是否发生过替换，用于无命中时直接返回原文
        while (i < len) {
            // 跳过空白符做匹配，但保留原文空白
            Map<Character, Object> current = root;
            int j = i;
            int lastMatchEnd = -1; // 命中区间的右端（含），相对原文索引
            // 在原文中向前探测，跳过空白符对齐 DFA 节点
            while (j < len) {
                char ch = content.charAt(j);
                if (Character.isWhitespace(ch)) {
                    j++;
                    continue;
                }
                Object next = current.get(Character.toLowerCase(ch));
                if (next == null) {
                    break;
                }
                current = (Map<Character, Object>) next;
                if (Boolean.TRUE.equals(current.get(END_MARKER))) {
                    lastMatchEnd = j;
                }
                j++;
            }
            if (lastMatchEnd >= 0) {
                // 将原文 [i, lastMatchEnd] 区间替换为 *（保留区间内空白符以维持格式）
                for (int k = i; k <= lastMatchEnd; k++) {
                    char orig = content.charAt(k);
                    result.append(Character.isWhitespace(orig) ? orig : '*');
                }
                i = lastMatchEnd + 1;
                masked = true;
            } else {
                // 当前字符未命中任何敏感词，原样输出
                result.append(content.charAt(i));
                i++;
            }
        }
        return masked ? result.toString() : content;
    }

    /**
     * 构建 DFA 词树
     */
    @SuppressWarnings("unchecked")
    private Map<Character, Object> buildTree(List<SysSensitiveWord> words) {
        Map<Character, Object> root = new HashMap<>();
        if (CollectionUtils.isEmpty(words)) {
            return root;
        }
        for (SysSensitiveWord sw : words) {
            String word = sw.getWord();
            if (word == null || word.isEmpty()) {
                continue;
            }
            String w = word.trim().toLowerCase();
            if (w.isEmpty()) {
                continue;
            }
            Map<Character, Object> current = root;
            for (int i = 0; i < w.length(); i++) {
                char ch = w.charAt(i);
                Object next = current.get(ch);
                if (!(next instanceof Map)) {
                    next = new HashMap<Character, Object>();
                    current.put(ch, next);
                }
                current = (Map<Character, Object>) next;
            }
            current.put(END_MARKER, Boolean.TRUE);
        }
        return root;
    }
}
