package com.moyun.ext.aigateway.handler.impl;

import com.moyun.ext.ai.entity.AiSceneConfig;
import com.moyun.ext.ai.enums.AiSceneEnum;
import com.moyun.ext.aigateway.constant.AiErrorCodes;
import com.moyun.ext.aigateway.handler.AbstractAiSceneHandler;
import com.moyun.ext.aigateway.model.AiExecuteRequest;
import com.moyun.ext.aigateway.model.AiExecuteResponse;
import com.moyun.ext.aigateway.model.data.GenericSceneData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 内容标签提取场景Handler（scene = content_tags）
 *
 * <p>从 PortalAiController 自造 SCENES 收编。输入：title + content（纯文本）。
 * 输出：tags 标签列表（3~8 个）。</p>
 *
 * @author laomao
 * @since 2026-09-18
 */
@Slf4j
@Component("contentTagsHandler")
public class ContentTagsHandler extends AbstractAiSceneHandler {

    private static final int MAX_CONTENT_CHARS = 3000;

    @Override
    public String getSceneCode() {
        return AiSceneEnum.CONTENT_TAGS.getCode();
    }

    @Override
    public void validate(AiExecuteRequest request) {
        requireInputString(request, "content");
    }

    @Override
    public AiExecuteResponse<?> execute(AiExecuteRequest request, AiSceneConfig config) {
        String title = getInputString(request, "title");
        String content = requireInputString(request, "content");

        String systemPrompt = "你是内容平台的编辑。请根据下面的内容提取 3~8 个最贴切的主题标签。\n"
                + "严格只输出一行，标签之间用英文逗号分隔，不要带序号和其他文字。示例：职场,成长,方法论";

        StringBuilder user = new StringBuilder();
        user.append("标题：").append(title != null ? title : "（无标题）").append("\n");
        user.append("内容：\n").append(clip(content, MAX_CONTENT_CHARS));

        String raw = chat(getSceneCode(), systemPrompt, user.toString());
        if (raw == null || raw.isBlank()) {
            return AiExecuteResponse.failure(AiErrorCodes.AI_CALL_FAILED, "AI服务暂不可用");
        }

        String line = raw.trim().split("\n")[0].replaceAll("[\"'\"\"]", "");
        List<String> tags = Arrays.stream(line.split("[,，]"))
                .map(String::trim)
                .filter(t -> !t.isEmpty() && t.length() <= 20)
                .limit(8)
                .toList();

        Map<String, Object> result = new HashMap<>();
        result.put("tags", tags);

        GenericSceneData data = new GenericSceneData();
        data.setStructured(result);
        return AiExecuteResponse.success(data);
    }

    private String clip(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max);
    }
}
