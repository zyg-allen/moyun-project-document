package com.moyun.agent.service.impl;

import com.moyun.agent.entity.ModelConfig;
import com.moyun.agent.service.ModelConfigService;
import com.moyun.agent.service.MultimodalService;
import dev.langchain4j.data.message.*;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.community.model.dashscope.QwenChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.time.Duration;
import java.util.Base64;

/**
 * 多模态服务实现类
 *
 * <p>提供图片理解功能，支持使用多模态大模型（如GPT-4V、通义千问VL）分析图片内容</p>
 *
 * @author laomao
 */
@Slf4j
@Service
public class MultimodalServiceImpl implements MultimodalService {

    @Autowired
    private ModelConfigService modelConfigService;

    @Override
    public String understandImage(String imagePath, String prompt) {
        try {
            File imageFile = new File(imagePath);
            if (!imageFile.exists()) {
                log.error("图片文件不存在: {}", imagePath);
                return "图片文件不存在";
            }

            BufferedImage bufferedImage = ImageIO.read(imageFile);
            if (bufferedImage == null) {
                log.error("无法读取图片: {}", imagePath);
                return "无法读取图片";
            }

            return understandImageInternal(bufferedImage, imagePath, prompt);

        } catch (Exception e) {
            log.error("图片理解失败", e);
            return "图片理解失败: " + e.getMessage();
        }
    }

    @Override
    public String understandImageFromUrl(String imageUrl, String prompt) {
        try {
            ModelConfig config = modelConfigService.getDefaultMultimodalConfig();
            if (config == null) {
                log.warn("未配置默认对话模型，使用简单描述");
                return "图片内容（对话模型未配置）";
            }

            if (prompt == null || prompt.isEmpty()) {
                prompt = getDefaultPrompt();
            }

            ChatLanguageModel model = createMultimodalModel(config);
            if (model == null) {
                log.error("不支持的模型提供商: {}", config.getProvider());
                return "不支持的模型提供商";
            }

            UserMessage userMessage = UserMessage.from(
                TextContent.from(prompt),
                ImageContent.from(imageUrl)
            );

            log.info("调用图片理解模型理解远程图片: {}", imageUrl);
            dev.langchain4j.model.chat.response.ChatResponse response = model.chat(userMessage);
            String description = response.aiMessage().text();
            log.info("图片理解完成，描述长度: {} 字符", description.length());

            return description;

        } catch (Exception e) {
            log.error("远程图片理解失败", e);
            return "图片理解失败: " + e.getMessage();
        }
    }

    @Override
    public String understandImage(BufferedImage image, String prompt) {
        return understandImageInternal(image, null, prompt);
    }

