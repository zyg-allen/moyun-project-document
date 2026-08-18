package com.moyun.portal.domain.vo;

import lombok.Data;

/**
 * 语音面试 SSE 事件对象（V10.1）
 *
 * <p>submitAnswer 接口返回 SseEmitter，通过事件类型区分：
 * <ul>
 *   <li>event:score   规则分（立即返回）</li>
 *   <li>event:speak   LLM 流式话术分片（如有）</li>
 *   <li>event:data    完整数据 JSON（评分+反馈+nextAction）</li>
 *   <li>event:end     结束信号</li>
 *   <li>event:error   异常</li>
 * </ul>
 *
 * @author moyun
 */
@Data
public class VoiceSseEvent {

    /** 事件类型 score/speak/data/end/error */
    private String event;

    /** 事件数据（JSON 字符串或纯文本） */
    private String data;

    public static VoiceSseEvent of(String event, String data) {
        VoiceSseEvent e = new VoiceSseEvent();
        e.setEvent(event);
        e.setData(data);
        return e;
    }
}
