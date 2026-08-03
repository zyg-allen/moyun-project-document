package com.moyun.system.service;

import java.util.List;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.moyun.system.domain.entity.SysSensitiveWord;
import com.moyun.system.domain.entity.SysSensitiveWordLog;

/**
 * 敏感词服务层
 * <p>封装词库管理与命中检测两类能力：
 * <ul>
 *   <li>词库管理：CMS 后台增删改查 + 词树刷新</li>
 *   <li>命中检测：供业务层调用，返回命中结果并写入审计日志</li>
 * </ul>
 *
 * @author moyun
 */
public interface ISensitiveWordService extends IService<SysSensitiveWord> {

    // ==================== 词库管理 ====================

    /**
     * 分页查询敏感词列表
     */
    Page<SysSensitiveWord> selectWordPage(Page<SysSensitiveWord> page, SysSensitiveWord query);

    /**
     * 新增敏感词（自动去空白、小写归一、触发词树刷新）
     */
    int insertWord(SysSensitiveWord word);

    /**
     * 修改敏感词（自动触发词树刷新）
     */
    int updateWord(SysSensitiveWord word);

    /**
     * 批量删除敏感词（自动触发词树刷新）
     */
    int deleteWordByIds(Long[] ids);

    /**
     * 重新加载词库（手动刷新入口）
     */
    void reload();

    // ==================== 命中检测 ====================

    /**
     * 检测内容是否命中敏感词（不写日志，仅判断）
     *
     * @param content 待检测内容
     * @return true 表示命中
     */
    boolean contains(String content);

    /**
     * 检测内容并返回命中敏感词列表（不写日志）
     *
     * @param content 待检测内容
     * @return 命中敏感词列表，无命中返回空列表
     */
    List<String> find(String content);

    /**
     * 脱敏替换：将命中的敏感词替换为 *
     *
     * @param content 原始内容
     * @return 脱敏后的内容（无命中返回原文）
     */
    String mask(String content);

    /**
     * 检测内容并写入命中审计日志
     *
     * @param bizType 业务类型：article/column/topic/topic_post/topic_comment/report
     * @param bizId   业务主键ID（可为 null，如提交前校验时还未生成）
     * @param userId  提交人ID（portal_user.id）
     * @param content 待检测内容
     * @param action  处理动作：block=拦截 / pending=转待审核 / flag=仅标记
     * @return 命中敏感词列表，无命中返回空列表
     */
    List<String> detectAndLog(String bizType, Long bizId, Long userId, String content, String action);
}
