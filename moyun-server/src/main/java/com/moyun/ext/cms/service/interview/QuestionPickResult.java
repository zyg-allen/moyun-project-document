package com.moyun.ext.cms.service.interview;

import com.moyun.portal.domain.entity.PortalInterviewQuestion;
import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 智能出题结果（v11.x）
 *
 * <p>沿用锚定题协议：简历项目锚定题为虚拟实体（id=null），
 * 题面通过 snapshots 持久化到 configJson.questionSnapshots（索引对齐）。</p>
 *
 * @author moyun
 */
@Data
public class QuestionPickResult {

    /** 题单（锚定题为虚拟实体，id=null） */
    private List<PortalInterviewQuestion> questions = new ArrayList<>();

    /** 索引 → 题面快照（锚定题协议，与旧 questionSnapshots 格式兼容） */
    private Map<Integer, Map<String, Object>> snapshots = new LinkedHashMap<>();

    /** 各题源实际命中数量（job/resume/weak/random/resume_project），供日志与出题分布观测 */
    private Map<String, Integer> sources = new LinkedHashMap<>();

    public void recordSource(String source) {
        sources.merge(source, 1, Integer::sum);
    }
}