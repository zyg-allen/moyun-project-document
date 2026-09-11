package com.moyun.ext.cms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.moyun.common.exception.system.ServiceException;
import com.moyun.ext.ai2.constant.AiErrorCodes;
import com.moyun.ext.ai2.model.AiExecuteRequest;
import com.moyun.ext.ai2.model.AiExecuteResponse;
import com.moyun.ext.ai2.model.data.QuestionSceneData;
import com.moyun.ext.ai2.service.AiGatewayService;
import com.moyun.ext.cms.service.IPortalJobTemplateService;
import com.moyun.ext.cms.service.LlmClient;
import com.moyun.portal.domain.entity.PortalInterviewQuestion;
import com.moyun.portal.domain.entity.PortalJobTemplate;
import com.moyun.portal.mapper.PortalInterviewQuestionMapper;
import com.moyun.portal.mapper.PortalJobTemplateMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 岗位模板服务实现（v11.x 智能出题）
 *
 * <p>C4 JD 关键词提取：优先 LLM 结构化提取（限 15 词），失败回退规则分词
 * （技术词表匹配 + 停用词过滤），保证无 LLM 也可用。</p>
 *
 * @author moyun
 */
@Service
public class PortalJobTemplateServiceImpl extends ServiceImpl<PortalJobTemplateMapper, PortalJobTemplate>
        implements IPortalJobTemplateService {
    /** v11.39：本服务所属 AI 场景代码（绑定见 ai_scene_config，业务不感知模型选择） */
    private static final String SCENE_QUESTION_GENERATE = "question_generate";


    private static final Logger log = LoggerFactory.getLogger(PortalJobTemplateServiceImpl.class);

    /** LLM 提取关键词上限 */
    private static final int MAX_LLM_KEYWORDS = 15;

    /** 规则分词上限（兜底路径同样限制） */
    private static final int MAX_RULE_KEYWORDS = 15;

    /** 规则兜底：常见技术词表（命中即作为关键词） */
    private static final List<String> TECH_VOCABULARY = Arrays.asList(
            "Java", "JVM", "Spring", "Spring Boot", "Spring Cloud", "MyBatis", "MyBatis-Plus",
            "MySQL", "PostgreSQL", "Oracle", "SQL", "Redis", "MongoDB", "Elasticsearch",
            "Kafka", "RocketMQ", "RabbitMQ", "消息队列", "分布式", "微服务", "高并发",
            "Docker", "Kubernetes", "K8s", "Linux", "Git", "CI/CD", "Jenkins",
            "JavaScript", "TypeScript", "Vue", "Vue3", "React", "Angular", "Node.js",
            "HTML", "CSS", "Webpack", "Vite", "小程序", "uni-app",
            "Python", "Django", "Flask", "Go", "Golang", "Rust", "C++", "C#",
            "TCP", "HTTP", "HTTPS", "网络编程", "操作系统", "数据结构", "算法",
            "多线程", "并发编程", "JUC", "锁", "事务", "索引", "分库分表",
            "设计模式", "系统设计", "架构", "性能优化", "安全", "XSS", "CSRF");

    /** 停用词（分词后过滤） */
    private static final Set<String> STOP_WORDS = Set.of(
            "的", "了", "和", "与", "及", "或", "等", "以上", "以下", "优先", "熟练", "掌握",
            "熟悉", "了解", "负责", "参与", "具备", "相关", "经验", "能力", "要求", "岗位职责",
            "任职", "加分", "良好", "较强", "and", "the", "for", "with", "you", "your");

    @Autowired
    private LlmClient llmClient;

    /** v11.58 P0-3：JD 关键词提取统一走 AI 网关（task=jd_keywords 子任务） */
    @Autowired
    private AiGatewayService aiGatewayService;

    @Autowired
    private PortalInterviewQuestionMapper questionMapper;

    @Override
    public List<PortalJobTemplate> listActive() {
        return list(new LambdaQueryWrapper<PortalJobTemplate>()
                .eq(PortalJobTemplate::getStatus, "active")
                .orderByAsc(PortalJobTemplate::getCategory)
                .orderByAsc(PortalJobTemplate::getName));
    }

    @Override
    public List<String> extractKeywords(String jdText) {
        if (jdText == null || jdText.isBlank()) {
            throw new ServiceException("JD 内容不能为空");
        }
        // 1. LLM 提取（可用且成功时优先）
        if (llmClient.isEnabled()) {
            try {
                List<String> llmKeywords = extractByLlm(jdText);
                if (llmKeywords != null && !llmKeywords.isEmpty()) {
                    return llmKeywords;
                }
            } catch (Exception e) {
                log.warn("[JobTemplate] LLM 关键词提取失败，回退规则分词：{}", e.getMessage());
            }
        }
        // 2. 规则兜底：技术词表匹配 + 分词过滤
        return extractByRule(jdText);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int bindQuestions(Long templateId, List<Long> questionIds) {
        PortalJobTemplate template = getById(templateId);
        if (template == null) {
            throw new ServiceException("岗位模板不存在");
        }
        // 1. 清空该模板下所有题目归属（全量覆盖语义）
        questionMapper.update(null, new LambdaUpdateWrapper<PortalInterviewQuestion>()
                .eq(PortalInterviewQuestion::getJobTemplateId, templateId)
                .set(PortalInterviewQuestion::getJobTemplateId, null));
        // 2. 绑定指定题目
        if (questionIds == null || questionIds.isEmpty()) {
            return 0;
        }
        questionMapper.update(null, new LambdaUpdateWrapper<PortalInterviewQuestion>()
                .in(PortalInterviewQuestion::getId, questionIds)
                .set(PortalInterviewQuestion::getJobTemplateId, templateId));
        log.info("[JobTemplate] 模板 {} 关联题目 {} 道", templateId, questionIds.size());
        return questionIds.size();
    }

    // ==================== 关键词提取（C4） ====================

    /**
     * v11.58 P0-3 业务收口：经统一网关执行 question_generate 场景（task=jd_keywords）。
     * 提示词已收编至 QuestionGenerateHandler（逐字一致），本方法仅做结果清洗
     * （去空白/去重/限制上限）；失败返回空列表由上层回退规则分词。
     */
    private List<String> extractByLlm(String jdText) {
        AiExecuteRequest request = new AiExecuteRequest();
        request.setSceneCode(SCENE_QUESTION_GENERATE);
        Map<String, Object> input = new HashMap<>();
        input.put("task", "jd_keywords");
        input.put("context", jdText);
        request.setInput(input);

        AiExecuteResponse<?> resp = aiGatewayService.execute(request);
        if (resp.getCode() == null || resp.getCode() != AiErrorCodes.SUCCESS
                || !(resp.getData() instanceof QuestionSceneData data)
                || data.getKeywords() == null || data.getKeywords().isEmpty()) {
            log.warn("[JobTemplate] 网关关键词提取未得结果: code={}, msg={}", resp.getCode(), resp.getMsg());
            return List.of();
        }
        // 清洗：去空白、去重、限制上限
        Set<String> cleaned = new LinkedHashSet<>();
        for (String kw : data.getKeywords()) {
            if (kw != null) {
                String t = kw.trim();
                if (!t.isEmpty() && t.length() <= 20) {
                    cleaned.add(t);
                }
            }
            if (cleaned.size() >= MAX_LLM_KEYWORDS) {
                break;
            }
        }
        return new ArrayList<>(cleaned);
    }

    private List<String> extractByRule(String jdText) {
        String lower = jdText.toLowerCase(Locale.ROOT);
        Set<String> keywords = new LinkedHashSet<>();
        // 1. 技术词表匹配（大小写不敏感，保留词表原文）
        for (String vocab : TECH_VOCABULARY) {
            if (lower.contains(vocab.toLowerCase(Locale.ROOT))) {
                keywords.add(vocab);
            }
            if (keywords.size() >= MAX_RULE_KEYWORDS) {
                return new ArrayList<>(keywords);
            }
        }
        // 2. 补充：中文短语切分（2-6字连续中文片段，过滤停用词）
        for (String token : jdText.split("[\\s,，、;；。：:（）()\\[\\]{}\"'`/\\n\\r\\t]+")) {
            String t = token.trim();
            if (t.length() >= 2 && t.length() <= 6 && t.matches("[\\u4e00-\\u9fa5]+") && !STOP_WORDS.contains(t)) {
                keywords.add(t);
            }
            if (keywords.size() >= MAX_RULE_KEYWORDS) {
                break;
            }
        }
        return new ArrayList<>(keywords);
    }
}