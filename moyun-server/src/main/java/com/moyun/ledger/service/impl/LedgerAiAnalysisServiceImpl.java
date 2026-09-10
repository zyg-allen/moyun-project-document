package com.moyun.ledger.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moyun.core.base.entity.SysDictData;
import com.moyun.ext.ai.entity.AiSceneConfig;
import com.moyun.ext.ai.mapper.AiSceneConfigMapper;
import com.moyun.ext.ai2.constant.AiErrorCodes;
import com.moyun.ext.ai2.model.AiExecuteRequest;
import com.moyun.ext.ai2.model.AiExecuteResponse;
import com.moyun.ledger.domain.entity.LedgerAiAnalysisReport;
import com.moyun.ledger.domain.entity.LedgerAssetAccount;
import com.moyun.ledger.domain.entity.LedgerBudget;
import com.moyun.ledger.domain.entity.LedgerLiabilityAccount;
import com.moyun.ledger.domain.entity.LedgerTransaction;
import com.moyun.ledger.mapper.LedgerAiAnalysisReportMapper;
import com.moyun.ledger.mapper.LedgerAssetAccountMapper;
import com.moyun.ledger.mapper.LedgerBudgetMapper;
import com.moyun.ledger.mapper.LedgerLiabilityAccountMapper;
import com.moyun.ledger.mapper.LedgerTransactionMapper;
import com.moyun.ledger.service.ILedgerAiAnalysisService;
import com.moyun.portal.domain.entity.PortalUser;
import com.moyun.portal.mapper.PortalUserMapper;
import com.moyun.portal.service.IPortalUserService;
import com.moyun.system.service.ISysDictTypeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 记账 AI 财务分析服务实现
 *
 * <p>v11.50 架构（Service 薄化 / 配置即场景）：本 Service 只承担业务编排——
 * 快照缓存（指纹失效）/ 网关调用 / 报告落表 / 画像管理。数据组装（查库/指标/趋势上下文）
 * 与 LLM 变换全部下沉 {@link com.moyun.ledger.handler.FinanceAnalysisHandler}（场景差异化编码区），
 * 提示词模板/人设/输出结构由 ai_scene_config 配置驱动，管理页修改即时生效。</p>
 *
 * <p>流程：指纹命中快照→直接返回；否则网关 execute(finance_analysis, {userId, range})
 * → Handler 查数组装+模板渲染+LLM（失败内部降级）→ 返回完整报告 → 落表月度快照 → 返回。</p>
 *
 * @author moyun
 */
@Service
public class LedgerAiAnalysisServiceImpl implements ILedgerAiAnalysisService {
    /** 本服务所属 AI 场景代码（绑定见 ai_scene_config，Handler 为 financeAnalysisHandler） */
    private static final String SCENE_FINANCE_ANALYSIS = "finance_analysis";

    private static final Logger log = LoggerFactory.getLogger(LedgerAiAnalysisServiceImpl.class);

    private static final String DICT_IDENTITY_TAG = "ledger_identity_tag";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** 合法分析维度（非法值回落 month） */
    private static final List<String> VALID_RANGES = List.of("month", "3m", "6m", "year");
    private static final Map<String, String> RANGE_LABEL = Map.of("month", "本月", "3m", "近3个月", "6m", "近6个月", "year", "近12个月");

    @Autowired
    private IPortalUserService portalUserService;
    @Autowired
    private PortalUserMapper portalUserMapper;
    @Autowired
    private ISysDictTypeService dictTypeService;
    /** 报告快照 Mapper */
    @Autowired
    private LedgerAiAnalysisReportMapper reportMapper;
    /** 场景配置读取（指纹纳入配置版本，改模板自动失效当月快照） */
    @Autowired
    private AiSceneConfigMapper sceneConfigMapper;
    /** 以下 Mapper 仅用于数据指纹（缓存键计算），数据组装在 Handler */
    @Autowired
    private LedgerTransactionMapper transactionMapper;
    @Autowired
    private LedgerAssetAccountMapper assetAccountMapper;
    @Autowired
    private LedgerLiabilityAccountMapper liabilityAccountMapper;
    @Autowired
    private LedgerBudgetMapper budgetMapper;

    /** AI 统一网关（com.moyun.ext.ai2）：限流/缓存/日志/降级统一编排 */
    @Autowired(required = false)
    private com.moyun.ext.ai2.service.AiGatewayService aiGatewayService;

