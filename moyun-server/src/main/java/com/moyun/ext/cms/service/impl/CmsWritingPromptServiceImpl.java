package com.moyun.ext.cms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.common.exception.system.ServiceException;
import com.moyun.ext.ai.service.LLMService;
import com.moyun.ext.cms.service.ICmsWritingPromptService;
import com.moyun.portal.domain.entity.PortalWritingPrompt;
import com.moyun.portal.mapper.PortalWritingPromptMapper;
import com.moyun.portal.util.SpecialDateProvider;
import com.moyun.util.string.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * CMS 每日写作 prompt 管理 Service 实现
 *
 * <p>v10.7：新增 AI 生成能力——
 * <ul>
 *   <li>结合当日特殊日期（节日/节气，见 {@link SpecialDateProvider}）构造提示词，调用默认聊天模型生成；</li>
 *   <li>AI 不可用/失败/输出解析失败时，回退内置主题池（按日期序号轮换，保证每天稳定有产出）；</li>
 *   <li>uk_prompt_date 唯一键兜底防重复，insertPrompt 冲突时忽略。</li>
 * </ul>
 *
 * @author moyun
 */
@Service
public class CmsWritingPromptServiceImpl implements ICmsWritingPromptService {

    private static final Logger log = LoggerFactory.getLogger(CmsWritingPromptServiceImpl.class);

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy年M月d日");
    private static final List<String> WEEK_CN = Arrays.asList("一", "二", "三", "四", "五", "六", "日");

    /** AI 输出解析：title / category / description 三行结构 */
    private static final Pattern TITLE_PATTERN = Pattern.compile("标题[:：]\\s*(.+)");
    private static final Pattern CATEGORY_PATTERN = Pattern.compile("分类[:：]\\s*(.+)");
    private static final Pattern DESC_PATTERN = Pattern.compile("描述[:：]\\s*([\\s\\S]+)");

    /** AI 失败回退主题池（与前台分类一致），按"年内第几天 % 池大小"轮换 */
    private static final String[][] FALLBACK_POOL = {
            {"生活", "窗外的一平方米", "观察你窗外一平方米的世界十分钟，写下你注意到的一个此前从未留意的细节，以及它让你想到的事。"},
            {"职场", "如果重来一次", "回忆你职业生涯中做过的一个重要选择，如果重来一次你会怎么做？写下当时的权衡与现在的答案。"},
            {"情感", "没说出口的那句话", "写下一句你一直想说却没有说出口的话，以及这句话背后的故事。"},
            {"虚构", "便利店的深夜顾客", "深夜便利店里来了一位行为奇怪的顾客，以店员的视角写下这个晚上的故事。"},
            {"哲思", "慢的价值", "在这个追求效率的时代，写一件你刻意放慢去做的事，以及\"慢\"带给你的东西。"},
            {"生活", "今天的味道", "描写今天你尝到的一种味道，它让你想起了谁、哪段时光？"},
            {"职场", "我的第一位同事", "写写你职场中遇到的第一位同事，他/她教会了你什么？"},
            {"情感", "谢谢你，陌生人", "写一次陌生人给你的善意，哪怕很小，你至今记得。"},
            {"虚构", "最后一班地铁", "末班地铁上只剩下你和一位熟睡的乘客，到站时你发现对方手里攥着一张写给\"你\"的纸条……续写这个故事。"},
            {"哲思", "拥有的与需要的", "清点一下你真正需要的东西和你拥有的东西，写写两者之间的差距。"}
    };

    @Autowired
    private PortalWritingPromptMapper promptMapper;

    /** AI 生成服务（未配置默认模型时调用会抛异常，由回退逻辑兜底） */
    @Autowired(required = false)
    private LLMService llmService;

    @Override
    public Page<PortalWritingPrompt> selectPromptPage(Page<PortalWritingPrompt> page, PortalWritingPrompt prompt) {
        LambdaQueryWrapper<PortalWritingPrompt> wrapper = new LambdaQueryWrapper<>();
        if (prompt != null) {
            if (prompt.getTitle() != null && !prompt.getTitle().isEmpty()) {
                wrapper.like(PortalWritingPrompt::getTitle, prompt.getTitle());
            }
            if (prompt.getCategory() != null && !prompt.getCategory().isEmpty()) {
                wrapper.eq(PortalWritingPrompt::getCategory, prompt.getCategory());
            }
            if (prompt.getSource() != null && !prompt.getSource().isEmpty()) {
                wrapper.eq(PortalWritingPrompt::getSource, prompt.getSource());
            }
        }
        wrapper.orderByDesc(PortalWritingPrompt::getPromptDate);
        return promptMapper.selectPage(page, wrapper);
    }

