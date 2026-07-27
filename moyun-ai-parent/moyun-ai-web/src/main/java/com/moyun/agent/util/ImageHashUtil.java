package com.moyun.agent.util;

import lombok.extern.slf4j.Slf4j;

import java.awt.image.BufferedImage;
import java.security.MessageDigest;

@Slf4j
public class ImageHashUtil {
    
    public static String calculateHash(BufferedImage image) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            
            int step = Math.max(1, image.getWidth() / 10);
            for (int y = 0; y < image.getHeight(); y += step) {
                for (int x = 0; x < image.getWidth(); x += step) {
                    int rgb = image.getRGB(x, y);
                    md.update((byte) (rgb >> 16));
                    md.update((byte) (rgb >> 8));
                    md.update((byte) rgb);
                }
            }
            
            byte[] digest = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            log.warn("计算图片hash失败", e);
            return java.util.UUID.randomUUID().toString();
        }
    }
}
