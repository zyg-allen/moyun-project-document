package com.moyun.ext.cms.service.interview.impl;

import com.moyun.ext.cms.service.interview.WebSearchService;
import org.springframework.stereotype.Component;

/**
 * 联网核查空实现（默认）：直接返回 null，调用方跳过核查上下文注入
 *
 * @author moyun
 */
@Component
public class NoopWebSearchServiceImpl implements WebSearchService {

    @Override
    public String search(String query) {
        return null;
    }
}