    @Override
    public Map<String, Object> analyze(Long userId, boolean refresh, String range) {
        // 维度归一化（非法值回落本月）
        if (!VALID_RANGES.contains(range)) {
            range = "month";
        }
        String rangeLabel = RANGE_LABEL.get(range);
        String period = LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"));
        // 仅"本月"维度读写月度快照；其他维度实时计算
        boolean snapshotable = "month".equals(range);
        PortalUser user = portalUserService.selectPortalUserById(userId);
        LocalDate today = LocalDate.now();
        // 数据指纹（流水/资产/负债/预算/画像 + 场景配置变更痕迹）——命中快照前比对
        String fingerprint = snapshotable ? buildFingerprint(userId, today, user) : null;
        if (snapshotable && !refresh) {
            LedgerAiAnalysisReport cached = reportMapper.selectOne(new LambdaQueryWrapper<LedgerAiAnalysisReport>()
                    .eq(LedgerAiAnalysisReport::getUserId, userId)
                    .eq(LedgerAiAnalysisReport::getPeriod, period));
            if (cached != null && fingerprint != null && fingerprint.equals(cached.getDataFingerprint())) {
                Map<String, Object> r = reportToResult(cached, true);
                r.put("range", range);
                r.put("rangeLabel", rangeLabel);
                return r;
            }
        }

        // 网关执行：Handler 全权负责查数→指标→模板渲染→LLM→降级
        Map<String, Object> data = executeViaGateway(userId, range);
        String aiSummary = data.get("summary") != null ? String.valueOf(data.get("summary")) : "";
        boolean aiEnabled = Boolean.TRUE.equals(data.get("aiEnabled"));
        int healthScore = data.get("healthScore") instanceof Number n ? n.intValue() : 0;
        List<Map<String, Object>> risks = castList(data.get("risks"));
        List<Map<String, Object>> suggestions = castList(data.get("suggestions"));
        Map<String, Object> indicators = data.get("indicators") instanceof Map<?, ?> m ? castMap(m) : new LinkedHashMap<>();
        List<Map<String, Object>> incomeSources = castList(data.get("incomeSources"));

        // ===== 组装返回 =====
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("period", period);
        result.put("healthScore", healthScore);
        result.put("indicators", indicators);
        result.put("incomeSources", incomeSources);
        result.put("debtRisks", risks);
        result.put("suggestions", suggestions);
        result.put("aiSummary", aiSummary);
        result.put("aiEnabled", aiEnabled);
        result.put("fromCache", false);
        result.put("range", range);
        result.put("rangeLabel", rangeLabel);

        // 落库月度快照（仅"本月"维度：3m/6m/year 是实时视图，避免快照表口径混杂）
        if (snapshotable) {
            saveReport(userId, period, healthScore, indicators, incomeSources,
                    risks, suggestions, aiSummary, aiEnabled, profileStamp(user), fingerprint);
        }
        return result;
    }

    /** 经统一网关调用 finance_analysis（LLM 失败 Handler 已内部降级；此处失败=查库异常等极端情况） */
    private Map<String, Object> executeViaGateway(Long userId, String range) {
        if (aiGatewayService == null) {
            throw new IllegalStateException("AI 服务未启用，无法生成财务分析");
        }
        AiExecuteRequest req = new AiExecuteRequest();
        req.setSceneCode(SCENE_FINANCE_ANALYSIS);
        req.setUserId(userId);
        Map<String, Object> input = new HashMap<>();
        input.put("userId", userId);
        input.put("range", range);
        req.setInput(input);
        AiExecuteResponse<?> resp = aiGatewayService.execute(req);
        if (resp != null && resp.getCode() != null && resp.getCode() == AiErrorCodes.SUCCESS
                && resp.getData() instanceof Map<?, ?> data) {
            return castMap(data);
        }
        log.warn("AI 财务分析网关未成功 userId={} code={} msg={}",
                userId, resp == null ? null : resp.getCode(), resp == null ? null : resp.getMsg());
        throw new IllegalStateException("财务分析生成失败，请稍后重试");
    }

    @Override
    public Map<String, Object> listReports(Long userId, int page, int pageSize) {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<LedgerAiAnalysisReport> p =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, pageSize);
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<LedgerAiAnalysisReport> r =
                reportMapper.selectPage(p, new LambdaQueryWrapper<LedgerAiAnalysisReport>()
                        .eq(LedgerAiAnalysisReport::getUserId, userId)
                        .orderByDesc(LedgerAiAnalysisReport::getPeriod));
        List<Map<String, Object>> list = new ArrayList<>();
        for (LedgerAiAnalysisReport rep : r.getRecords()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", rep.getId());
            item.put("period", rep.getPeriod());
            item.put("healthScore", rep.getHealthScore());
            item.put("aiSummary", rep.getAiSummary());
            item.put("aiEnabled", rep.getAiEnabled());
            item.put("profileSnapshot", rep.getProfileSnapshot());
            item.put("updateTime", rep.getUpdateTime());
            list.add(item);
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("list", list);
        data.put("total", r.getTotal());
        return data;
    }

