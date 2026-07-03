package com.moyun.ext.cms.domain.query;

import com.moyun.core.base.page.PageDomain;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 话题查询参数
 *
 * @author moyun
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TopicQuery extends PageDomain implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 关键词（名称/描述） */
    private String keyword;

    /** 状态：active/disabled */
    private String status;

    /** 首字母（用于字母索引） */
    private String initial;
}
