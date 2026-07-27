package com.moyun.ext.cms.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.ext.cms.domain.query.UserResumeQuery;
import com.moyun.ext.cms.domain.vo.ResumeAiAdviceVO;
import com.moyun.ext.cms.domain.vo.UserResumeVO;

import java.util.List;

/**
 * 用户简历 Service 接口（面试空间第2期）
 *
 * @author moyun
 */
public interface IUserResumeService {

    /**
     * 分页查询当前用户的简历列表
     */
    Page<UserResumeVO> selectMyResumePage(Page<UserResumeVO> page, Long userId, UserResumeQuery query);

    /**
     * 查询简历详情（仅返回属于当前用户的简历）
     *
     * @param id     简历ID
     * @param userId 当前用户ID（用于权限校验）
     * @return 简历 VO；若不存在或不属于该用户则返回 null
     */
    UserResumeVO selectResumeDetail(Long id, Long userId);

    /**
     * 保存简历（新增或更新）。
     * <p>当 vo.id 为空时新增（versionNo=1, parentId=null）；非空时更新原记录并保留 JSON 字段。</p>
     *
     * @param vo     简历内容（结构化字段会被序列化为 JSON 存入数据库）
     * @param userId 当前用户ID
     * @return 简历ID
     */
    Long saveResume(UserResumeVO vo, Long userId);

    /**
     * 删除简历（仅作者可删）
     *
     * @return 影响行数
     */
    int deleteResume(Long id, Long userId);

    /**
     * 将指定简历复制为新版本（生成新记录，parentId=原首版ID，versionNo+1）
     *
     * @return 新版本简历ID
     */
    Long copyResumeAsNewVersion(Long id, Long userId);

    /**
     * 查询某简历的全部历史版本
     */
    List<UserResumeVO> selectVersionHistory(Long id, Long userId);

    /**
     * 导出简历为 PDF（生成文件并回填 fileUrl/exportTime）
     *
     * @return 含 fileUrl 的简历 VO
     */
    UserResumeVO exportResumePdf(Long id, Long userId);

    /**
     * 对简历进行规则评分（回填 score/scoreDetail/scoredTime）
     *
     * @return 含评分结果的简历 VO
     */
    UserResumeVO scoreResume(Long id, Long userId);

    /**
     * 生成简历 AI 改进建议（v5.9 阶段2）
     * <p>
     * 基于当前评分明细与岗位匹配度子项生成改进建议，不持久化（每次实时生成）。
     * 当前为规则化生成，后期可替换为真实 AI 模型调用。
     *
     * @param id     简历ID
     * @param userId 当前用户ID
     * @return 改进建议 VO
     */
    ResumeAiAdviceVO generateAiAdvice(Long id, Long userId);

    /**
     * 更新简历状态（draft/published/archived）
     */
    int updateStatus(Long id, Long userId, String status);

    /**
     * 读取简历 PDF 文件磁盘路径（供认证下载端点使用）。
     * 内部已校验简历归属；返回 null 表示无导出文件或不属于该用户。
     */
    String getResumePdfDiskPath(Long id, Long userId);
}