    @Override
    public String batchUnderstandImages(BufferedImage[] images, String prompt) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < images.length; i++) {
            String desc = understandImage(images[i], prompt + " (图片 " + (i + 1) + ")");
            if (desc != null) {
                result.append("图片").append(i + 1).append(": ").append(desc).append("\n\n");
            }
        }
        return result.toString();
    }

    /**
     * 内部方法：统一处理图片理解逻辑
     */
    private String understandImageInternal(BufferedImage image, String originalFilePath, String prompt) {
        try {
            ModelConfig config = modelConfigService.getDefaultMultimodalConfig();
            if (config == null) {
                log.warn("未配置默认对话模型，跳过图片理解");
                return null;
            }

            if (prompt == null || prompt.isEmpty()) {
                prompt = getDefaultPrompt();
            }

            ChatLanguageModel model = createMultimodalModel(config);
            if (model == null) {
                log.warn("不支持的模型提供商: {}", config.getProvider());
                return null;
            }

            UserMessage userMessage = createUserMessageWithImage(image, config, prompt);
            if (userMessage == null) {
                log.warn("无法创建图片消息，跳过多模态理解");
                return null;
            }

            log.info("调用图片理解模型: {} ({})", config.getName(), config.getModelName());
            dev.langchain4j.model.chat.response.ChatResponse response = model.chat(userMessage);
            String description = response.aiMessage().text();
            log.info("图片理解完成: {} 字符", description.length());

            return description;

        } catch (Exception e) {
            log.error("图片理解失败", e);
            return null;
        }
    }

    /**
     * 根据模型类型创建包含图片的UserMessage
     */
    private UserMessage createUserMessageWithImage(BufferedImage image, ModelConfig config, String prompt) {
        try {
            if (image == null) {
                log.error("图片对象为null，无法创建消息");
                return null;
            }

            String provider = config.getProvider().toLowerCase();
            String base64 = imageToBase64(image);
            if (base64 == null || base64.isEmpty()) {
                log.error("Base64编码结果为空");
                return null;
            }

            switch (provider) {
                case "dashscope":
                    ImageContent imageContentQwen = ImageContent.from(base64, "image/jpeg");
                    return UserMessage.from(TextContent.from(prompt), imageContentQwen);

                case "openai":
                    String dataUrl = "data:image/jpeg;base64," + base64;
                    return UserMessage.from(TextContent.from(prompt), ImageContent.from(dataUrl));

                default:
                    log.warn("未知模型提供商 {}，尝试使用base64编码", provider);
                    ImageContent imageContentDefault = ImageContent.from(base64, "image/jpeg");
                    return UserMessage.from(TextContent.from(prompt), imageContentDefault);
            }
        } catch (Exception e) {
            log.error("创建图片消息失败", e);
            return null;
        }
    }

    private String getDefaultPrompt() {
        return "请非常详细地描述这张图片的内容，要求：\n" +
                "1. 如果有标题或主题，请首先说明\n" +
                "2. 详细描述图片的主要内容、对象、场景\n" +
                "3. 如果有文字，请完整提取所有文字内容\n" +
                "4. 如果是图表、架构图、流程图，请说明类型并描述结构和关键信息\n" +
                "5. 如果是截图或界面，请描述界面元素和功能\n" +
                "6. 提取所有可能被用户搜索的关键词\n" +
                "用中文回答，尽可能详细和全面，以便用户通过关键词搜索时能够匹配到这张图片。";
    }

    private ChatLanguageModel createMultimodalModel(ModelConfig config) {
        String provider = config.getProvider().toLowerCase();

        try {
            switch (provider) {
                case "openai":
                    return OpenAiChatModel.builder()
                            .apiKey(config.getApiKey())
                            .baseUrl(config.getBaseUrl())
                            .modelName(config.getModelName())
                            .temperature(config.getTemperature() != null ? config.getTemperature() : 0.3)  // 降低温度提高精确度
                            .maxTokens(config.getMaxTokens() != null ? config.getMaxTokens() : 450)  // 支持150字输出
                            .timeout(Duration.ofSeconds(config.getTimeout() != null ? config.getTimeout() : 120))
                            .logRequests(true)
                            .logResponses(true)
                            .build();

                case "dashscope":
                    return QwenChatModel.builder()
                            .apiKey(config.getApiKey())
                            .modelName(config.getModelName())
                            .temperature(config.getTemperature() != null ? config.getTemperature().floatValue() : 0.3f)  // 降低温度提高精确度
                            .maxTokens(config.getMaxTokens() != null ? config.getMaxTokens() : 450)  // 支持150字输出
                            .build();

                default:
                    log.warn("暂不支持的模型提供商: {}", provider);
                    return null;
            }
        } catch (Exception e) {
            log.error("创建多模态模型失败: {}", e.getMessage(), e);
            return null;
        }
    }

    private String imageToBase64(BufferedImage image) throws Exception {
        if (image == null) {
            throw new IllegalArgumentException("图片对象不能为null");
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        boolean success = ImageIO.write(image, "jpg", baos);

        if (!success) {
            baos.reset();
            success = ImageIO.write(image, "png", baos);
            if (!success) {
                throw new Exception("无法将图片编码为jpg或png格式");
            }
        }

        byte[] imageBytes = baos.toByteArray();
        if (imageBytes.length == 0) {
            throw new Exception("图片编码后字节数组为空");
        }

        return Base64.getEncoder().encodeToString(imageBytes);
    }
}
