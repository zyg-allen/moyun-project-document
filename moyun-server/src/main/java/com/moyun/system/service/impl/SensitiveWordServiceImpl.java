package com.moyun.system.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.moyun.common.exception.system.ServiceException;
import com.moyun.system.domain.entity.SysSensitiveWord;
import com.moyun.system.domain.entity.SysSensitiveWordLog;
import com.moyun.system.filter.SensitiveWordFilter;
import com.moyun.system.mapper.SysSensitiveWordLogMapper;
import com.moyun.system.mapper.SysSensitiveWordMapper;
import com.moyun.system.service.ISensitiveWordService;

import lombok.extern.slf4j.Slf4j;

/**
 * 敏感词服务实现
 *
 * @author moyun
 */
@Slf4j
@Service
public class SensitiveWordServiceImpl extends ServiceImpl<SysSensitiveWordMapper, SysSensitiveWord>
        implements ISensitiveWordService {

    /** 命中日志内容片段最大长度 */
    private static final int LOG_CONTENT_MAX_LEN = 1000;

    /** 命中敏感词列表拼接最大长度 */
    private static final int HIT_WORDS_MAX_LEN = 500;

    @Autowired
    private SysSensitiveWordMapper sensitiveWordMapper;

    @Autowired
    private SysSensitiveWordLogMapper sensitiveWordLogMapper;

    @Autowired
    private SensitiveWordFilter sensitiveWordFilter;

    // ==================== 词库管理 ====================

    @Override
    public Page<SysSensitiveWord> selectWordPage(Page<SysSensitiveWord> page, SysSensitiveWord query) {
        LambdaQueryWrapper<SysSensitiveWord> qw = new LambdaQueryWrapper<>();
        if (query != null) {
            if (query.getWord() != null && !query.getWord().isEmpty()) {
                qw.like(SysSensitiveWord::getWord, query.getWord());
            }
            if (query.getCategory() != null && !query.getCategory().isEmpty()) {
                qw.eq(SysSensitiveWord::getCategory, query.getCategory());
            }
            if (query.getStatus() != null && !query.getStatus().isEmpty()) {
                qw.eq(SysSensitiveWord::getStatus, query.getStatus());
            }
        }
        qw.orderByDesc(SysSensitiveWord::getId);
        return sensitiveWordMapper.selectPage(page, qw);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertWord(SysSensitiveWord word) {
        if (word.getWord() == null || word.getWord().trim().isEmpty()) {
            throw new ServiceException("敏感词不能为空");
        }
        // 归一化：去空白、小写，避免同词重复入库与绕过
        word.setWord(word.getWord().trim().toLowerCase());
        if (word.getStatus() == null || word.getStatus().isEmpty()) {
            word.setStatus("0");
        }
        int rows = sensitiveWordMapper.insert(word);
        // 增删改后刷新词树，保证实时生效
        sensitiveWordFilter.reload();
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateWord(SysSensitiveWord word) {
        if (word.getId() == null) {
            throw new ServiceException("敏感词ID不能为空");
        }
        if (word.getWord() != null) {
            word.setWord(word.getWord().trim().toLowerCase());
        }
        int rows = sensitiveWordMapper.updateById(word);
        sensitiveWordFilter.reload();
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteWordByIds(Long[] ids) {
        if (ids == null || ids.length == 0) {
            return 0;
        }
        int rows = sensitiveWordMapper.deleteBatchIds(java.util.Arrays.asList(ids));
        sensitiveWordFilter.reload();
        return rows;
    }

    @Override
    public void reload() {
        sensitiveWordFilter.reload();
    }

    // ==================== 命中检测 ====================

    @Override
    public boolean contains(String content) {
        return sensitiveWordFilter.contains(content);
    }

    @Override
    public List<String> find(String content) {
        return sensitiveWordFilter.find(content);
    }

    @Override
    public String mask(String content) {
        return sensitiveWordFilter.mask(content);
    }

    @Override
    public List<String> detectAndLog(String bizType, Long bizId, Long userId, String content, String action) {
        List<String> hits = sensitiveWordFilter.find(content);
        if (hits == null || hits.isEmpty()) {
            return hits;
        }
        try {
            SysSensitiveWordLog logEntry = new SysSensitiveWordLog();
            logEntry.setBizType(bizType);
            logEntry.setBizId(bizId);
            logEntry.setUserId(userId);
            // 截断长内容，避免 TEXT 字段过大
            String snippet = content == null ? "" : content;
            if (snippet.length() > LOG_CONTENT_MAX_LEN) {
                snippet = snippet.substring(0, LOG_CONTENT_MAX_LEN);
            }
            logEntry.setContent(snippet);
            String hitWordsStr = String.join(",", hits);
            if (hitWordsStr.length() > HIT_WORDS_MAX_LEN) {
                hitWordsStr = hitWordsStr.substring(0, HIT_WORDS_MAX_LEN);
            }
            logEntry.setHitWords(hitWordsStr);
            logEntry.setHitCount(hits.size());
            logEntry.setAction(action == null ? "flag" : action);
            logEntry.setCreateTime(LocalDateTime.now());
            sensitiveWordLogMapper.insert(logEntry);
        } catch (Exception e) {
            // 命中日志失败不影响主流程
            log.warn("敏感词命中日志写入失败: bizType={}, bizId={}, err={}", bizType, bizId, e.getMessage());
        }
        return hits;
    }
}
