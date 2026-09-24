package com.moyun.ext.cms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.common.exception.system.ServiceException;
import com.moyun.ext.ai.enums.AiSceneEnum;
import com.moyun.ext.aigateway.constant.AiErrorCodes;
import com.moyun.ext.aigateway.model.AiExecuteRequest;
import com.moyun.ext.aigateway.model.AiExecuteResponse;
import com.moyun.ext.aigateway.model.data.GenericSceneData;
import com.moyun.ext.aigateway.service.AiGatewayService;
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

/**
 * CMS 每日写作 prompt 管理 Service 实现
 *
 * <p>新增 AI 生成能力——任务指令在 ai_scene_config.writing_prompt 配置行（2B.2 配置驱动，
 * DefaultSceneExecutor 执行），本类只组装日期/星期/特殊日期业务上下文：
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
    private AiGatewayService aiGatewayService;

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
        if (aiGatewayService != null) {
            try {
                applyAiContent(prompt, date, specialDates);
                aiOk = true;
            } catch (Exception e) {
                log.warn("[WritingPrompt] AI 生成失败，回退内置主题池 date={} err={}", date, e.getMessage());
            }
        } else {
            log.warn("[WritingPrompt] AiGatewayService 未注入（AI 模块未启用），使用内置主题池 date={}", date);
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
        if (aiGatewayService != null) {
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
     * 任务指令在 ai_scene_config.writing_prompt 配置行（2B.2 配置驱动），
     * 本方法只组装业务上下文（日期/星期/特殊日期或季节素材）。
     */
    private void applyAiContent(PortalWritingPrompt prompt, LocalDate date, List<String> specialDates) {
        String context = "今天是" + date.format(DATE_FMT)
                + "，星期" + WEEK_CN.get(date.getDayOfWeek().getValue() - 1) + "。\n";
        if (!specialDates.isEmpty()) {
            context += "今天恰逢：" + String.join("、", specialDates)
                    + "。请将主题与这个特殊日子自然关联。";
        } else {
            context += "今天不是特别的节日，请以当前的【节气/季节/自然物候】为核心素材来设计主题。";
        }
        java.util.Map<String, Object> input = new java.util.HashMap<>();
        input.put("context", context);
        AiExecuteRequest request = new AiExecuteRequest();
        request.setSceneCode(AiSceneEnum.WRITING_PROMPT.getCode());
        request.setInput(input);
        AiExecuteResponse<?> resp = aiGatewayService.execute(request);
        if (resp.getCode() == null || resp.getCode() != AiErrorCodes.SUCCESS
                || !(resp.getData() instanceof GenericSceneData generic)
                || generic.getStructured() == null) {
            throw new RuntimeException("AI 网关调用失败");
        }
        java.util.Map<String, Object> result = generic.getStructured();
        String title = result.get("title") != null ? String.valueOf(result.get("title")) : null;
        String category = result.get("category") != null ? String.valueOf(result.get("category")) : null;
        String description = result.get("description") != null ? String.valueOf(result.get("description")) : null;
        if (title == null || title.isBlank() || description == null || description.isBlank()) {
            throw new RuntimeException("AI 输出格式解析失败");
        }
        prompt.setTitle(truncate(title, 128));
        prompt.setDescription(description.trim());
        prompt.setCategory(normalizeCategory(category));
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

    private String truncate(String s, int max) {
        return s.length() > max ? s.substring(0, max) : s;
    }
}