    @Override
    public PortalWritingPrompt selectPromptById(Long id) {
        return promptMapper.selectById(id);
    }

    @Override
    public int insertPrompt(PortalWritingPrompt prompt) {
        if (prompt.getSource() == null || prompt.getSource().isBlank()) {
            prompt.setSource("manual");
        }
        if (prompt.getCreatedTime() == null) {
            prompt.setCreatedTime(java.time.LocalDateTime.now());
        }
        // uk_prompt_date 唯一键兜底：同日已存在则跳过（返回 0）
        LambdaQueryWrapper<PortalWritingPrompt> exist = new LambdaQueryWrapper<>();
        exist.eq(PortalWritingPrompt::getPromptDate, prompt.getPromptDate());
        if (promptMapper.selectCount(exist) > 0) {
            return 0;
        }
        return promptMapper.insert(prompt);
    }

    @Override
    public int updatePrompt(PortalWritingPrompt prompt) {
        return promptMapper.updateById(prompt);
    }

    @Override
    public int deletePromptByIds(Long[] ids) {
        return promptMapper.deleteBatchIds(Arrays.asList(ids));
    }

    // ==================== AI 生成 ====================

    @Override
    public PortalWritingPrompt aiGenerateForDate(LocalDate date) {
        // 已存在则直接返回（幂等）
        LambdaQueryWrapper<PortalWritingPrompt> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PortalWritingPrompt::getPromptDate, date);
        PortalWritingPrompt existing = promptMapper.selectOne(wrapper);
        if (existing != null) {
            return existing;
        }

        List<String> specialDates = SpecialDateProvider.getSpecialDates(date);
        String festivalName = specialDates.isEmpty() ? null : String.join("·", specialDates);

        PortalWritingPrompt prompt = new PortalWritingPrompt();
        prompt.setPromptDate(date);
        prompt.setFestivalName(festivalName);
        prompt.setSource("ai");
        prompt.setCreatedTime(java.time.LocalDateTime.now());

        boolean aiOk = false;
        if (llmService != null) {
            try {
                applyAiContent(prompt, date, specialDates);
                aiOk = true;
            } catch (Exception e) {
                log.warn("[WritingPrompt] AI 生成失败，回退内置主题池 date={} err={}", date, e.getMessage());
            }
        } else {
            log.warn("[WritingPrompt] LLMService 未注入（AI 模块未启用），使用内置主题池 date={}", date);
        }
        if (!aiOk) {
            applyFallbackContent(prompt, date);
        }

