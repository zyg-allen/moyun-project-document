package com.moyun.ext.cms.domain.vo;

import lombok.Data;

/**
 * 个性化推荐统一结果项（任务 4.8）
 *
 * <p>用于统一承载文章 / 题目 / 书籍 / 创作者四类推荐结果。
 * score 为推荐得分（越高越相关），由内容匹配数或协同过滤共现数得出。</p>
 *
 * @author moyun
 */
@Data
public class RecommendItemVO {

    /** 对象ID */
    private Long id;

    /** 类型 article/question/book/creator */
    private String type;

    /** 标题 */
    private String title;

    /** 封面 */
    private String cover;

    /** 摘要 / 简介 */
    private String excerpt;

    /** 推荐得分（越高越相关） */
    private Double score;

    /** 作者名（文章作者昵称 / 书籍作者 / 创作者昵称） */
    private String authorName;

    /** 文章作者ID */
    private Long authorId;

    /** 作者/创作者头像 */
    private String authorAvatar;

    /** 标签（逗号分隔，题目/书籍可用） */
    private String tags;

    /** 扩展信息（题目的难度等） */
    private String extra;
}
