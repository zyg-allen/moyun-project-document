package com.moyun.ext.ai.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.moyun.ext.ai.vo.KnowledgeBaseVO;
import com.moyun.ext.ai.dto.KnowledgeStatsResponse;
import com.moyun.ext.ai.dto.RetrievalTestRequest;
import com.moyun.ext.ai.dto.RetrievalTestResult;
import com.moyun.ext.ai.entity.KnowledgeBase;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 知识库服务接口
 *
 * <p>提供知识库文件上传、向量化处理、删除等核心功能</p>
 *
 * @author laomao
 */
public interface KnowledgeBaseService extends IService<KnowledgeBase> {

    /**
     * 上传文件并处理向量化
     *
     * @param file 上传的文件
     * @return 知识库实体
     * @throws Exception 处理异常
     */
    KnowledgeBase uploadFile(MultipartFile file) throws Exception;

    /**
     * 获取所有知识库列表（带格式化信息）
     *
     * @return 知识库VO列表
     */
    List<KnowledgeBaseVO> listAllWithFormat();

    /**
     * 删除知识库（包括向量数据）
     *
     * @param id 知识库ID
     * @return 是否删除成功
     */
    boolean deleteKnowledge(Long id);

    /**
     * 重新处理文件向量化
     *
     * @param id 知识库ID
     * @return 是否成功
     */
    boolean reprocessFile(Long id);

    /**
     * 只上传文件，不进行处理
     *
     * @param file 上传的文件
     * @return 知识库实体
     * @throws Exception 上传异常
     */
    KnowledgeBase uploadFileOnly(MultipartFile file) throws Exception;

    /**
     * 根据配置处理知识库
     *
     * @param knowledgeId 知识库ID
     * @param config 处理配置
     * @throws Exception 处理异常
     */
    void processKnowledge(Long knowledgeId, com.moyun.ext.ai.entity.KnowledgeConfig config) throws Exception;
    
    /**
     * 更新知识库元数据
     *
     * @param id 知识库ID
     * @param category 分组
     * @param tags 标签（JSON字符串）
     * @param description 描述
     * @return 是否更新成功
     */
    boolean updateMetadata(Long id, String category, String tags, String description);
    
    /**
     * 获取知识库统计信息
     *
     * @return 统计响应
     */
    KnowledgeStatsResponse getStats();
    
    /**
     * 测试知识库检索
     *
     * @param id 知识库ID
     * @param request 检索请求
     * @return 检索结果列表
     */
    List<RetrievalTestResult> testRetrieval(Long id, RetrievalTestRequest request);
    
    /**
     * 批量操作
     *
     * @param operation 操作类型
     * @param ids 知识库ID列表
     * @param category 分组
     * @param tags 标签
     * @return 是否成功
     */
    boolean batchOperation(String operation, java.util.List<Long> ids, String category, String tags);
    
    /**
     * 更新知识库使用统计
     * 在知识库被检索时调用
     *
     * @param knowledgeId 知识库ID
     * @param hitCount 本次命中数量
     */
    void updateUsageStats(Long knowledgeId, int hitCount);
    
    /**
     * 只上传文件，返回文档ID
     *
     * @param file 上传的文件
     * @return 文档ID
     */
    Long uploadKnowledge(org.springframework.web.multipart.MultipartFile file);
    
    /**
     * 使用知识库配置处理文档
     *
     * @param documentId 文档ID
     * @param config 知识库配置
     */
    void processKnowledgeWithConfig(Long documentId, com.moyun.ext.ai.entity.KnowledgeLibraryConfig config);
    
    /**
     * 多文档检索测试
     *
     * @param documentIds 文档ID列表
     * @param request 检索请求
     * @return 检索结果列表
     */
    java.util.List<RetrievalTestResult> testRetrievalMultiple(java.util.List<Long> documentIds, RetrievalTestRequest request);
    
    /**
     * 修复向量维度
     * 从 document_segment 表中读取向量维度并更新到 knowledge_base 表
     *
     * @param id 知识库ID，如果为null则修复所有
     * @return 修复的记录数
     */
    int fixVectorDimension(Long id);
}
