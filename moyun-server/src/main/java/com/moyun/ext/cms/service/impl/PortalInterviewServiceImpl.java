package com.moyun.ext.cms.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moyun.common.exception.system.ServiceException;
import com.moyun.core.base.dto.ImportResult;
import com.moyun.ext.cms.domain.vo.UserProfileSnapshotVO;
import com.moyun.ext.cms.service.IUserProfileSnapshotService;
import com.moyun.util.security.SecurityUtils;
import com.moyun.util.string.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moyun.ext.cms.domain.query.InterviewCommentQuery;
import com.moyun.ext.cms.domain.query.InterviewCompanyQuery;
import com.moyun.ext.cms.domain.query.InterviewExperienceQuery;
import com.moyun.ext.cms.domain.query.InterviewQuestionQuery;
import com.moyun.ext.cms.domain.query.InterviewResumeTemplateQuery;
import com.moyun.ext.cms.domain.vo.InterviewBookmarkVO;
import com.moyun.ext.cms.domain.vo.InterviewCategoryVO;
import com.moyun.ext.cms.domain.vo.InterviewCommentVO;
import com.moyun.ext.cms.domain.vo.InterviewCompanyVO;
import com.moyun.ext.cms.domain.vo.InterviewExperienceVO;
import com.moyun.ext.cms.domain.vo.InterviewHomeDataVO;
import com.moyun.ext.cms.domain.vo.InterviewQuestionDetailVO;
import com.moyun.ext.cms.domain.vo.InterviewQuestionVO;
import com.moyun.ext.cms.domain.vo.InterviewResumeTemplateVO;
import com.moyun.ext.cms.domain.vo.InterviewSubmissionVO;
import com.moyun.ext.cms.service.IFeedService;
import com.moyun.system.domain.dto.AuditTaskSubmitDTO;
import com.moyun.system.domain.entity.SysNotification;
import com.moyun.system.service.ISysNotificationService;
import com.moyun.ext.cms.service.IPortalInterviewService;
import com.moyun.system.service.ISensitiveWordService;
import com.moyun.portal.domain.entity.PortalInterviewAttempt;
import com.moyun.portal.domain.entity.PortalInterviewBookmark;
import com.moyun.portal.domain.entity.PortalInterviewCategory;
import com.moyun.portal.domain.entity.PortalInterviewComment;
import com.moyun.portal.domain.entity.PortalInterviewCommentLike;
import com.moyun.portal.domain.entity.PortalInterviewCompany;
import com.moyun.portal.domain.entity.PortalInterviewExperience;
import com.moyun.portal.domain.entity.PortalInterviewExperienceLike;
import com.moyun.portal.domain.entity.PortalInterviewQuestion;
import com.moyun.portal.domain.entity.PortalInterviewQuestionLike;
import com.moyun.portal.domain.entity.PortalInterviewResumeTemplate;
import com.moyun.portal.domain.entity.PortalInterviewResumeTemplateLike;
import com.moyun.portal.domain.entity.PortalInterviewSubmission;
import com.moyun.portal.mapper.PortalInterviewAttemptMapper;
import com.moyun.portal.mapper.PortalInterviewBookmarkMapper;
import com.moyun.portal.mapper.PortalInterviewCategoryMapper;
import com.moyun.portal.mapper.PortalInterviewCommentLikeMapper;
import com.moyun.portal.mapper.PortalInterviewCommentMapper;
import com.moyun.portal.mapper.PortalInterviewCompanyMapper;
import com.moyun.portal.mapper.PortalInterviewExperienceLikeMapper;
import com.moyun.portal.mapper.PortalInterviewExperienceMapper;
import com.moyun.portal.mapper.PortalInterviewQuestionLikeMapper;
import com.moyun.portal.mapper.PortalInterviewQuestionMapper;
import com.moyun.portal.mapper.PortalInterviewResumeTemplateLikeMapper;
import com.moyun.portal.mapper.PortalInterviewResumeTemplateMapper;
import com.moyun.portal.mapper.PortalInterviewSubmissionMapper;
import com.moyun.portal.domain.entity.PortalGrowthLog;
import com.moyun.portal.mapper.PortalGrowthLogMapper;
import com.moyun.portal.service.IPortalGrowthService;
import com.moyun.portal.service.IPortalTagService;

/**
 * 面试模块 Service 实现
 *
 * @author moyun
 */
@Service
public class PortalInterviewServiceImpl implements IPortalInterviewService {

    private static final Logger log = LoggerFactory.getLogger(PortalInterviewServiceImpl.class);

    /** 结构化字段 JSON 解析复用 ObjectMapper（线程安全） */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /** 画像推荐：薄弱点召回上限（避免单路刷屏） */
    private static final int RECO_WEAK_TAG_LIMIT = 3;

    /** 画像推荐：岗位必备技能召回上限 */
    private static final int RECO_REQUIRED_SKILL_LIMIT = 3;

    /** 画像推荐：每个标签/技能召回的题目数 */
    private static final int RECO_PER_TAG_LIMIT = 2;

    @Autowired private PortalInterviewQuestionMapper questionMapper;
    @Autowired private PortalInterviewCategoryMapper categoryMapper;
    @Autowired private PortalInterviewExperienceMapper experienceMapper;
    @Autowired private PortalInterviewResumeTemplateMapper resumeTemplateMapper;
    @Autowired private PortalInterviewResumeTemplateLikeMapper resumeTemplateLikeMapper;
    @Autowired private PortalInterviewSubmissionMapper submissionMapper;
    @Autowired private PortalInterviewBookmarkMapper bookmarkMapper;
    @Autowired private PortalInterviewQuestionLikeMapper questionLikeMapper;
    @Autowired private PortalInterviewAttemptMapper attemptMapper;
    @Autowired private PortalInterviewExperienceLikeMapper experienceLikeMapper;
    @Autowired private PortalInterviewCommentMapper commentMapper;
    @Autowired private PortalInterviewCommentLikeMapper commentLikeMapper;
    @Autowired private PortalInterviewCompanyMapper companyMapper;
    @Autowired private IPortalTagService portalTagService;
    @Autowired private IPortalGrowthService portalGrowthService;
    @Autowired private PortalGrowthLogMapper growthLogMapper;
    @Autowired private com.moyun.portal.mapper.PortalUserMapper portalUserMapper;
    @Autowired private IFeedService feedService;
    @Autowired private ISysNotificationService notificationService;
    @Autowired private IUserProfileSnapshotService profileSnapshotService;
    @Autowired private ISensitiveWordService sensitiveWordService;
    @Autowired @org.springframework.context.annotation.Lazy
    private com.moyun.system.service.IAuditTaskService auditTaskService;

    // ========================================================================
    // 首页聚合
    // ========================================================================
    @Override
    public InterviewHomeDataVO getHomeData(Long currentUserId) {
        InterviewHomeDataVO vo = new InterviewHomeDataVO();

        // 分类列表
        List<PortalInterviewCategory> categoryEntities = categoryMapper.selectList(
                Wrappers.<PortalInterviewCategory>lambdaQuery().eq(PortalInterviewCategory::getStatus, "active").orderByAsc(PortalInterviewCategory::getSort));
        vo.setCategories(categoryEntities.stream().map(this::toCategoryVO).collect(Collectors.toList()));

        // 热门题目（按点赞数 + 提交数排序）
        Page<InterviewQuestionVO> hotQPage = new Page<>(1, 10);
        Page<InterviewQuestionVO> hotQResult = selectQuestionPage(hotQPage, new InterviewQuestionQuery(), currentUserId);
        vo.setHotQuestions(hotQResult.getRecords());

        // 热门面经（按浏览数 + 点赞数）
        InterviewExperienceQuery expQuery = new InterviewExperienceQuery();
        expQuery.setStatus("published");
        Page<InterviewExperienceVO> hotExpPage = new Page<>(1, 5);
        Page<InterviewExperienceVO> hotExpResult = selectExperiencePage(hotExpPage, expQuery, currentUserId);
        vo.setHotExperiences(hotExpResult.getRecords());

        // 简历模板
        InterviewResumeTemplateQuery resumeQuery = new InterviewResumeTemplateQuery();
        resumeQuery.setStatus("active");
        Page<InterviewResumeTemplateVO> resumePage = new Page<>(1, 8);
        Page<InterviewResumeTemplateVO> resumeResult = selectResumeTemplatePage(resumePage, resumeQuery, currentUserId);
        vo.setResumeTemplates(resumeResult.getRecords());

        // 热门公司
        List<PortalInterviewCompany> hotCompanies = companyMapper.selectHotCompanies(10);
        vo.setHotCompanies(hotCompanies.stream().map(this::toCompanyVO).collect(Collectors.toList()));

        // 平台统计
        vo.setTotalQuestionCount((long) questionMapper.selectCount(Wrappers.<PortalInterviewQuestion>lambdaQuery().eq(PortalInterviewQuestion::getStatus, "active")));
        vo.setTotalSubmissionCount(submissionMapper.selectCount(null) == null ? 0L : submissionMapper.selectCount(null).longValue());

        return vo;
    }

    // ========================================================================
    // 分类管理
    // ========================================================================
    @Override
    public List<InterviewCategoryVO> selectCategoryList() {
        List<PortalInterviewCategory> list = categoryMapper.selectList(Wrappers.<PortalInterviewCategory>lambdaQuery().orderByAsc(PortalInterviewCategory::getSort));
        return list.stream().map(this::toCategoryVO).collect(Collectors.toList());
    }

