package com.moyun.util.file;

import com.moyun.common.config.RuoYiConfig;
import com.moyun.common.constant.Constants;
import com.moyun.util.string.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.util.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.Arrays;

@Slf4j
public class ImageUtils {

    public static byte[] getImage(String imagePath) {
        InputStream is = getFile(imagePath);
        try {
            return IOUtils.toByteArray(is);
        } catch (Exception e) {
            log.error("图片加载异常 {}", e);
            return null;
        } finally {
            IOUtils.closeQuietly(is);
        }
    }

    public static InputStream getFile(String imagePath) {
        try {
            byte[] result = readFile(imagePath);
            result = Arrays.copyOf(result, result.length);
            return new ByteArrayInputStream(result);
        } catch (Exception e) {
            log.error("获取图片异常 {}", e);
        }
        return null;
    }

    public static byte[] readFile(String url) {
        InputStream in = null;
        try {
            if (url.startsWith("http")) {
                // SSRF 防护：校验目标地址是否为内网/回环/链路本地地址
                validateUrl(url);
                URL urlObj = new URL(url);
                URLConnection urlConnection = urlObj.openConnection();
                urlConnection.setConnectTimeout(30 * 1000);
                urlConnection.setReadTimeout(60 * 1000);
                urlConnection.setDoInput(true);
                in = urlConnection.getInputStream();
            } else {
                String localPath = RuoYiConfig.getProfile();
                String downloadPath = localPath + StringUtils.substringAfter(url, Constants.RESOURCE_PREFIX);
                in = new FileInputStream(downloadPath);
            }
            return IOUtils.toByteArray(in);
        } catch (Exception e) {
            log.error("获取文件路径异常 {}", e);
            return null;
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

    /**
     * 校验 URL 是否指向内网地址，防止 SSRF 攻击
     * 覆盖 127.0.0.0/8、10.0.0.0/8、172.16.0.0/12、192.168.0.0/16、169.254.169.254 等地址
     */
    private static void validateUrl(String url) {
        try {
            URL u = new URL(url);
            String host = u.getHost();
            InetAddress addr = InetAddress.getByName(host);
            if (addr.isAnyLocalAddress() || addr.isLoopbackAddress()
                    || addr.isSiteLocalAddress() || addr.isLinkLocalAddress()
                    || host.equals("169.254.169.254")) {
                throw new RuntimeException("不允许访问内网地址");
            }
        } catch (UnknownHostException e) {
            throw new RuntimeException("无法解析主机名");
        } catch (MalformedURLException e) {
            throw new RuntimeException("URL格式错误");
        }
    }
}
