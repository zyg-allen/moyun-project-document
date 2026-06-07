package com.moyun.util.file;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.StrUtil;
import com.moyun.common.config.RuoYiConfig;
import com.moyun.core.config.ServerConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Base64 图片处理工具类
 *
 * @author moyun
 */
@Slf4j
@Component
public class Base64ImageUtils {

    @Autowired
    private MinioUtils minioUtils;

    @Autowired
    private ServerConfig serverConfig;

    /**
     * Base64图片正则
     */
    private static final Pattern BASE64_IMAGE_PATTERN = Pattern.compile("data:image/(\\w+);base64,([\\s\\S]*)");

    /**
     * 判断是否是Base64图片
     */
    public static boolean isBase64Image(String content) {
        if (StrUtil.isBlank(content)) {
            return false;
        }
        return BASE64_IMAGE_PATTERN.matcher(content).find();
    }

    /**
     * 上传单张Base64图片
     *
     * @param base64Image Base64图片
     * @return 图片URL
     */
    public String uploadBase64Image(String base64Image) {
        if (StrUtil.isBlank(base64Image)) {
            return null;
        }

        Matcher matcher = BASE64_IMAGE_PATTERN.matcher(base64Image);
        if (!matcher.find()) {
            return base64Image;
        }

        try {
            String type = matcher.group(1);
            String base64Data = matcher.group(2);
            byte[] imageBytes = Base64.decode(base64Data);

            // 根据type获取后缀
            String suffix = getImageSuffix(type);

            // 上传到MinIO
            if (minioUtils.isEnabled()) {
                return minioUtils.uploadBytes(imageBytes, "image/" + type, suffix);
            } else {
                // 本地存储
                return uploadToLocal(imageBytes, suffix);
            }
        } catch (Exception e) {
            log.error("Base64图片处理失败", e);
            return base64Image;
        }
    }

    /**
     * 上传到本地存储
     */
    private String uploadToLocal(byte[] imageBytes, String suffix) throws Exception {
        String baseDir = RuoYiConfig.getProfile();
        String datePath = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String fileName = datePath + "/" + UUID.randomUUID().toString().replace("-", "") + "." + suffix;
        String filePath = baseDir + File.separator + fileName;

        // 确保目录存在
        File destFile = new File(filePath);
        if (!destFile.getParentFile().exists()) {
            destFile.getParentFile().mkdirs();
        }

        // 写入文件
        try (OutputStream os = new FileOutputStream(destFile)) {
            os.write(imageBytes);
        }

        // 返回访问URL
        return serverConfig.getUrl() + "/profile/" + fileName;
    }

    /**
     * 批量替换富文本/Markdown中的Base64图片
     *
     * @param content 富文本/Markdown
     * @return 处理后的内容
     */
    public String processContentImages(String content) {
        if (StrUtil.isBlank(content)) {
            return content;
        }

        // 处理<img>标签中的base64（富文本）
        Pattern imgPattern = Pattern.compile("<img[^>]+src\\s*=\\s*['\"]?([^'\">]+)['\"]?[^>]*>", Pattern.CASE_INSENSITIVE);
        StringBuffer result = new StringBuffer();
        Matcher imgMatcher = imgPattern.matcher(content);
        while (imgMatcher.find()) {
            String src = imgMatcher.group(1);
            if (isBase64Image(src)) {
                String newUrl = uploadBase64Image(src);
                imgMatcher.appendReplacement(result, imgMatcher.group().replace(src, newUrl));
            }
        }
        imgMatcher.appendTail(result);
        String processedContent = result.toString();

        // 处理Markdown中的![]()中的base64（Markdown）
        Pattern mdPattern = Pattern.compile("!\\[(.*?)\\]\\((.*?)\\)");
        StringBuffer mdResult = new StringBuffer();
        Matcher mdMatcher = mdPattern.matcher(processedContent);
        while (mdMatcher.find()) {
            String url = mdMatcher.group(2);
            if (isBase64Image(url)) {
                String newUrl = uploadBase64Image(url);
                mdMatcher.appendReplacement(mdResult, "![" + mdMatcher.group(1) + "](" + newUrl + ")");
            }
        }
        mdMatcher.appendTail(mdResult);

        return mdResult.toString();
    }

    /**
     * 根据类型转文件后缀
     */
    private String getImageSuffix(String type) {
        return switch (type.toLowerCase()) {
            case "jpeg", "jpg" -> "jpg";
            case "png" -> "png";
            case "gif" -> "gif";
            case "bmp" -> "bmp";
            case "webp" -> "webp";
            default -> "jpg";
        };
    }
}
