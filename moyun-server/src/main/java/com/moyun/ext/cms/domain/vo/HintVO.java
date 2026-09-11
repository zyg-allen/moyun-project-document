package com.moyun.ext.cms.domain.vo;

import lombok.Data;

import java.util.List;

/**
 * 面试提示引擎返回对象
 *
 * <p>分级别提示，level 越高提示越详细：
 * <ul>
 *   <li>level=1：切入点提示（1~2 个关键词，引导思考方向）</li>
 *   <li>level=2：结构提示（STAR 框架 + 答题大纲，引导组织语言）</li>
 *   <li>level=3：全量提示（全部关键词 + 考察点 + 结构提示，不给完整答案）</li>
 * </ul>
 *
 * @author moyun
 */
@Data
public class HintVO {

    /** 提示级别 1~3 */
    private int level;

    /** 提示标题（如"切入点提示"/"结构提示"/"全量提示"） */
    private String title;

    /** 关键词列表 */
    private List<String> keywords;

    /** 结构提示文本（STAR 框架 / 答题大纲） */
    private String structureHint;

    /** 考察点列表（level≥3 时返回） */
    private List<String> examinePoints;

    /** 话术提示（可直接 TTS 播报的引导语） */
    private String speakText;

    public static HintVO of(int level, String title) {
        HintVO vo = new HintVO();
        vo.setLevel(level);
        vo.setTitle(title);
        return vo;
    }
}