    @Override
    public InterviewCategoryVO selectCategoryById(Long id) {
        PortalInterviewCategory entity = categoryMapper.selectById(id);
        return entity == null ? null : toCategoryVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertCategory(PortalInterviewCategory category) {
        category.setCreateTime(LocalDateTime.now());
        category.setUpdateTime(LocalDateTime.now());
        if (category.getStatus() == null) category.setStatus("active");
        return categoryMapper.insert(category);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateCategory(PortalInterviewCategory category) {
        category.setUpdateTime(LocalDateTime.now());
        return categoryMapper.updateById(category);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteCategoryByIds(Long[] ids) {
        return categoryMapper.deleteBatchIds(Arrays.asList(ids));
    }

    // ========================================================================
    // 题目管理
    // ========================================================================
    @Override
    public Page<InterviewQuestionVO> selectQuestionPage(Page<InterviewQuestionVO> page, InterviewQuestionQuery query, Long currentUserId) {
        LambdaQueryWrapper<PortalInterviewQuestion> qw = buildQuestionQueryWrapper(query);
        Page<PortalInterviewQuestion> entityPage = new Page<>(page.getCurrent(), page.getSize());
        questionMapper.selectPage(entityPage, qw);
        List<InterviewQuestionVO> vos = entityPage.getRecords().stream().map(entity -> toQuestionVO(entity, currentUserId)).collect(Collectors.toList());
        page.setRecords(vos);
        page.setTotal(entityPage.getTotal());
        return page;
    }

    /**
     * 题目分页查询条件构造（与 selectQuestionList 共享，避免重复）
     * - 未传 status 默认查 published（与实体 status 枚举 draft/published/archived 一致；
     *   历史曾用 active/inactive 已废弃，存量数据建议 UPDATE 修正为 published）
     * - 排序：sort 升序 + createTime 降序
     */
    private LambdaQueryWrapper<PortalInterviewQuestion> buildQuestionQueryWrapper(InterviewQuestionQuery query) {
        LambdaQueryWrapper<PortalInterviewQuestion> qw = Wrappers.lambdaQuery();
        qw.eq(PortalInterviewQuestion::getStatus, query.getStatus() == null ? "published" : query.getStatus());
        if (query.getCategoryId() != null) qw.eq(PortalInterviewQuestion::getCategoryId, query.getCategoryId());
        if (StringUtils.isNotEmpty(query.getDifficulty())) qw.eq(PortalInterviewQuestion::getDifficulty, query.getDifficulty());
        // v6.3 题目结构化：按题型筛选
        if (StringUtils.isNotEmpty(query.getQuestionType())) qw.eq(PortalInterviewQuestion::getQuestionType, query.getQuestionType());
        // v10.6 题库重构：按练习模式筛选（reading/choice/coding）
        if (StringUtils.isNotEmpty(query.getPracticeMode())) qw.eq(PortalInterviewQuestion::getPracticeMode, query.getPracticeMode());
        // 关键词需嵌套分组：裸 .or() 会提升优先级，绕过 status/practiceMode 等前置 AND 条件
        if (StringUtils.isNotEmpty(query.getKeyword())) {
            qw.and(w -> w.like(PortalInterviewQuestion::getTitle, query.getKeyword())
                    .or().like(PortalInterviewQuestion::getDescription, query.getKeyword()));
        }
        qw.orderByAsc(PortalInterviewQuestion::getSort).orderByDesc(PortalInterviewQuestion::getCreateTime);
        return qw;
    }

    @Override
    public List<PortalInterviewQuestion> selectQuestionList(InterviewQuestionQuery query) {
        LambdaQueryWrapper<PortalInterviewQuestion> qw = buildQuestionQueryWrapper(query);
        // 导出场景：未传 status 时查全部（与分页列表"默认 published"不同，导出应覆盖草稿/归档）
        if (query.getStatus() == null) {
            qw = Wrappers.lambdaQuery();
            if (query.getCategoryId() != null) qw.eq(PortalInterviewQuestion::getCategoryId, query.getCategoryId());
            if (StringUtils.isNotEmpty(query.getDifficulty())) qw.eq(PortalInterviewQuestion::getDifficulty, query.getDifficulty());
            if (StringUtils.isNotEmpty(query.getQuestionType())) qw.eq(PortalInterviewQuestion::getQuestionType, query.getQuestionType());
            // 与分页列表字段对齐：导出同样支持按练习模式筛选
            if (StringUtils.isNotEmpty(query.getPracticeMode())) qw.eq(PortalInterviewQuestion::getPracticeMode, query.getPracticeMode());
            if (StringUtils.isNotEmpty(query.getKeyword())) {
                qw.and(w -> w.like(PortalInterviewQuestion::getTitle, query.getKeyword())
                        .or().like(PortalInterviewQuestion::getDescription, query.getKeyword()));
            }
            qw.orderByAsc(PortalInterviewQuestion::getSort).orderByDesc(PortalInterviewQuestion::getCreateTime);
        }
        return questionMapper.selectList(qw);
    }

    /**
     * 批量导入题目
     * <p>
     * 校验规则：
     * - title 必填，长度 ≤ 500
     * - difficulty 非空时必须为 easy/medium/hard
     * - questionType 非空时必须为 bagwen/algorithm/system_design/project/hr
     * - status 非空时必须为 draft/published/archived（兼容 active/inactive）
     * <p>
     * 失败行处理：
     * - 保留原始 rowData 便于前端下载失败行 Excel 修正后重导
     * - 行号从 1 开始（1 = 第一条数据行，对应 Excel 第 4 行；表头/说明/示例各占 1 行）
     * <p>
     * 成功行：批量 insert，并同步标签引用计数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ImportResult importQuestions(List<Map<String, String>> rows, String operName) {
        ImportResult result = new ImportResult();
        if (rows == null || rows.isEmpty()) {
            result.setTotalRows(0);
            result.setSuccessCount(0);
            result.setFailCount(0);
            result.setMsg("导入数据为空");
            return result;
        }

        List<ImportResult.FailRow> failRows = new java.util.ArrayList<>();
        int successCount = 0;
        java.time.LocalDateTime now = java.time.LocalDateTime.now();

        // 合法枚举（与 PortalInterviewQuestion 字段注释一致）
        java.util.Set<String> validDifficulty = java.util.Set.of("easy", "medium", "hard");
        java.util.Set<String> validQuestionType = java.util.Set.of("bagwen", "algorithm", "system_design", "project", "hr");
        java.util.Set<String> validStatus = java.util.Set.of("draft", "published", "archived", "active", "inactive");
        java.util.Set<String> validPracticeMode = java.util.Set.of("reading", "choice", "coding");

        for (int i = 0; i < rows.size(); i++) {
            Map<String, String> row = rows.get(i);
            int rowNo = i + 1; // 1=第一条数据行
            try {
                String title = trimToEmpty(row.get("title"));
                if (title.isEmpty()) {
                    throw new IllegalArgumentException("题目标题不能为空");
                }
                if (title.length() > 500) {
                    throw new IllegalArgumentException("题目标题长度超过 500 字");
                }

                String difficulty = trimToEmpty(row.get("difficulty"));
                if (!difficulty.isEmpty() && !validDifficulty.contains(difficulty)) {
                    throw new IllegalArgumentException("难度非法，应为 easy/medium/hard");
                }

                String questionType = trimToEmpty(row.get("questionType"));
                if (!questionType.isEmpty() && !validQuestionType.contains(questionType)) {
                    throw new IllegalArgumentException("题目类型非法，应为 bagwen/algorithm/system_design/project/hr");
                }

                // 练习模式校验（v10.6 新增）
                String practiceMode = trimToEmpty(row.get("practiceMode"));
                if (!practiceMode.isEmpty() && !validPracticeMode.contains(practiceMode)) {
                    throw new IllegalArgumentException("练习模式非法，应为 reading/choice/coding");
                }

                // 选择题选项 JSON 校验（practiceMode=choice 时必填）
                String options = trimToEmpty(row.get("options"));
                String correctAnswer = trimToEmpty(row.get("correctAnswer"));
                if ("choice".equals(practiceMode)) {
                    if (options.isEmpty()) {
                        throw new IllegalArgumentException("练习模式为 choice 时，选择题选项不能为空");
                    }
                    try {
                        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                        mapper.readTree(options); // 校验 JSON 格式
                    } catch (Exception ex) {
                        throw new IllegalArgumentException("选择题选项不是合法的 JSON 格式");
                    }
                }

                String status = trimToEmpty(row.get("status"));
                if (status.isEmpty()) {
                    status = "published"; // 默认发布
                }
                if (!validStatus.contains(status)) {
                    throw new IllegalArgumentException("状态非法，应为 draft/published/archived");
                }

                String tags = trimToEmpty(row.get("tags"));

                PortalInterviewQuestion q = new PortalInterviewQuestion();
                q.setTitle(title);
                q.setDescription(trimToEmpty(row.get("description")));
                q.setDifficulty(difficulty.isEmpty() ? null : difficulty);
                q.setTags(tags);
                q.setCompanies(trimToEmpty(row.get("companies")));
                q.setHint(trimToEmpty(row.get("hint")));
                q.setSolution(trimToEmpty(row.get("solution")));
                q.setReferenceAnswer(trimToEmpty(row.get("referenceAnswer")));
                q.setAnswerOutline(trimToEmpty(row.get("answerOutline")));
                q.setExaminePoints(trimToEmpty(row.get("examinePoints")));
                q.setScoringCriteria(trimToEmpty(row.get("scoringCriteria")));
                q.setPrerequisiteIds(trimToEmpty(row.get("prerequisiteIds")));
                q.setQuestionType(questionType.isEmpty() ? null : questionType);
                // v10.6 新增字段
                q.setPracticeMode(practiceMode.isEmpty() ? "reading" : practiceMode);
                q.setOptions(options.isEmpty() ? null : options);
                q.setCorrectAnswer(correctAnswer.isEmpty() ? null : correctAnswer);
                q.setAnalysis(trimToEmpty(row.get("analysis")));
                q.setKnowledgeTags(trimToEmpty(row.get("knowledgeTags")));
                // 数值类字段：空字符串保持默认，非空才解析
                String sortStr = trimToEmpty(row.get("sort"));
                if (!sortStr.isEmpty()) {
                    try {
                        q.setSort(Integer.parseInt(sortStr));
                    } catch (NumberFormatException ex) {
                        throw new IllegalArgumentException("排序必须为整数");
                    }
                }
                String catStr = trimToEmpty(row.get("categoryId"));
                if (!catStr.isEmpty()) {
                    try {
                        q.setCategoryId(Long.parseLong(catStr));
                    } catch (NumberFormatException ex) {
                        throw new IllegalArgumentException("分类ID必须为数字");
                    }
                }
                q.setStatus(status);
                q.setAcceptanceRate(java.math.BigDecimal.ZERO);
                q.setSubmissionCount(0L);
                q.setLikeCount(0L);
                q.setCreateBy(operName);
                q.setCreateTime(now);
                q.setUpdateBy(operName);
                q.setUpdateTime(now);

                questionMapper.insert(q);
                successCount++;

                // 同步标签引用计数（与 insertQuestion 一致）
                if (!tags.isEmpty()) {
                    List<String> tagNames = new java.util.ArrayList<>();
                    for (String p : tags.split(",")) {
                        if (!p.trim().isEmpty()) tagNames.add(p.trim());
                    }
                    if (!tagNames.isEmpty()) {
                        portalTagService.bindTags("interview_question", q.getId(),
                                java.util.Collections.emptyList(), tagNames, "interview_question");
                    }
                }
            } catch (Exception e) {
                failRows.add(new ImportResult.FailRow(
                        rowNo,
                        e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage(),
                        new java.util.LinkedHashMap<>(row)
                ));
            }
        }

        result.setTotalRows(rows.size());
        result.setSuccessCount(successCount);
        result.setFailCount(failRows.size());
        result.setFailRows(failRows);
        result.setMsg(String.format("共 %d 条，成功 %d 条，失败 %d 条", rows.size(), successCount, failRows.size()));
        return result;
    }

    private static String trimToEmpty(String s) {
        return s == null ? "" : s.trim();
    }

    // ========================================================================
    // 画像推荐题目（v5.9 阶段1：题库页"为你推荐"）
    // 三路召回：薄弱点优先 + 岗位必备技能 + 热门兜底
    // ========================================================================
    @Override
    public List<InterviewQuestionVO> selectRecommendedQuestions(Long currentUserId, int limit) {
        if (currentUserId == null) {
            return Collections.emptyList();
        }
        int target = Math.max(1, Math.min(limit, 12));

        // 1. 构建用户画像快照（position/scene 留空，由岗位字典与答题历史驱动）
        UserProfileSnapshotVO snapshot = null;
        try {
            snapshot = profileSnapshotService.buildSnapshot(currentUserId, null, null);
        } catch (Exception e) {
            log.warn("[Recommend] 构建用户 {} 画像快照失败：{}，降级热门兜底", currentUserId, e.getMessage());
        }

        Set<Long> pickedIds = new LinkedHashSet<>();
        List<InterviewQuestionVO> result = new ArrayList<>();

        // 2. 路径1：薄弱点优先召回（按 failRate 降序）
        if (snapshot != null && snapshot.getWeakTags() != null && !snapshot.getWeakTags().isEmpty()) {
            List<UserProfileSnapshotVO.WeakTagItem> sorted = new ArrayList<>(snapshot.getWeakTags());
            sorted.sort((a, b) -> {
                double fa = a.getFailRate() == null ? 0 : a.getFailRate();
                double fb = b.getFailRate() == null ? 0 : b.getFailRate();
                return Double.compare(fb, fa);
            });
            int remain = Math.min(sorted.size(), RECO_WEAK_TAG_LIMIT);
            for (int i = 0; i < remain && result.size() < target; i++) {
                UserProfileSnapshotVO.WeakTagItem tag = sorted.get(i);
                if (StringUtils.isEmpty(tag.getTagName())) continue;
                int need = Math.min(RECO_PER_TAG_LIMIT, target - result.size());
                List<PortalInterviewQuestion> matched = queryActiveByTag(tag.getTagName(), need);
                mergeUnique(matched, pickedIds, result, currentUserId, "weak_tag", tag.getTagName(), target);
            }
        }

        // 3. 路径2：岗位必备技能召回
        if (snapshot != null && snapshot.getRequiredSkills() != null && !snapshot.getRequiredSkills().isEmpty() && result.size() < target) {
            int remain = Math.min(snapshot.getRequiredSkills().size(), RECO_REQUIRED_SKILL_LIMIT);
            for (int i = 0; i < remain && result.size() < target; i++) {
                String skill = snapshot.getRequiredSkills().get(i);
                if (StringUtils.isEmpty(skill)) continue;
                int need = Math.min(RECO_PER_TAG_LIMIT, target - result.size());
                List<PortalInterviewQuestion> matched = queryActiveByTag(skill, need);
                mergeUnique(matched, pickedIds, result, currentUserId, "required_skill", skill, target);
            }
        }

        // 4. 路径3：热门兜底，按点赞数 + 提交数补齐
        if (result.size() < target) {
            LambdaQueryWrapper<PortalInterviewQuestion> qw = Wrappers.<PortalInterviewQuestion>lambdaQuery()
                    .eq(PortalInterviewQuestion::getStatus, "active");
            if (!pickedIds.isEmpty()) {
                qw.notIn(PortalInterviewQuestion::getId, pickedIds);
            }
            qw.orderByDesc(PortalInterviewQuestion::getLikeCount)
                    .orderByDesc(PortalInterviewQuestion::getSubmissionCount)
                    .orderByDesc(PortalInterviewQuestion::getCreateTime)
                    .last("LIMIT " + Math.max(1, target - result.size()));
            List<PortalInterviewQuestion> hot = questionMapper.selectList(qw);
            mergeUnique(hot, pickedIds, result, currentUserId, "hot", null, target);
        }

        return result;
    }

    /** 按标签精确匹配（LIKE %tag%）查询启用状态的题目，按点赞数倒序 */
    private List<PortalInterviewQuestion> queryActiveByTag(String tag, int limit) {
        if (StringUtils.isEmpty(tag) || limit <= 0) return Collections.emptyList();
        LambdaQueryWrapper<PortalInterviewQuestion> qw = Wrappers.<PortalInterviewQuestion>lambdaQuery()
                .eq(PortalInterviewQuestion::getStatus, "active")
                .like(PortalInterviewQuestion::getTags, tag.trim())
                .orderByDesc(PortalInterviewQuestion::getLikeCount)
                .orderByDesc(PortalInterviewQuestion::getSubmissionCount)
                .last("LIMIT " + Math.max(1, limit));
        return questionMapper.selectList(qw);
    }

    /** 将候选题目去重合并到结果集，并打上推荐来源标记 */
    private void mergeUnique(List<PortalInterviewQuestion> candidates,
                             Set<Long> pickedIds,
                             List<InterviewQuestionVO> result,
                             Long currentUserId,
                             String reason,
                             String tag,
                             int target) {
        if (candidates == null || candidates.isEmpty()) return;
        for (PortalInterviewQuestion q : candidates) {
            if (result.size() >= target) break;
            if (q == null || q.getId() == null) continue;
            if (pickedIds.contains(q.getId())) continue;
            pickedIds.add(q.getId());
            InterviewQuestionVO vo = toQuestionVO(q, currentUserId);
            vo.setRecommendReason(reason);
            vo.setRecommendTag(tag);
            result.add(vo);
        }
    }

    @Override
    public InterviewQuestionDetailVO selectQuestionDetailById(Long id, Long currentUserId) {
        PortalInterviewQuestion entity = questionMapper.selectById(id);
        if (entity == null) throw new ServiceException("题目不存在");
        InterviewQuestionDetailVO vo = new InterviewQuestionDetailVO();
        org.springframework.beans.BeanUtils.copyProperties(toQuestionVO(entity, currentUserId), vo);
        vo.setHint(entity.getHint());
        vo.setSolution(entity.getSolution());

        // v6.3 题目结构化：填充结构化字段（JSON 字符串解析为对象）
        vo.setQuestionType(entity.getQuestionType());
        vo.setExaminePoints(parseStringArray(entity.getExaminePoints()));
        vo.setAnswerOutline(entity.getAnswerOutline());
        vo.setScoringCriteria(parseScoringCriteria(entity.getScoringCriteria()));
        vo.setReferenceAnswer(entity.getReferenceAnswer());
        vo.setPrerequisiteIds(parseLongArray(entity.getPrerequisiteIds()));

        // v10.6 题库重构·阶段2：填充练习模式扩展字段
        vo.setPracticeMode(entity.getPracticeMode());
        vo.setKnowledgeTags(entity.getKnowledgeTags());
        // 阅读定位（v12.0）：详情页即"直接查看模式"，correct_answer/analysis 正常下发，
        // 供学习/复习场景查阅（与 LeetCode 公开题解同理；判分有效性由做题页提交链路保证）。
        vo.setCorrectAnswer(entity.getCorrectAnswer());
        vo.setAnalysis(entity.getAnalysis());
        // options JSON 中的 is_correct 标记脱敏：详情页阅读无需该字段，
        // 避免被用于做题页未提交时的本地比对，做题判分一律以服务端 submitAnswer 返回为准。
        vo.setOptions(sanitizeOptionsForClient(entity.getOptions()));

        // 我的提交记录
        if (currentUserId != null) {
            List<PortalInterviewSubmission> submissions = submissionMapper.selectSubmissionsByQuestionAndUser(id, currentUserId);
            vo.setMySubmissions(submissions.stream().map(this::toSubmissionVO).limit(10).collect(Collectors.toList()));
        }
        // 关联查询通用标签
        if (vo != null && vo.getId() != null) {
            List<com.moyun.portal.domain.vo.TagVO> tagList = portalTagService.getTagsByEntity("interview_question", vo.getId());
            if (tagList != null) vo.setTagList(tagList);
        }
        return vo;
    }

    @Override
    public Map<String, Object> selectQuestionNeighbor(Long questionId, InterviewQuestionQuery query) {
        Map<String, Object> result = new HashMap<>();
        result.put("prevId", null);
        result.put("prevTitle", null);
        result.put("nextId", null);
        result.put("nextTitle", null);
        result.put("currentIndex", null);
        result.put("total", 0);
        if (questionId == null) {
            return result;
        }

        // 查询条件与列表页 buildQuestionQueryWrapper 同源：categoryId/questionType/
        // practiceMode/difficulty/keyword 全支持，排序保持 sort 升序 + createTime 降序，
        // 保证"上一题/下一题"与用户在来源列表页的浏览顺序一致
        if (query == null) {
            query = new InterviewQuestionQuery();
        }
        // 门户导航仅在已发布题目内切换（忽略调用方传入的 status）
        query.setStatus("published");
        LambdaQueryWrapper<PortalInterviewQuestion> qw = buildQuestionQueryWrapper(query);
        qw.select(PortalInterviewQuestion::getId, PortalInterviewQuestion::getTitle,
                PortalInterviewQuestion::getSort, PortalInterviewQuestion::getCreateTime);

        List<PortalInterviewQuestion> list = questionMapper.selectList(qw);
        if (list == null || list.isEmpty()) {
            return result;
        }
        result.put("total", list.size());

        int idx = -1;
        for (int i = 0; i < list.size(); i++) {
            if (questionId.equals(list.get(i).getId())) {
                idx = i;
                break;
            }
        }

        // 当前题不在结果集（未发布 / 筛选条件变化）：不提供导航，避免顺序错乱
        if (idx == -1) {
            return result;
        }
        result.put("currentIndex", idx + 1);
        if (idx > 0) {
            result.put("prevId", list.get(idx - 1).getId());
            result.put("prevTitle", list.get(idx - 1).getTitle());
        }
        if (idx < list.size() - 1) {
            result.put("nextId", list.get(idx + 1).getId());
            result.put("nextTitle", list.get(idx + 1).getTitle());
        }
        return result;
    }

    /**
     * 解析 JSON 字符串数组为 List<String>，失败或空返回空列表
     * 用于 examinePoints 等字段
     */
    private List<String> parseStringArray(String json) {
        if (StringUtils.isEmpty(json)) return Collections.emptyList();
        try {
            List<String> list = OBJECT_MAPPER.readValue(json,
                    OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, String.class));
            return list == null ? Collections.emptyList() : list;
        } catch (Exception e) {
            log.warn("[Question] 解析字符串数组失败，原值将忽略：{}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 解析逗号分隔字符串为 List<Long>，用于 prerequisiteIds
     */
    private List<Long> parseLongArray(String csv) {
        if (StringUtils.isEmpty(csv)) return Collections.emptyList();
        List<Long> result = new ArrayList<>();
        for (String part : csv.split(",")) {
            String trimmed = part == null ? "" : part.trim();
            if (trimmed.isEmpty()) continue;
            try {
                result.add(Long.valueOf(trimmed));
            } catch (NumberFormatException ignored) {
                // 非数字忽略，避免脏数据导致整列解析失败
            }
        }
        return result;
    }

    /**
     * 解析 scoringCriteria JSON 字符串为评分标准列表
     * 兼容两种形态：字符串数组（仅维度名）与对象数组（dimension/weight/description）
     */
    private List<InterviewQuestionDetailVO.ScoringCriterionItem> parseScoringCriteria(String json) {
        if (StringUtils.isEmpty(json)) return Collections.emptyList();
        try {
            List<InterviewQuestionDetailVO.ScoringCriterionItem> list = OBJECT_MAPPER.readValue(json,
                    OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, InterviewQuestionDetailVO.ScoringCriterionItem.class));
            return list == null ? Collections.emptyList() : list;
        } catch (Exception e) {
            // 兼容历史字符串数组形态：尝试按字符串数组解析后降级为仅 dimension
            try {
                List<String> dims = OBJECT_MAPPER.readValue(json,
                        OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, String.class));
                if (dims == null) return Collections.emptyList();
                List<InterviewQuestionDetailVO.ScoringCriterionItem> fallback = new ArrayList<>();
                for (String d : dims) {
                    if (d == null || d.trim().isEmpty()) continue;
                    InterviewQuestionDetailVO.ScoringCriterionItem item = new InterviewQuestionDetailVO.ScoringCriterionItem();
                    item.setDimension(d.trim());
                    fallback.add(item);
                }
                return fallback;
            } catch (Exception ex) {
                log.warn("[Question] 解析评分标准 JSON 失败，将忽略：{}", ex.getMessage());
                return Collections.emptyList();
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertQuestion(PortalInterviewQuestion question) {
        // 服务端兜底校验（与后台表单校验对齐）：选择题必须有选项与正确答案
        validateQuestionByPracticeMode(question);
        question.setCreateTime(LocalDateTime.now());
        question.setUpdateTime(LocalDateTime.now());
        // 状态枚举 draft/published/archived（历史 active/inactive 已废弃，前台默认查 published）
        if (question.getStatus() == null) question.setStatus("published");
        // 练习模式缺省按阅读题处理，保证前台三模式筛选均可命中
        if (question.getPracticeMode() == null || question.getPracticeMode().trim().isEmpty()) {
            question.setPracticeMode("reading");
        }
        if (question.getAcceptanceRate() == null) question.setAcceptanceRate(BigDecimal.ZERO);
        if (question.getSubmissionCount() == null) question.setSubmissionCount(0L);
        if (question.getLikeCount() == null) question.setLikeCount(0L);
        int row = questionMapper.insert(question);
        // 同步绑定通用标签
        java.util.List<Long> extractedTagIds = new java.util.ArrayList<>();
        java.util.List<String> extractedTagNames = new java.util.ArrayList<>();
        if (question != null && question.getTags() != null && !question.getTags().trim().isEmpty()) {
            String[] parts = question.getTags().split(",");
            for (String p : parts) if (p != null && !p.trim().isEmpty()) extractedTagNames.add(p.trim());
        }
        portalTagService.bindTags("interview_question", question.getId(), extractedTagIds, extractedTagNames, "interview_question");
        return row;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateQuestion(PortalInterviewQuestion question) {
        // 更新同样校验：防止把选择题的选项/正确答案清空后造成前台判分失效
        validateQuestionByPracticeMode(question, true);
        question.setUpdateTime(LocalDateTime.now());
        if (question.getPracticeMode() != null && question.getPracticeMode().trim().isEmpty()) {
            question.setPracticeMode(null);
        }
        int row = questionMapper.updateById(question);
        java.util.List<Long> extractedTagIds = new java.util.ArrayList<>();
        java.util.List<String> extractedTagNames = new java.util.ArrayList<>();
        if (question != null && question.getTags() != null && !question.getTags().trim().isEmpty()) {
            String[] parts = question.getTags().split(",");
            for (String p : parts) if (p != null && !p.trim().isEmpty()) extractedTagNames.add(p.trim());
        }
        portalTagService.bindTags("interview_question", question.getId(), extractedTagIds, extractedTagNames, "interview_question");
        return row;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteQuestionByIds(Long[] ids) {
        return questionMapper.deleteBatchIds(Arrays.asList(ids));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InterviewSubmissionVO submitAnswer(Long questionId, Long userId, Map<String, Object> body) {
        PortalInterviewQuestion question = questionMapper.selectById(questionId);
        if (question == null) throw new ServiceException("题目不存在");
        if (userId == null) throw new ServiceException("请登录后提交");

        // 1. 记录提交
        PortalInterviewSubmission submission = new PortalInterviewSubmission();
        submission.setQuestionId(questionId);
        submission.setUserId(userId);
        submission.setCode(body.get("code") != null ? body.get("code").toString() : null);
        submission.setContent(body.get("content") != null ? body.get("content").toString() : null);
        submission.setLanguage(body.get("language") != null ? body.get("language").toString() : "text");
        submission.setAnswerType(body.get("answerType") != null ? body.get("answerType").toString() : "text");
        submission.setNote(body.get("note") != null ? body.get("note").toString() : null);

        // 按练习模式权威判分（行业标准：服务端判定，客户端不可信）
        String practiceMode = question.getPracticeMode() != null ? question.getPracticeMode() : "reading";
        boolean isChoice = "choice".equals(practiceMode);
        boolean isSuccess;
        if (isChoice) {
            // 选择题：服务端比对 correct_answer（单选直接比，多选排序后比）
            String userAnswer = body.get("answer") != null ? body.get("answer").toString() : "";
            String correctAnswer = question.getCorrectAnswer() != null ? question.getCorrectAnswer().trim() : "";
            isSuccess = isChoiceAnswerCorrect(userAnswer, correctAnswer);
            // 选择题答案统一落 content，answerType 标记 choice
            if (submission.getContent() == null || submission.getContent().isEmpty()) {
                submission.setContent(userAnswer);
            }
            if (!"choice".equals(submission.getAnswerType())) {
                submission.setAnswerType("choice");
            }
        } else if ("coding".equals(practiceMode)) {
            // v12.0 双轨合一：编程题统一走 OJ 判题（沙箱运行全部测试用例），
            // 本接口不再受理编程题提交，避免出现"提交即通过"的假判分记录
            throw new ServiceException("编程题请通过在线判题提交，提交后将运行全部测试用例评测");
        } else {
            // 阅读/八股等文本主观题：无法自动判分，提交有效内容即完成作答
            isSuccess = StringUtils.isNotEmpty(submission.getCode()) || StringUtils.isNotEmpty(submission.getContent());
        }
        submission.setIsSuccess(isSuccess);
        submission.setStatus(isSuccess ? "accepted" : "wrong_answer");
        submission.setRuntime(null);
        submission.setMemoryUsage(null);
        submission.setCreateTime(LocalDateTime.now());
        submissionMapper.insert(submission);

        // 2. 更新题目提交数
        question.setSubmissionCount((question.getSubmissionCount() == null ? 0L : question.getSubmissionCount()) + 1);
        long totalSub = submissionMapper.countSubmissionsByQuestion(questionId);
        long successSub = submissionMapper.countSuccessByQuestion(questionId);
        BigDecimal rate = totalSub > 0 ? BigDecimal.valueOf(successSub * 100.0 / totalSub) : BigDecimal.ZERO;
        question.setAcceptanceRate(rate);
        questionMapper.updateById(question);

        // 3. 更新做题记录 + 成长事件（choice/reading 与 OJ 判题共享同一闭环）
        recordAttemptAndGrowth(question, userId, isSuccess, submission.getId());

        InterviewSubmissionVO vo = toSubmissionVO(submission);
        // v9.1：选择题练习模式返回服务端权威判分结果 + 正确答案 + 解析（仅判分后下发，防作弊）
        vo.setPassed(isSuccess);
        if (isChoice) {
            vo.setCorrectAnswer(question.getCorrectAnswer());
            vo.setAnalysis(question.getAnalysis());
            vo.setPracticeMode(question.getPracticeMode());
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void finalizeJudgeResult(Long questionId, Long userId, boolean accepted) {
        PortalInterviewQuestion question = questionMapper.selectById(questionId);
        if (question == null) {
            log.warn("[OJ] 判题终态回调：题目不存在 questionId={}", questionId);
            return;
        }
        if (userId == null) {
            return;
        }
        // 编程题判题终态：与选择题共享「做题记录 + 首次通过成长事件 + 答题动态」闭环
        recordAttemptAndGrowth(question, userId, accepted, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> recordQuestionRead(Long questionId, Long userId, Map<String, Object> body) {
        PortalInterviewQuestion question = questionMapper.selectById(questionId);
        if (question == null) throw new ServiceException("题目不存在");
        if (userId == null) throw new ServiceException("登录后才能记录学习行为");

        Map<String, Object> result = new HashMap<>();

        // 1. 阅读事件幂等：同用户 + 同题目 + 同一天只记一次
        boolean readRecorded = false;
        Long existToday = growthLogMapper.selectCount(
                new LambdaQueryWrapper<PortalGrowthLog>()
                        .eq(PortalGrowthLog::getUserId, userId)
                        .eq(PortalGrowthLog::getModule, "interview")
                        .eq(PortalGrowthLog::getAction, "read_question")
                        .eq(PortalGrowthLog::getEntityType, "question")
                        .eq(PortalGrowthLog::getEntityId, questionId)
                        .ge(PortalGrowthLog::getCreateTime, LocalDateTime.now().toLocalDate().atStartOfDay()));
        if (existToday == null || existToday == 0) {
            portalGrowthService.recordEvent("interview", "read_question",
                    userId, "question", questionId);
            readRecorded = true;
        }
        result.put("readRecorded", readRecorded);

        // 2. 附带笔记：落一条阅读提交（answerType=reading，不计数入题目提交数），并记写笔记成长事件
        boolean noteRecorded = false;
        String note = body != null && body.get("note") != null ? body.get("note").toString().trim() : null;
        if (StringUtils.isNotEmpty(note)) {
            PortalInterviewSubmission submission = new PortalInterviewSubmission();
            submission.setQuestionId(questionId);
            submission.setUserId(userId);
            submission.setAnswerType("reading");
            submission.setLanguage("text");
            submission.setNote(note);
            submission.setContent(note);
            submission.setIsSuccess(true);
            submission.setStatus("accepted");
            submission.setCreateTime(LocalDateTime.now());
            submissionMapper.insert(submission);
            // entityType=question：成长时间线点击可跳转题目详情（submission 类型无对应路由）
            portalGrowthService.recordEvent("interview", "write_note",
                    userId, "question", questionId);
            noteRecorded = true;
        }
        result.put("noteRecorded", noteRecorded);
        return result;
    }

    /**
     * 做题记录与成长闭环（choice 提交 / reading 提交 / OJ 判题终态 共用）
     * <p>
     * - 更新 portal_interview_attempt（attempt_count / status / 首次通过时间）
     * - 首次通过时发 solve_question 成长事件（成长时间线可见）
     * - 首次通过时发答题动态（Feed 流），失败静默不影响主流程
     *
     * @param question     题目实体（用于标题/难度等动态字段）
     * @param userId       做题用户
     * @param isSuccess    本次是否通过
     * @param submissionId 关联提交记录ID（OJ 判题回调场景可为 null）
     */
    private void recordAttemptAndGrowth(PortalInterviewQuestion question, Long userId,
                                         boolean isSuccess, Long submissionId) {
        Long questionId = question.getId();
        PortalInterviewAttempt attempt = attemptMapper.selectAttempt(questionId, userId);
        boolean firstSolve = false;
        if (attempt == null) {
            attempt = new PortalInterviewAttempt();
            attempt.setQuestionId(questionId);
            attempt.setUserId(userId);
            attempt.setAttemptCount(1);
            attempt.setStatus(isSuccess ? "solved" : "attempted");
            attempt.setLastAttemptAt(LocalDateTime.now());
            if (isSuccess) {
                attempt.setFirstSolvedAt(LocalDateTime.now());
                attempt.setLastSolvedAt(LocalDateTime.now());
                firstSolve = true;
            }
            attemptMapper.insert(attempt);
        } else {
            attempt.setAttemptCount(attempt.getAttemptCount() + 1);
            attempt.setLastAttemptAt(LocalDateTime.now());
            if (isSuccess && "attempted".equals(attempt.getStatus())) {
                attempt.setStatus("solved");
                attempt.setFirstSolvedAt(LocalDateTime.now());
                attempt.setLastSolvedAt(LocalDateTime.now());
                firstSolve = true;
            } else if (isSuccess) {
                attempt.setLastSolvedAt(LocalDateTime.now());
            }
            attemptMapper.updateById(attempt);
        }

        // 首次通过：成长事件 + 答题动态
        if (firstSolve) {
            portalGrowthService.recordEvent("interview", "solve_question",
                    userId, "question", questionId);

            // 发布动态事件（Feed 流），失败不影响主流程
            try {
                feedService.publishEvent(userId, "solve_question", "question",
                        questionId, question.getTitle(),
                        question.getDifficulty(), null);
            } catch (Exception e) {
                log.error("[Feed] 答题动态事件失败：questionId={}", questionId, e);
            }
        }
        // 提交包含笔记时，记录写笔记成长事件（判题回调无笔记场景）
        if (submissionId != null) {
            PortalInterviewSubmission submission = submissionMapper.selectById(submissionId);
            if (submission != null && StringUtils.isNotEmpty(submission.getNote())) {
                // entityType=question：成长时间线点击可跳转题目详情（submission 类型无对应路由）
                portalGrowthService.recordEvent("interview", "write_note",
                        userId, "question", questionId);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> toggleQuestionLike(Long questionId, Long userId) {
        if (userId == null) throw new ServiceException("请登录后操作");
        PortalInterviewQuestionLike exist = questionLikeMapper.selectLike(questionId, userId);
        PortalInterviewQuestion question = questionMapper.selectById(questionId);
        if (question == null) throw new ServiceException("题目不存在");

        Map<String, Object> result = new HashMap<>();
        if (exist != null) {
            // 取消点赞
            questionLikeMapper.deleteById(exist.getId());
            // 原子减少点赞数（不低于0，避免并发丢失更新）
            questionMapper.incrementLikes(questionId, -1);
            question.setLikeCount(Math.max(0L, (question.getLikeCount() == null ? 0L : question.getLikeCount()) - 1));
            result.put("liked", false);
        } else {
            // 新增点赞
            PortalInterviewQuestionLike like = new PortalInterviewQuestionLike();
            like.setQuestionId(questionId);
            like.setUserId(userId);
            like.setCreateTime(LocalDateTime.now());
            questionLikeMapper.insert(like);
            // 原子增加点赞数（避免并发丢失更新）
            questionMapper.incrementLikes(questionId, 1);
            question.setLikeCount((question.getLikeCount() == null ? 0L : question.getLikeCount()) + 1);
            result.put("liked", true);
        }
        result.put("likeCount", question.getLikeCount());
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> toggleQuestionBookmark(Long questionId, Long userId, String note) {
        if (userId == null) throw new ServiceException("请登录后操作");
        PortalInterviewBookmark exist = bookmarkMapper.selectBookmark(questionId, userId);
        Map<String, Object> result = new HashMap<>();
        if (exist != null) {
            bookmarkMapper.deleteById(exist.getId());
            result.put("bookmarked", false);
        } else {
            PortalInterviewBookmark bm = new PortalInterviewBookmark();
            bm.setQuestionId(questionId);
            bm.setUserId(userId);
            bm.setNote(note);
            bm.setCreateTime(LocalDateTime.now());
            bookmarkMapper.insert(bm);
            result.put("bookmarked", true);
        }
        return result;
    }

    @Override
    public Page<InterviewBookmarkVO> selectBookmarkPage(Page<InterviewBookmarkVO> page, Long userId) {
        if (userId == null) { page.setRecords(Collections.emptyList()); return page; }
        List<PortalInterviewBookmark> list = bookmarkMapper.selectBookmarkListByUserId(userId);
        List<InterviewBookmarkVO> vos = list.stream().map(entity -> {
            InterviewBookmarkVO vo = new InterviewBookmarkVO();
            vo.setId(entity.getId());
            vo.setQuestionId(entity.getQuestionId());
            vo.setUserId(entity.getUserId());
            vo.setNote(entity.getNote());
            vo.setCreateTime(entity.getCreateTime());
            PortalInterviewQuestion q = questionMapper.selectById(entity.getQuestionId());
            if (q != null) vo.setQuestion(toQuestionVO(q, userId));
            return vo;
        }).skip((long) (int)((page.getCurrent() - 1) * page.getSize())).limit((int) page.getSize()).collect(Collectors.toList());
        page.setRecords(vos);
        page.setTotal((long) list.size());
        return page;
    }

    @Override
    public Page<InterviewSubmissionVO> selectMySubmissionList(Page<InterviewSubmissionVO> page, Long userId) {
        if (userId == null) {
            page.setRecords(Collections.emptyList());
            return page;
        }
        List<PortalInterviewSubmission> list = submissionMapper.selectSubmissionsByUserId(userId);
        List<InterviewSubmissionVO> vos = list.stream().map(entity -> {
            InterviewSubmissionVO vo = new InterviewSubmissionVO();
            vo.setId(entity.getId());
            vo.setQuestionId(entity.getQuestionId());
            vo.setUserId(entity.getUserId());
            vo.setCode(entity.getCode());
            vo.setContent(entity.getContent());
            vo.setLanguage(entity.getLanguage());
            vo.setAnswerType(entity.getAnswerType());
            vo.setStatus(entity.getStatus());
            vo.setIsSuccess(entity.getIsSuccess());
            vo.setRuntime(entity.getRuntime());
            vo.setMemoryUsage(entity.getMemoryUsage());
            vo.setNote(entity.getNote());
            vo.setIsFeatured(entity.getIsFeatured());
            vo.setFeaturedTime(entity.getFeaturedTime());
            vo.setCreateTime(entity.getCreateTime());
            // 填充题目标题和难度
            PortalInterviewQuestion q = questionMapper.selectById(entity.getQuestionId());
            if (q != null) {
                vo.setQuestionTitle(q.getTitle());
                vo.setQuestionDifficulty(q.getDifficulty());
            }
            return vo;
        }).skip((long) (int) ((page.getCurrent() - 1) * page.getSize())).limit((int) page.getSize()).collect(Collectors.toList());
        page.setRecords(vos);
        page.setTotal((long) list.size());
        return page;
    }

    @Override
    public Page<InterviewExperienceVO> selectMyExperienceList(Page<InterviewExperienceVO> page, InterviewExperienceQuery query, Long userId) {
        // 不复用 selectExperiencePage：公开列表对 null status 默认只查 published，
        // 会把草稿/待审核过滤掉（v10.10 修复"保存草稿后列表消失"问题）。
        // 我的面经默认可见所有状态（含 draft/pending/rejected），并支持按状态筛选。
        LambdaQueryWrapper<PortalInterviewExperience> qw = Wrappers.lambdaQuery();
        if (query != null) {
            if (StringUtils.isNotEmpty(query.getStatus())) {
                qw.eq(PortalInterviewExperience::getStatus, query.getStatus());
            }
            if (StringUtils.isNotEmpty(query.getKeyword())) {
                qw.and(w -> w.like(PortalInterviewExperience::getTitle, query.getKeyword())
                        .or().like(PortalInterviewExperience::getContent, query.getKeyword()));
            }
            if (StringUtils.isNotEmpty(query.getCompany())) {
                qw.like(PortalInterviewExperience::getCompany, query.getCompany());
            }
            if (query.getYear() != null) {
                qw.eq(PortalInterviewExperience::getYear, query.getYear());
            }
        }
        qw.eq(PortalInterviewExperience::getUserId, userId);
        qw.orderByDesc(PortalInterviewExperience::getIsTop).orderByDesc(PortalInterviewExperience::getCreateTime);
        Page<PortalInterviewExperience> entityPage = new Page<>(page.getCurrent(), page.getSize());
        experienceMapper.selectPage(entityPage, qw);
        List<InterviewExperienceVO> vos = entityPage.getRecords().stream()
                .map(e -> toExperienceVO(e, userId)).collect(Collectors.toList());
        page.setRecords(vos);
        page.setTotal(entityPage.getTotal());
        return page;
    }

    /**
     * 后台采纳/取消采纳提交笔记为精选
     * 采纳时为提交者记录 note_adopted 成长事件
     */
    @Override
    public Map<String, Object> adoptSubmission(Long submissionId, boolean isFeatured) {
        PortalInterviewSubmission submission = submissionMapper.selectById(submissionId);
        if (submission == null) throw new ServiceException("提交记录不存在");
        if (submission.getNote() == null || submission.getNote().trim().isEmpty()) {
            throw new ServiceException("该提交无笔记内容，无法采纳");
        }

        // 查询原状态，判断是否是首次采纳
        boolean wasFeatured = Boolean.TRUE.equals(submission.getIsFeatured());

        // 原子更新精选状态
        int rows = submissionMapper.updateFeatured(submissionId, isFeatured);

        Map<String, Object> result = new HashMap<>();
        result.put("affected", rows);
        result.put("isFeatured", isFeatured);

        // 仅在"未采纳 → 采纳"时记录成长事件，避免重复
        if (rows > 0 && isFeatured && !wasFeatured && submission.getUserId() != null) {
            portalGrowthService.recordEvent("interview", "note_adopted",
                    submission.getUserId(), "submission", submissionId);
        }

        result.put("message", isFeatured ? "已采纳为精选笔记" : "已取消精选");
        return result;
    }

    /**
     * 查询某题目的精选笔记列表
     */
    @Override
    public List<InterviewSubmissionVO> selectFeaturedSubmissions(Long questionId) {
        List<PortalInterviewSubmission> list = submissionMapper.selectFeaturedByQuestion(questionId);
        List<InterviewSubmissionVO> result = list.stream().map(this::toSubmissionVO).collect(Collectors.toList());
        // 填充提交者昵称/头像
        for (InterviewSubmissionVO vo : result) {
            if (vo.getUserId() != null) {
                try {
                    com.moyun.portal.domain.entity.PortalUser user = portalUserMapper.selectPortalUserById(vo.getUserId());
                    if (user != null) {
                        vo.setUserNickname(user.getNickname() != null ? user.getNickname() : user.getUsername());
                        vo.setUserAvatar(user.getAvatar());
                    }
                } catch (Exception e) { /* ignore */ }
            }
        }
        return result;
    }

    // ========================================================================
    // 面经管理
    // ========================================================================
    @Override
    public Page<InterviewExperienceVO> selectExperiencePage(Page<InterviewExperienceVO> page, InterviewExperienceQuery query, Long currentUserId) {
        LambdaQueryWrapper<PortalInterviewExperience> qw = Wrappers.lambdaQuery();
        qw.eq(PortalInterviewExperience::getStatus, query.getStatus() == null ? "published" : query.getStatus());
        if (StringUtils.isNotEmpty(query.getKeyword())) {
            // and() 包裹 OR 条件，避免 or() 打断外层 status 过滤导致草稿/待审核泄露到公开搜索
            qw.and(w -> w.like(PortalInterviewExperience::getTitle, query.getKeyword())
                    .or().like(PortalInterviewExperience::getContent, query.getKeyword()));
        }
        if (StringUtils.isNotEmpty(query.getCompany())) qw.like(PortalInterviewExperience::getCompany, query.getCompany());
        if (query.getYear() != null) qw.eq(PortalInterviewExperience::getYear, query.getYear());
        if (query.getUserId() != null) qw.eq(PortalInterviewExperience::getUserId, query.getUserId());
        qw.orderByDesc(PortalInterviewExperience::getIsTop).orderByDesc(PortalInterviewExperience::getCreateTime);

        Page<PortalInterviewExperience> entityPage = new Page<>(page.getCurrent(), page.getSize());
        experienceMapper.selectPage(entityPage, qw);
        List<InterviewExperienceVO> vos = entityPage.getRecords().stream().map(e -> toExperienceVO(e, currentUserId)).collect(Collectors.toList());
        page.setRecords(vos);
        page.setTotal(entityPage.getTotal());
        return page;
    }

    @Override
    public InterviewExperienceVO selectExperienceDetailById(Long id, Long currentUserId) {
        PortalInterviewExperience entity = experienceMapper.selectById(id);
        if (entity == null) throw new ServiceException("面经不存在");
        // 原子更新浏览数
        experienceMapper.incrementViewCount(id);
        entity.setViewCount((entity.getViewCount() == null ? 0L : entity.getViewCount()) + 1);
        InterviewExperienceVO vo = toExperienceVO(entity, currentUserId);
        if (vo != null && vo.getId() != null) {
            List<com.moyun.portal.domain.vo.TagVO> tagList = portalTagService.getTagsByEntity("interview_experience", vo.getId());
            if (tagList != null) vo.setTagList(tagList);
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertExperience(PortalInterviewExperience experience, Long userId) {
        if (userId == null) {
            throw new ServiceException("请先登录");
        }
        // v10.10 实名策略：发布面经不再强制创作者认证（未实名也可发布），
        // 由前端弹窗提示实名（可跳过），仅打赏/积分消费等敏感场景强制实名。
        experience.setStatus(experience.getStatus() == null ? "pending" : experience.getStatus());
        experience.setUserId(userId);
        experience.setCreateTime(LocalDateTime.now());
        experience.setUpdateTime(LocalDateTime.now());
        if (experience.getViewCount() == null) experience.setViewCount(0L);
        if (experience.getLikeCount() == null) experience.setLikeCount(0L);
        if (experience.getCommentCount() == null) experience.setCommentCount(0L);
        int row = experienceMapper.insert(experience);
        // 同步绑定通用标签
        java.util.List<Long> extractedTagIds = new java.util.ArrayList<>();
        java.util.List<String> extractedTagNames = new java.util.ArrayList<>();
        if (experience != null && experience.getTags() != null && !experience.getTags().trim().isEmpty()) {
            String[] parts = experience.getTags().split(",");
            for (String p : parts) if (p != null && !p.trim().isEmpty()) extractedTagNames.add(p.trim());
        }
        portalTagService.bindTags("interview_experience", experience.getId(), extractedTagIds, extractedTagNames, "interview_experience");

        // v8.1：进入待审核态时，提交统一审核任务（写 sys_audit_task），使首页/审核中心待办可见
        if (row > 0 && "pending".equals(experience.getStatus())) {
            submitAuditTask("interview_exp", experience.getId(), experience.getTitle(),
                    experience.getSummary(), userId);
        }

        // 记录发布面经成长事件（仅"提交发布"触发，草稿不计入成长与 Feed）
        if (row > 0 && userId != null && "pending".equals(experience.getStatus())) {
            portalGrowthService.recordEvent("interview", "publish_experience",
                    userId, "experience", experience.getId());

            // 发布动态事件（Feed 流）。try-catch 包裹，避免 Feed 失败影响面经发布主流程
            try {
                feedService.publishEvent(userId, "publish_experience", "experience",
                        experience.getId(), experience.getTitle(),
                        experience.getSummary(), experience.getCoverImage());
            } catch (Exception e) {
                org.slf4j.LoggerFactory.getLogger(PortalInterviewServiceImpl.class)
                        .error("[Feed] 面经发布动态事件失败：experienceId={}", experience.getId(), e);
            }
        }

        return row;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateExperience(PortalInterviewExperience experience, Long userId) {
        if (userId == null) {
            throw new ServiceException("请先登录");
        }
        PortalInterviewExperience db = experienceMapper.selectById(experience.getId());
        if (db == null) throw new ServiceException("面经不存在");
        if (!db.getUserId().equals(userId)) throw new ServiceException("无权修改他人的面经");
        // v10.10 实名策略：发布不再强制创作者认证（前端弹窗提示可跳过）
        // 判断是否为"提交发布"：新状态为 pending 且原状态不是 pending（草稿/被拒 → 发布）。
        // 已是 pending 的编辑不重复提交审核任务，避免重复待办。
        boolean submitForReview = experience.getStatus() != null
                && "pending".equals(experience.getStatus())
                && !"pending".equals(db.getStatus());
        experience.setUpdateTime(LocalDateTime.now());
        int row = experienceMapper.updateById(experience);
        java.util.List<Long> extractedTagIds = new java.util.ArrayList<>();
        java.util.List<String> extractedTagNames = new java.util.ArrayList<>();
        if (experience != null && experience.getTags() != null && !experience.getTags().trim().isEmpty()) {
            String[] parts = experience.getTags().split(",");
            for (String p : parts) if (p != null && !p.trim().isEmpty()) extractedTagNames.add(p.trim());
        }
        portalTagService.bindTags("interview_experience", experience.getId(), extractedTagIds, extractedTagNames, "interview_experience");

        // v10.10 修复链路断裂：草稿/被拒面经通过编辑"提交发布"时，
        // 此前未提交审核任务，导致面经永远停在 pending 且审核中心不可见。
        if (row > 0 && submitForReview) {
            submitAuditTask("interview_exp", experience.getId(), experience.getTitle(),
                    experience.getSummary(), userId);
            // 与 insertExperience 发布路径对齐：记录成长事件 + Feed 动态
            portalGrowthService.recordEvent("interview", "publish_experience",
                    userId, "experience", experience.getId());
            try {
                feedService.publishEvent(userId, "publish_experience", "experience",
                        experience.getId(), experience.getTitle(),
                        experience.getSummary(), experience.getCoverImage());
            } catch (Exception e) {
                org.slf4j.LoggerFactory.getLogger(PortalInterviewServiceImpl.class)
                        .error("[Feed] 面经发布动态事件失败：experienceId={}", experience.getId(), e);
            }
        }
        return row;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteExperienceById(Long id, Long userId) {
        PortalInterviewExperience db = experienceMapper.selectById(id);
        if (db == null) return 0;
        if (!db.getUserId().equals(userId)) throw new ServiceException("无权删除他人的面经");
        return experienceMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int auditExperience(Long id, String status, String remark) {
        // 1. 状态白名单校验：仅允许 published / rejected
        if (!"published".equals(status) && !"rejected".equals(status)) {
            throw new ServiceException("审核状态非法，仅允许 published 或 rejected");
        }
        // 2. 驳回时必填审核意见
        if ("rejected".equals(status) && StringUtils.isEmpty(remark)) {
            throw new ServiceException("驳回必须填写审核意见");
        }
        // 3. 存在性校验
        PortalInterviewExperience existing = experienceMapper.selectById(id);
        if (existing == null) {
            throw new ServiceException("面经不存在");
        }
        // 4. 乐观锁 + 写入审核轨迹（对齐文章审核模式：仅 pending 可审核）
        LambdaUpdateWrapper<PortalInterviewExperience> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(PortalInterviewExperience::getId, id)
                .eq(PortalInterviewExperience::getStatus, "pending")
                .set(PortalInterviewExperience::getStatus, status)
                .set(PortalInterviewExperience::getAuditorId, SecurityUtils.getUserId())
                .set(PortalInterviewExperience::getAuditTime, LocalDateTime.now())
                .set(PortalInterviewExperience::getUpdateTime, LocalDateTime.now());
        if (StringUtils.isNotEmpty(remark)) {
            wrapper.set(PortalInterviewExperience::getAuditRemark, remark);
        }
        int rows = experienceMapper.update(null, wrapper);
        if (rows == 0) {
            // 乐观锁失败：非 pending 状态（已被他人审核过），给出明确提示
            PortalInterviewExperience current = experienceMapper.selectById(id);
            if (current == null) {
                throw new ServiceException("面经不存在或已被删除");
            }
            throw new ServiceException("当前面经状态为「" + current.getStatus()
                    + "」，仅待审核状态（pending）可执行审核操作");
        }
        // 5. 审核结果通知作者（非阻塞，失败不影响主流程）
        try {
            if (existing.getUserId() != null) {
                String noticeContent = "您的面经《" +
                        (existing.getTitle() != null && existing.getTitle().length() > 30
                                ? existing.getTitle().substring(0, 30) + "…"
                                : existing.getTitle()) + "》" +
                        ("published".equals(status) ? "审核通过" : "审核未通过") +
                        (StringUtils.isNotEmpty(remark) ? "，原因：" + remark : "");
                try {
                    SysNotification notification = new SysNotification();
                    notification.setType("notice");
                    notification.setTitle("面经审核结果");
                    notification.setContent(noticeContent);
                    notification.setScope("user");
                    notification.setUserId(existing.getUserId());
                    notification.setUserType("portal");
                    notification.setStatus("0");
                    notificationService.save(notification);
                } catch (Throwable ignored) {
                    log.debug("面经审核站内信接口不可用，已跳过: experienceId={}", id);
                }
            }
        } catch (Exception e) {
            log.warn("面经审核站内信发送失败，experienceId={}: {}", id, e.getMessage());
        }
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int topExperience(Long id, Boolean isTop) {
        PortalInterviewExperience entity = experienceMapper.selectById(id);
        if (entity == null) throw new ServiceException("面经不存在");
        entity.setIsTop(isTop);
        entity.setUpdateTime(LocalDateTime.now());
        return experienceMapper.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> toggleExperienceLike(Long experienceId, Long userId) {
        if (userId == null) throw new ServiceException("请登录后操作");
        PortalInterviewExperienceLike exist = experienceLikeMapper.selectLike(experienceId, userId);
        PortalInterviewExperience exp = experienceMapper.selectById(experienceId);
        if (exp == null) throw new ServiceException("面经不存在");
        Map<String, Object> result = new HashMap<>();
        if (exist != null) {
            experienceLikeMapper.deleteById(exist.getId());
            // 原子减少点赞数（不低于0，避免并发丢失更新）
            experienceMapper.incrementLikes(experienceId, -1);
            exp.setLikeCount(Math.max(0L, (exp.getLikeCount() == null ? 0L : exp.getLikeCount()) - 1));
            result.put("liked", false);
        } else {
            PortalInterviewExperienceLike like = new PortalInterviewExperienceLike();
            like.setExperienceId(experienceId);
            like.setUserId(userId);
            like.setCreateTime(LocalDateTime.now());
            experienceLikeMapper.insert(like);
            // 原子增加点赞数（避免并发丢失更新）
            experienceMapper.incrementLikes(experienceId, 1);
            exp.setLikeCount((exp.getLikeCount() == null ? 0L : exp.getLikeCount()) + 1);
            result.put("liked", true);

            // 为面经作者记录被赞成长事件
            if (exp.getUserId() != null && !exp.getUserId().equals(userId)) {
                portalGrowthService.recordEventWithTarget("interview", "experience_liked",
                        exp.getUserId(), userId, "experience", experienceId);
            }
        }
        result.put("likeCount", exp.getLikeCount());
        return result;
    }

    // ========================================================================
    // 评论管理
    // ========================================================================
    @Override
    public Page<InterviewCommentVO> selectCommentPage(Page<InterviewCommentVO> page, InterviewCommentQuery query, Long currentUserId) {
        LambdaQueryWrapper<PortalInterviewComment> qw = Wrappers.lambdaQuery();
        qw.eq(PortalInterviewComment::getStatus, query.getStatus() == null ? "published" : query.getStatus());
        if (query.getExperienceId() != null) qw.eq(PortalInterviewComment::getExperienceId, query.getExperienceId());
        if (query.getUserId() != null) qw.eq(PortalInterviewComment::getUserId, query.getUserId());
        if (StringUtils.isNotEmpty(query.getKeyword())) qw.like(PortalInterviewComment::getContent, query.getKeyword());
        qw.isNull(PortalInterviewComment::getParentId).orderByDesc(PortalInterviewComment::getCreateTime);

        Page<PortalInterviewComment> entityPage = new Page<>(page.getCurrent(), page.getSize());
        commentMapper.selectPage(entityPage, qw);
        List<InterviewCommentVO> vos = entityPage.getRecords().stream().map(e -> toCommentVO(e, currentUserId)).collect(Collectors.toList());
        page.setRecords(vos);
        page.setTotal(entityPage.getTotal());
        return page;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertComment(PortalInterviewComment comment, Long userId) {
        if (userId == null) throw new ServiceException("请登录后操作");
        PortalInterviewExperience exp = experienceMapper.selectById(comment.getExperienceId());
        if (exp == null) throw new ServiceException("面经不存在");
        comment.setUserId(userId);
        // 敏感词检查：命中即拦截，写审计日志（与文章评论发布侧策略一致）
        if (comment.getContent() != null && !comment.getContent().isEmpty()) {
            try {
                List<String> hits = sensitiveWordService.find(comment.getContent());
                if (hits != null && !hits.isEmpty()) {
                    sensitiveWordService.detectAndLog(
                            "interview_comment", null, userId,
                            comment.getContent(), "block");
                    log.warn("面经评论命中敏感词已拦截：experienceId={}, userId={}, hits={}",
                            comment.getExperienceId(), userId, hits);
                    throw new ServiceException("评论内容包含违规信息，请修改后重试");
                }
            } catch (ServiceException e) {
                throw e;
            } catch (Exception e) {
                log.warn("面经评论敏感词扫描异常：experienceId={}, err={}",
                        comment.getExperienceId(), e.getMessage());
            }
        }
        comment.setStatus(comment.getStatus() == null ? "published" : comment.getStatus());
        if (comment.getLikeCount() == null) comment.setLikeCount(0L);
        comment.setCreateTime(LocalDateTime.now());
        comment.setUpdateTime(LocalDateTime.now());
        int row = commentMapper.insert(comment);
        // 更新面经评论数
        experienceMapper.incrementCommentCount(comment.getExperienceId());
        // v8.1：评论进入待审核态时提交统一审核任务（默认 published 不进审核）
        if (row > 0 && "pending".equals(comment.getStatus())) {
            submitAuditTask("interview_comment", comment.getId(), null,
                    comment.getContent(), userId);
        }
        return row;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteCommentById(Long id, Long userId) {
        PortalInterviewComment comment = commentMapper.selectById(id);
        if (comment == null) return 0;
        if (!comment.getUserId().equals(userId)) throw new ServiceException("无权删除他人的评论");
        return commentMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int auditComment(Long id, String status, String remark) {
        PortalInterviewComment entity = commentMapper.selectById(id);
        if (entity == null) throw new ServiceException("评论不存在");
        // 状态白名单：仅允许 published / rejected
        if (!"published".equals(status) && !"rejected".equals(status)) {
            throw new ServiceException("非法的审核状态：" + status);
        }
        // 驳回必须填写原因
        if ("rejected".equals(status) && (remark == null || remark.trim().isEmpty())) {
            throw new ServiceException("驳回必须填写原因");
        }
        // 乐观锁：仅 pending 状态可审核，避免并发重复审核
        LambdaUpdateWrapper<PortalInterviewComment> uw = Wrappers.<PortalInterviewComment>lambdaUpdate()
                .eq(PortalInterviewComment::getId, id)
                .eq(PortalInterviewComment::getStatus, "pending")
                .set(PortalInterviewComment::getStatus, status)
                .set(PortalInterviewComment::getAuditRemark, remark)
                .set(PortalInterviewComment::getAuditTime, LocalDateTime.now())
                .set(PortalInterviewComment::getUpdateTime, LocalDateTime.now());
        int rows = commentMapper.update(null, uw);
        if (rows == 0) {
            throw new ServiceException("审核失败：评论状态已变更或不存在待审核记录");
        }
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> toggleCommentLike(Long commentId, Long userId) {
        if (userId == null) throw new ServiceException("请登录后操作");
        PortalInterviewCommentLike exist = commentLikeMapper.selectLike(commentId, userId);
        PortalInterviewComment comment = commentMapper.selectById(commentId);
        if (comment == null) throw new ServiceException("评论不存在");
        Map<String, Object> result = new HashMap<>();
        if (exist != null) {
            commentLikeMapper.deleteById(exist.getId());
            // 原子减少点赞数（不低于0，避免并发丢失更新）
            commentMapper.incrementLikes(commentId, -1);
            comment.setLikeCount(Math.max(0L, (comment.getLikeCount() == null ? 0L : comment.getLikeCount()) - 1));
            result.put("liked", false);
        } else {
            PortalInterviewCommentLike like = new PortalInterviewCommentLike();
            like.setCommentId(commentId);
            like.setUserId(userId);
            like.setCreateTime(LocalDateTime.now());
            commentLikeMapper.insert(like);
            // 原子增加点赞数（避免并发丢失更新）
            commentMapper.incrementLikes(commentId, 1);
            comment.setLikeCount((comment.getLikeCount() == null ? 0L : comment.getLikeCount()) + 1);
            result.put("liked", true);
        }
        result.put("likeCount", comment.getLikeCount());
        return result;
    }

    // ========================================================================
    // 简历模板管理
    // ========================================================================
    @Override
    public Page<InterviewResumeTemplateVO> selectResumeTemplatePage(Page<InterviewResumeTemplateVO> page, InterviewResumeTemplateQuery query, Long currentUserId) {
        LambdaQueryWrapper<PortalInterviewResumeTemplate> qw = Wrappers.lambdaQuery();
        qw.eq(PortalInterviewResumeTemplate::getStatus, query.getStatus() == null ? "" : query.getStatus());
        if (StringUtils.isNotEmpty(query.getCategory())) qw.eq(PortalInterviewResumeTemplate::getCategory, query.getCategory());
        if (StringUtils.isNotEmpty(query.getFileType())) qw.eq(PortalInterviewResumeTemplate::getFileType, query.getFileType());
        if (query.getIsPremium() != null) qw.eq(PortalInterviewResumeTemplate::getIsPremium, query.getIsPremium());
        if (StringUtils.isNotEmpty(query.getKeyword())) {
            qw.like(PortalInterviewResumeTemplate::getTitle, query.getKeyword()).or().like(PortalInterviewResumeTemplate::getDescription, query.getKeyword());
        }
        qw.orderByAsc(PortalInterviewResumeTemplate::getSort).orderByDesc(PortalInterviewResumeTemplate::getCreateTime);

        Page<PortalInterviewResumeTemplate> entityPage = new Page<>(page.getCurrent(), page.getSize());
        resumeTemplateMapper.selectPage(entityPage, qw);
        List<InterviewResumeTemplateVO> vos = entityPage.getRecords().stream().map(entity -> toResumeTemplateVO(entity, currentUserId)).collect(Collectors.toList());
        page.setRecords(vos);
        page.setTotal(entityPage.getTotal());
        return page;
    }

    @Override
    public InterviewResumeTemplateVO selectResumeTemplateById(Long id) {
        PortalInterviewResumeTemplate entity = resumeTemplateMapper.selectById(id);
        InterviewResumeTemplateVO vo = entity == null ? null : toResumeTemplateVO(entity, null);
        if (vo != null && vo.getId() != null) {
            List<com.moyun.portal.domain.vo.TagVO> tagList = portalTagService.getTagsByEntity("interview_resume_template", vo.getId());
            if (tagList != null) vo.setTagList(tagList);
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InterviewResumeTemplateVO downloadResumeTemplate(Long id) {
        // 先查询模板
        InterviewResumeTemplateVO vo = selectResumeTemplateById(id);
        if (vo == null) {
            return null;
        }
        // 原子递增下载次数，避免并发丢失更新
        resumeTemplateMapper.incrementDownloadCount(id);
        // 回填最新计数
        vo.setDownloadCount((vo.getDownloadCount() == null ? 0L : vo.getDownloadCount()) + 1);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertResumeTemplate(PortalInterviewResumeTemplate template) {
        template.setCreateTime(LocalDateTime.now());
        template.setUpdateTime(LocalDateTime.now());
        if (template.getStatus() == null) template.setStatus("active");
        if (template.getDownloadCount() == null) template.setDownloadCount(0L);
        if (template.getLikeCount() == null) template.setLikeCount(0L);
        int row = resumeTemplateMapper.insert(template);
        // 同步绑定通用标签（当前 entity 无 tags 字段，可由前端通过 /portal/tag/bind 单独绑定）
        java.util.List<Long> extractedTagIds = new java.util.ArrayList<>();
        java.util.List<String> extractedTagNames = new java.util.ArrayList<>();
        portalTagService.bindTags("interview_resume_template", template.getId(), extractedTagIds, extractedTagNames, "interview_resume_template");
        return row;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateResumeTemplate(PortalInterviewResumeTemplate template) {
        template.setUpdateTime(LocalDateTime.now());
        int row = resumeTemplateMapper.updateById(template);
        java.util.List<Long> extractedTagIds = new java.util.ArrayList<>();
        java.util.List<String> extractedTagNames = new java.util.ArrayList<>();
        portalTagService.bindTags("interview_resume_template", template.getId(), extractedTagIds, extractedTagNames, "interview_resume_template");
        return row;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteResumeTemplateByIds(Long[] ids) {
        return resumeTemplateMapper.deleteBatchIds(Arrays.asList(ids));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> toggleResumeTemplateLike(Long templateId, Long userId) {
        if (userId == null) throw new ServiceException("请登录后操作");
        PortalInterviewResumeTemplate template = resumeTemplateMapper.selectById(templateId);
        if (template == null) throw new ServiceException("模板不存在");
        Map<String, Object> result = new HashMap<>();

        // 查询是否已点赞（防重）
        PortalInterviewResumeTemplateLike exist = resumeTemplateLikeMapper.selectLike(templateId, userId);
        if (exist != null) {
            // 已点赞 → 取消点赞
            resumeTemplateLikeMapper.deleteById(exist.getId());
            // 原子减少点赞数（不低于0，避免并发丢失更新）
            resumeTemplateMapper.incrementLikes(templateId, -1);
            template.setLikeCount(Math.max(0L, (template.getLikeCount() == null ? 0L : template.getLikeCount()) - 1));
            result.put("liked", false);
        } else {
            // 未点赞 → 点赞
            PortalInterviewResumeTemplateLike like = new PortalInterviewResumeTemplateLike();
            like.setTemplateId(templateId);
            like.setUserId(userId);
            like.setCreateTime(LocalDateTime.now());
            resumeTemplateLikeMapper.insert(like);
            // 原子增加点赞数（避免并发丢失更新）
            resumeTemplateMapper.incrementLikes(templateId, 1);
            template.setLikeCount((template.getLikeCount() == null ? 0L : template.getLikeCount()) + 1);
            result.put("liked", true);
        }
        result.put("likeCount", template.getLikeCount());
        return result;
    }

    // ========================================================================
    // 公司标签管理
    // ========================================================================
    @Override
    public List<InterviewCompanyVO> selectCompanyList(InterviewCompanyQuery query) {
        LambdaQueryWrapper<PortalInterviewCompany> qw = Wrappers.lambdaQuery();
        qw.eq(PortalInterviewCompany::getStatus, query.getStatus() == null ? "active" : query.getStatus());
        if (StringUtils.isNotEmpty(query.getIndustry())) qw.eq(PortalInterviewCompany::getIndustry, query.getIndustry());
        if (StringUtils.isNotEmpty(query.getKeyword())) {
            qw.like(PortalInterviewCompany::getName, query.getKeyword()).or().like(PortalInterviewCompany::getDescription, query.getKeyword());
        }
        qw.orderByAsc(PortalInterviewCompany::getSort).orderByDesc(PortalInterviewCompany::getQuestionCount);
        List<PortalInterviewCompany> list = companyMapper.selectList(qw);
        return list.stream().map(this::toCompanyVO).collect(Collectors.toList());
    }

    @Override
    public InterviewCompanyVO selectCompanyById(Long id) {
        PortalInterviewCompany entity = companyMapper.selectById(id);
        return entity == null ? null : toCompanyVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertCompany(PortalInterviewCompany company) {
        company.setCreateTime(LocalDateTime.now());
        company.setUpdateTime(LocalDateTime.now());
        if (company.getStatus() == null) company.setStatus("active");
        return companyMapper.insert(company);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateCompany(PortalInterviewCompany company) {
        company.setUpdateTime(LocalDateTime.now());
        return companyMapper.updateById(company);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteCompanyByIds(Long[] ids) {
        return companyMapper.deleteBatchIds(Arrays.asList(ids));
    }

    // ========================================================================
    // Entity -> VO 转换辅助
    // ========================================================================
    private InterviewCategoryVO toCategoryVO(PortalInterviewCategory entity) {
        InterviewCategoryVO vo = new InterviewCategoryVO();
        org.springframework.beans.BeanUtils.copyProperties(entity, vo);
        vo.setId(entity.getId());
        return vo;
    }

    private InterviewCompanyVO toCompanyVO(PortalInterviewCompany entity) {
        InterviewCompanyVO vo = new InterviewCompanyVO();
        org.springframework.beans.BeanUtils.copyProperties(entity, vo);
        vo.setId(entity.getId());
        return vo;
    }

    private InterviewQuestionVO toQuestionVO(PortalInterviewQuestion entity, Long currentUserId) {
        InterviewQuestionVO vo = new InterviewQuestionVO();
        org.springframework.beans.BeanUtils.copyProperties(entity, vo);
        vo.setId(entity.getId());
        // 标签字符串切分
        if (StringUtils.isNotEmpty(entity.getTags())) {
            vo.setTags(Arrays.asList(entity.getTags().split(",")).stream().map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList()));
        }
        // 关联公司（查询 portal_interview_question_company + portal_interview_company）
        List<PortalInterviewCompany> companies = companyMapper.selectCompaniesByQuestionId(entity.getId());
        vo.setCompanies(companies.stream().map(this::toCompanyVO).collect(Collectors.toList()));
        // 登录态关联信息
        if (currentUserId != null) {
            vo.setLiked(questionLikeMapper.selectLike(entity.getId(), currentUserId) != null);
            vo.setBookmarked(bookmarkMapper.selectBookmark(entity.getId(), currentUserId) != null);
            PortalInterviewAttempt attempt = attemptMapper.selectAttempt(entity.getId(), currentUserId);
            vo.setAttemptStatus(attempt == null ? "not_attempted" : attempt.getStatus());
        } else {
            vo.setLiked(false);
            vo.setBookmarked(false);
            vo.setAttemptStatus("not_attempted");
        }
        return vo;
    }

    private InterviewExperienceVO toExperienceVO(PortalInterviewExperience entity, Long currentUserId) {
        InterviewExperienceVO vo = new InterviewExperienceVO();
        org.springframework.beans.BeanUtils.copyProperties(entity, vo);
        vo.setId(entity.getId());
        if (StringUtils.isNotEmpty(entity.getTags())) {
            vo.setTags(Arrays.asList(entity.getTags().split(",")).stream().map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList()));
        }
        // 作者信息（这里省略用户名查询，可结合 sys_user 表或 portal_user 表）
        vo.setUserId(entity.getUserId());
        if (currentUserId != null) {
            vo.setLiked(experienceLikeMapper.selectLike(entity.getId(), currentUserId) != null);
        } else {
            vo.setLiked(false);
        }
        return vo;
    }

    private InterviewResumeTemplateVO toResumeTemplateVO(PortalInterviewResumeTemplate entity, Long currentUserId) {
        InterviewResumeTemplateVO vo = new InterviewResumeTemplateVO();
        org.springframework.beans.BeanUtils.copyProperties(entity, vo);
        vo.setId(entity.getId());
        return vo;
    }

    private InterviewCommentVO toCommentVO(PortalInterviewComment entity, Long currentUserId) {
        InterviewCommentVO vo = new InterviewCommentVO();
        org.springframework.beans.BeanUtils.copyProperties(entity, vo);
        vo.setId(entity.getId());
        if (currentUserId != null) {
            vo.setLiked(commentLikeMapper.selectLike(entity.getId(), currentUserId) != null);
        } else {
            vo.setLiked(false);
        }
        // 子评论（一级递归即可）
        List<PortalInterviewComment> children = commentMapper.selectCommentsByParentId(entity.getId());
        if (children != null && !children.isEmpty()) {
            vo.setReplies(children.stream().map(c -> toCommentVO(c, currentUserId)).collect(Collectors.toList()));
        }
        return vo;
    }

    private InterviewSubmissionVO toSubmissionVO(PortalInterviewSubmission entity) {
        InterviewSubmissionVO vo = new InterviewSubmissionVO();
        org.springframework.beans.BeanUtils.copyProperties(entity, vo);
        vo.setId(entity.getId());
        return vo;
    }

    /**
     * v8.1：提交统一审核任务到 sys_audit_task（事务内，异常回滚保证双写一致）。
     *
     * @param taskType    任务类型（interview_exp / interview_comment）
     * @param bizId       业务记录ID
     * @param title       任务标题（可空，空则由 Service 用类型名+ID 兜底）
     * @param description 任务描述/摘要
     * @param submitterId 提交人（门户用户ID）
     */
    private void submitAuditTask(String taskType, Long bizId, String title,
                                 String description, Long submitterId) {
        AuditTaskSubmitDTO dto = new AuditTaskSubmitDTO();
        dto.setTaskType(taskType);
        dto.setBizId(bizId);
        dto.setTitle(title);
        dto.setDescription(description);
        dto.setSubmitterId(submitterId);
        if (submitterId != null) {
            try {
                com.moyun.portal.domain.entity.PortalUser u = portalUserMapper.selectPortalUserById(submitterId);
                if (u != null) {
                    dto.setSubmitterName(u.getUsername());
                }
            } catch (Exception e) {
                log.warn("[AuditTask] 查询提交人用户名失败 submitterId={} err={}", submitterId, e.getMessage());
            }
        }
        auditTaskService.submit(dto);
    }

    /**
     * 选择题服务端权威判分：归一化比对用户答案与正确答案。
     * 支持多选（逗号/空格分隔，忽略顺序与大小写），如 "A,C" 与 "c,a" 视为一致。
     */
    private boolean isChoiceAnswerCorrect(String userAnswer, String correctAnswer) {
        if (correctAnswer == null || correctAnswer.trim().isEmpty()) {
            return false;
        }
        if (userAnswer == null || userAnswer.trim().isEmpty()) {
            return false;
        }
        Set<String> expected = normalizeChoiceAnswer(correctAnswer);
        Set<String> actual = normalizeChoiceAnswer(userAnswer);
        return expected.equals(actual);
    }

    /**
     * 将选择题答案归一化为选项字母集合：去除空白、统一大写、按非字母字符切分。
     */
    private Set<String> normalizeChoiceAnswer(String answer) {
        Set<String> result = new java.util.TreeSet<>();
        for (String token : answer.trim().toUpperCase().split("[^A-Z]+")) {
            if (!token.isEmpty()) {
                result.add(token);
            }
        }
        return result;
    }

    /**
     * 按练习模式校验判分材料完整性（与后台表单校验对齐，服务端兜底）。
     * isUpdate=true 的部分更新场景：未携带 practiceMode 时跳过（判分材料以库内已有数据为准）。
     */
    private void validateQuestionByPracticeMode(PortalInterviewQuestion question) {
        validateQuestionByPracticeMode(question, false);
    }

    private void validateQuestionByPracticeMode(PortalInterviewQuestion question, boolean isUpdate) {
        String mode = question.getPracticeMode() == null ? null : question.getPracticeMode().trim();
        if (mode == null || mode.isEmpty()) {
            if (isUpdate) return;
            mode = "reading";
        }
        if (!"reading".equals(mode) && !"choice".equals(mode) && !"coding".equals(mode)) {
            throw new ServiceException("练习模式非法：仅支持 reading/choice/coding");
        }
        if (!"choice".equals(mode)) {
            return;
        }
        if (StringUtils.isEmpty(question.getOptions())) {
            throw new ServiceException("选择题必须配置选项");
        }
        if (StringUtils.isEmpty(question.getCorrectAnswer())) {
            throw new ServiceException("选择题必须配置正确答案");
        }
        // options 结构与正确答案字母交叉校验：防脏数据导致前台判分永远失败
        try {
            com.fasterxml.jackson.databind.JsonNode arr = OBJECT_MAPPER.readTree(question.getOptions());
            if (arr == null || !arr.isArray() || arr.size() < 2) {
                throw new ServiceException("选择题至少需要 2 个选项");
            }
            java.util.Set<String> labels = new java.util.HashSet<>();
            for (com.fasterxml.jackson.databind.JsonNode item : arr) {
                if (item != null && item.isObject() && item.has("label")) {
                    labels.add(item.get("label").asText().toUpperCase());
                }
            }
            Set<String> answers = normalizeChoiceAnswer(question.getCorrectAnswer());
            if (answers.isEmpty() || !labels.containsAll(answers)) {
                throw new ServiceException("正确答案 " + question.getCorrectAnswer()
                        + " 引用了未配置的选项，请检查选项字母");
            }
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException("选择题选项 JSON 格式非法，请检查后重试");
        }
    }

    /**
     * 下发给前端的选项 JSON 脱敏：剥离 is_correct 标记，防止答案泄露。
     * 入参为空或非法 JSON 时原样返回（由前端兜底解析）。
     */
    private String sanitizeOptionsForClient(String optionsJson) {
        if (optionsJson == null || optionsJson.trim().isEmpty()) {
            return optionsJson;
        }
        try {
            com.fasterxml.jackson.databind.JsonNode arr = OBJECT_MAPPER.readTree(optionsJson);
            if (arr == null || !arr.isArray()) {
                return optionsJson;
            }
            com.fasterxml.jackson.databind.node.ArrayNode safe = OBJECT_MAPPER.createArrayNode();
            for (com.fasterxml.jackson.databind.JsonNode item : arr) {
                if (item != null && item.isObject()) {
                    com.fasterxml.jackson.databind.node.ObjectNode node = item.deepCopy();
                    node.remove("is_correct");
                    node.remove("isCorrect");
                    safe.add(node);
                }
            }
            return safe.toString();
        } catch (Exception e) {
            return optionsJson;
        }
    }
}