    /** 快照实体 → 前端报告结构（缓存命中时使用） */
    private Map<String, Object> reportToResult(LedgerAiAnalysisReport rep, boolean fromCache) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("period", rep.getPeriod());
        result.put("healthScore", rep.getHealthScore());
        result.put("indicators", parseJson(rep.getMetricsJson()));
        result.put("incomeSources", parseJson(rep.getIncomeJson()));
        result.put("debtRisks", parseJson(rep.getRiskJson()));
        result.put("suggestions", parseJson(rep.getAdviceJson()));
        result.put("aiSummary", rep.getAiSummary());
        result.put("aiEnabled", rep.getAiEnabled() != null && rep.getAiEnabled() == 1);
        result.put("fromCache", fromCache);
        return result;
    }

    /** 落库（当月存在则更新；失败仅告警不影响返回） */
    private void saveReport(Long userId, String period, int healthScore,
                            Map<String, Object> indicators, List<Map<String, Object>> incomeSources,
                            List<Map<String, Object>> debtRisks, List<Map<String, Object>> suggestions,
                            String aiSummary, boolean aiEnabled, String profileSnapshot, String fingerprint) {
        try {
            LedgerAiAnalysisReport rep = reportMapper.selectOne(new LambdaQueryWrapper<LedgerAiAnalysisReport>()
                    .eq(LedgerAiAnalysisReport::getUserId, userId)
                    .eq(LedgerAiAnalysisReport::getPeriod, period));
            boolean exists = rep != null;
            if (rep == null) {
                rep = new LedgerAiAnalysisReport();
                rep.setUserId(userId);
                rep.setPeriod(period);
            }
            rep.setHealthScore(healthScore);
            rep.setMetricsJson(MAPPER.writeValueAsString(indicators));
            rep.setIncomeJson(MAPPER.writeValueAsString(incomeSources));
            rep.setRiskJson(MAPPER.writeValueAsString(debtRisks));
            rep.setAdviceJson(MAPPER.writeValueAsString(suggestions));
            rep.setAiSummary(aiSummary);
            rep.setAiEnabled(aiEnabled ? 1 : 0);
            rep.setProfileSnapshot(profileSnapshot);
            rep.setDataFingerprint(fingerprint);
            if (exists) {
                reportMapper.updateById(rep);
            } else {
                reportMapper.insert(rep);
            }
        } catch (Exception e) {
            log.warn("AI 分析报告落库失败 userId={}", userId, e);
        }
    }

    private Object parseJson(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return MAPPER.readValue(json, Object.class);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public Map<String, Object> getProfile(Long userId) {
        PortalUser user = portalUserService.selectPortalUserById(userId);
        Map<String, Object> profile = buildProfile(user);
        List<Map<String, String>> options = new ArrayList<>();
        try {
            for (SysDictData d : dictTypeService.selectDictDataByType(DICT_IDENTITY_TAG)) {
                Map<String, String> opt = new HashMap<>();
                opt.put("value", d.getDictValue());
                opt.put("label", d.getDictLabel());
                options.add(opt);
            }
        } catch (Exception e) {
            log.warn("查询身份标签字典失败", e);
        }
        profile.put("identityOptions", options);
        return profile;
    }

    @Override
    public Map<String, Object> updateProfile(Long userId, Map<String, String> body) {
        PortalUser user = portalUserService.selectPortalUserById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        if (body.containsKey("position")) {
            String position = body.get("position");
            if (position != null && position.length() > 100) {
                throw new IllegalArgumentException("职位长度不能超过100个字符");
            }
            user.setPosition(position);
        }
        if (body.containsKey("company")) {
            String company = body.get("company");
            if (company != null && company.length() > 200) {
                throw new IllegalArgumentException("公司长度不能超过200个字符");
            }
            user.setCompany(company);
        }
        if (body.containsKey("identityTag")) {
            String tag = body.get("identityTag");
            if (tag != null && tag.length() > 32) {
                throw new IllegalArgumentException("身份标签长度不能超过32个字符");
            }
            boolean valid = tag == null || tag.isEmpty();
            if (!valid) {
                valid = dictTypeService.selectDictDataByType(DICT_IDENTITY_TAG).stream()
                        .anyMatch(d -> tag.equals(d.getDictValue()));
            }
            if (!valid) {
                throw new IllegalArgumentException("非法身份标签");
            }
            user.setIdentityTag(tag);
        }
        portalUserMapper.updateById(user);
        return getProfile(userId);
    }

    // ==================== 数据指纹（快照失效判断） ====================

    /**
     * 数据指纹：流水（条数+最后变更时间，近6个月口径与趋势上下文一致）、启用资产/负债账户、
     * 本月预算、画像原始值、finance_analysis 场景配置变更痕迹（cfg）。
     * 任一输入变化自动失效当月快照重算；无变化零 token 命中。
     */
    private String buildFingerprint(Long userId, LocalDate today, PortalUser user) {
        try {
            LocalDate trendStart = today.minusMonths(5).withDayOfMonth(1);
            String tx = aggSignature(transactionMapper.selectMaps(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<LedgerTransaction>()
                            .select("count(*) as cnt", "date_format(max(update_time), '%y%m%d%H%i%s') as last")
                            .eq("user_id", userId).eq("status", 1)
                            .ge("transaction_date", trendStart).le("transaction_date", today)));
            String as = aggSignature(assetAccountMapper.selectMaps(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<LedgerAssetAccount>()
                            .select("count(*) as cnt", "date_format(max(update_time), '%y%m%d%H%i%s') as last")
                            .eq("user_id", userId).eq("status", 1)));
            String li = aggSignature(liabilityAccountMapper.selectMaps(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<LedgerLiabilityAccount>()
                            .select("count(*) as cnt", "date_format(max(update_time), '%y%m%d%H%i%s') as last")
                            .eq("user_id", userId).eq("status", 1)));
            String bd = aggSignature(budgetMapper.selectMaps(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<LedgerBudget>()
                            .select("count(*) as cnt", "date_format(max(update_time), '%y%m%d%H%i%s') as last")
                            .eq("user_id", userId)
                            .eq("year", today.getYear()).eq("month", today.getMonthValue())));
            String fp = "cfg:" + sceneConfigStamp() + "|tx:" + tx + "|as:" + as + "|li:" + li
                    + "|bd:" + bd + "|pf:" + profileStamp(user);
            return fp.length() > 200 ? fp.substring(0, 200) : fp;
        } catch (Exception e) {
            log.warn("数据指纹构建失败（当次不命中缓存，直接重算） userId={}", userId, e);
            return null;
        }
    }

    /** finance_analysis 场景配置变更痕迹（update_time；无配置行返回 na） */
    private String sceneConfigStamp() {
        try {
            List<AiSceneConfig> configs = sceneConfigMapper.selectList(
                    new LambdaQueryWrapper<AiSceneConfig>()
                            .eq(AiSceneConfig::getSceneCode, SCENE_FINANCE_ANALYSIS)
                            .eq(AiSceneConfig::getEnabled, true)
                            .orderByDesc(AiSceneConfig::getPriority)
                            .last("LIMIT 1"));
            if (configs.isEmpty() || configs.get(0).getUpdateTime() == null) {
                return "na";
            }
            return configs.get(0).getUpdateTime()
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyMMddHHmmss"));
        } catch (Exception e) {
            return "na";
        }
    }

    /** 聚合行 → "count@last" 签名（行为空返回 "0@null"） */
    private String aggSignature(List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty() || rows.get(0) == null) {
            return "0@null";
        }
        Object cnt = rows.get(0).get("cnt");
        Object last = rows.get(0).get("last");
        return (cnt == null ? 0 : cnt) + "@" + (last == null ? "null" : last);
    }

    /** 画像原始值戳（快照存档与指纹共用；展示层翻译在 Handler/getProfile） */
    private String profileStamp(PortalUser user) {
        if (user == null) {
            return "未填写";
        }
        StringBuilder sb = new StringBuilder();
        String tagLabel = identityTagLabel(user.getIdentityTag());
        if (tagLabel != null) sb.append(tagLabel);
        if (user.getPosition() != null && !user.getPosition().isEmpty()) {
            if (sb.length() > 0) sb.append(" / ");
            sb.append(user.getPosition());
        }
        if (user.getCompany() != null && !user.getCompany().isEmpty()) {
            if (sb.length() > 0) sb.append(" @ ");
            sb.append(user.getCompany());
        }
        return sb.length() > 0 ? sb.toString() : "未填写";
    }

    private Map<String, Object> buildProfile(PortalUser user) {
        Map<String, Object> p = new LinkedHashMap<>();
        if (user != null) {
            p.put("position", user.getPosition());
            p.put("company", user.getCompany());
            p.put("identityTag", user.getIdentityTag());
            p.put("identityTagLabel", identityTagLabel(user.getIdentityTag()));
        }
        return p;
    }

    private String identityTagLabel(String tag) {
        if (tag == null || tag.isEmpty()) return null;
        try {
            return dictTypeService.selectDictDataByType(DICT_IDENTITY_TAG).stream()
                    .filter(d -> tag.equals(d.getDictValue()))
                    .map(SysDictData::getDictLabel).findFirst().orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    // ==================== 类型工具 ====================

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Map<?, ?> data) {
        return (Map<String, Object>) data;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> castList(Object value) {
        if (!(value instanceof List<?> list)) {
            return new ArrayList<>();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> m) {
                result.add((Map<String, Object>) m);
            }
        }
        return result;
    }
}
