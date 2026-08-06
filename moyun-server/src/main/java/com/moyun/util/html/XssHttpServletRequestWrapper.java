package com.moyun.util.html;

import com.moyun.util.string.StringUtils;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.apache.commons.io.IOUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.io.ByteArrayInputStream;
import java.io.IOException;

public class XssHttpServletRequestWrapper extends HttpServletRequestWrapper {
    public XssHttpServletRequestWrapper(HttpServletRequest request) {
        super(request);
    }

    @Override
    public String[] getParameterValues(String name) {
        String[] values = super.getParameterValues(name);
        if (values != null) {
            int length = values.length;
            String[] escapesValues = new String[length];
            for (int i = 0; i < length; i++) {
                escapesValues[i] = EscapeUtil.clean(values[i]).trim();
            }
            return escapesValues;
        }
        return super.getParameterValues(name);
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        // JSON 请求体直接透传，不做 HTML 过滤。
        // 原因：HTMLFilter 不理解 JSON 结构，会把 JSON 字符串值中转义的双引号 \"
        // 误解为 HTML 属性边界，重建标签时破坏 JSON 转义，导致 Jackson 解析失败
        // （如 <img src=\"url\"> 被重组为 <img src="\\"/>，双引号不再转义，JSON 字符串提前结束）。
        // XSS 防护应在业务层针对具体字段做 HTML 净化（如对富文本 content 字段做标签白名单过滤），
        // 而不是在 filter 层对整个 JSON 请求体做粗暴的 HTML 过滤。
        // 表单参数（getParameterValues）仍保留 HTML 过滤。
        return super.getInputStream();
    }

    public boolean isJsonRequest() {
        String header = super.getHeader(HttpHeaders.CONTENT_TYPE);
        return StringUtils.startsWithIgnoreCase(header, MediaType.APPLICATION_JSON_VALUE);
    }
}