        promptMapper.insert(prompt);
        log.info("[WritingPrompt] 生成写作提示 date={} source={} festival={} title={}",
                date, prompt.getSource(), festivalName, prompt.getTitle());
        return prompt;
    }

    @Override
    public int aiGenerateRange(LocalDate startDate, int days) {
        int count = 0;
        for (int i = 0; i < days; i++) {
            LocalDate date = startDate.plusDays(i);
            LambdaQueryWrapper<PortalWritingPrompt> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(PortalWritingPrompt::getPromptDate, date);
            if (promptMapper.selectCount(wrapper) > 0) {
                continue;
            }
            aiGenerateForDate(date);
            count++;
        }
        return count;
    }

    @Override
    public PortalWritingPrompt aiRegenerate(Long id) {
        PortalWritingPrompt prompt = promptMapper.selectById(id);
        if (prompt == null) {
            throw new ServiceException("写作提示不存在: " + id);
        }
        LocalDate date = prompt.getPromptDate();
        List<String> specialDates = SpecialDateProvider.getSpecialDates(date);
        String festivalName = specialDates.isEmpty() ? null : String.join("·", specialDates);

        boolean aiOk = false;
        if (llmService != null) {
            try {
                applyAiContent(prompt, date, specialDates);
                aiOk = true;
            } catch (Exception e) {
                log.warn("[WritingPrompt] AI 重新生成失败，回退内置主题池 id={} err={}", id, e.getMessage());
            }
        }
        if (!aiOk) {
            applyFallbackContent(prompt, date);
        }
        prompt.setFestivalName(festivalName);
        prompt.setSource(StringUtils.hasText(prompt.getSource()) ? prompt.getSource() : "manual");
        promptMapper.updateById(prompt);
        return prompt;
    }

    // ==================== 内部工具 ====================

    /**
     * 调用 AI 生成标题/描述/分类并填充到 prompt（解析失败抛异常触发回退）。
     */
    private void applyAiContent(PortalWritingPrompt prompt, LocalDate date, List<String> specialDates) {
        String aiPrompt = buildAiPrompt(date, specialDates);
        String response = llmService.generate(aiPrompt);
        if (response == null || response.isBlank()) {
            throw new RuntimeException("AI 返回空内容");
        }
        String title = extract(TITLE_PATTERN, response);
        String category = extract(CATEGORY_PATTERN, response);
        String description = extract(DESC_PATTERN, response);
        if (title == null || title.isBlank() || description == null || description.isBlank()) {
            throw new RuntimeException("AI 输出格式解析失败");
        }
        prompt.setTitle(truncate(title, 128));
        prompt.setDescription(description.trim());
        prompt.setCategory(normalizeCategory(category));
    }

    /**
     * 构造 AI 提示词：日期 + 星期 + 特殊日期上下文 + 输出格式约束。
     * 优化点：
     * 1. 明确主题必须“具体可写”，避免空泛；
     * 2. 分类严格限定，确保后台管理一致；
     * 3. 描述部分提供写作切入点和示例角度，激发用户灵感；
     * 4. 特殊日期要求“自然关联”，而非生硬命题；
     * 5. 无特殊日期时，以“节气/季节/自然物候”作为核心素材，提供具体可写的季节主题。
     */
    private String buildAiPrompt(LocalDate date, List<String> specialDates) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是社区写作平台的编辑，负责为每天设计一个“今日写作主题”，激励创作者写出真实、有感染力的文章。\n");
        sb.append("今天是").append(date.format(DATE_FMT))
                .append("，星期").append(WEEK_CN.get(date.getDayOfWeek().getValue() - 1)).append("。\n");

        // ========== 核心改动：特殊日期判断，无则走季节主题 ==========
        if (!specialDates.isEmpty()) {
            sb.append("今天恰逢：").append(String.join("、", specialDates))
                    .append("。请将主题与这个特殊日子自然关联（避免生硬点题），可结合该日的氛围、情感或常见活动来设计。");
        } else {
            // ✅ 精确描述：明确告诉 AI 去取“这个季节”的素材，而非泛泛而谈
            sb.append("今天不是特别的节日，请以当前的【节气/季节/自然物候】为核心素材库来设计主题。\n");
            sb.append("例如：这个季节有什么典型的花、果、天气现象（雨/雪/风/霜）、农事活动、自然景观？\n");
            sb.append("有哪些因季节而产生的生活场景（如换季整理、时令饮食、户外活动）？\n");
            sb.append("将这些自然元素融入主题，让内容有“季节感”，而非凭空抒情。\n");
            // 补充一个具体示例，让 AI 理解“结合季节”是什么样子
            sb.append("示例：");
            sb.append("· 若秋季：枫叶、银杏、桂花、秋雨、丰收\n");
            sb.append("· 若冬季：雪、炉火、腊梅、年末总结\n");
            sb.append("· 若春季：樱花、春雨、清明、新芽\n");
            sb.append("· 若夏季：蝉鸣、荷花、暴雨、西瓜\n");
        }
        sb.append("\n要求：\n");
        sb.append("1. 主题必须具体、可写，能激发真实表达，避免“人生”“梦想”等大词，最好聚焦一个场景、一段回忆或一种情绪。\n");
        sb.append("2. 标题在 30 字以内，简洁有力，吸引点击。\n");
        sb.append("3. 描述在 100 字以内，必须包含“写作切入点”，例如“可以写一次雨中的等待”、“试着描述你通勤路上看到的一个人”等具体引导。\n");
        sb.append("4. 分类严格限定为以下之一：生活/职场/情感/虚构/哲思，不要输出其他分类。\n");
        sb.append("严格按如下三行格式输出，不要任何多余内容（包括解释、前缀、序号）：\n");
        sb.append("标题：xxx\n分类：xx\n描述：xxx");
        return sb.toString();
    }

    /** AI 失败时的内置主题池回退（按年内天数轮换，保证稳定可用） */
    private void applyFallbackContent(PortalWritingPrompt prompt, LocalDate date) {
        int idx = date.getDayOfYear() % FALLBACK_POOL.length;
        String[] item = FALLBACK_POOL[idx];
        prompt.setTitle(item[1]);
        prompt.setCategory(item[0]);
        prompt.setDescription(item[2]);
        prompt.setSource("manual");
    }

    /** 分类归一化：映射到标准五分类，非法值归入"生活" */
    private String normalizeCategory(String category) {
        if (category == null) {
            return "生活";
        }
        String c = category.trim();
        List<String> standard = Arrays.asList("生活", "职场", "情感", "虚构", "哲思");
        for (String s : standard) {
            if (s.equals(c)) {
                return s;
            }
        }
        return "生活";
    }

    private String extract(Pattern pattern, String text) {
        Matcher m = pattern.matcher(text);
        return m.find() ? m.group(1).trim() : null;
    }

    private String truncate(String s, int max) {
        return s.length() > max ? s.substring(0, max) : s;
    }
}
