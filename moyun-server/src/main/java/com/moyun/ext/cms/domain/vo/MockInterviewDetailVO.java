package com.moyun.ext.cms.domain.vo;

import com.moyun.portal.domain.entity.PortalMockInterview;
import com.moyun.portal.domain.entity.PortalMockInterviewQA;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 模拟面试详情 VO（含问答列表）
 * <p>
 * 继承 PortalMockInterview 获得会话字段（createTime/updateTime 的 @JsonFormat 由 BaseEntity 提供），
 * 额外携带问答列表与已答题数。
 *
 * @author moyun
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MockInterviewDetailVO extends PortalMockInterview {

    private static final long serialVersionUID = 1L;

    /** 该面试的问答列表（按 question_idx 升序） */
    private List<PortalMockInterviewQA> qaList;

    /** 当前用户视角：已答完题数 */
    private Integer answeredCount;
}
