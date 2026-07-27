package com.moyun.agent.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.moyun.agent.dto.KnowledgeLibraryDTO;
import com.moyun.agent.entity.KnowledgeLibrary;
import com.moyun.agent.vo.KnowledgeLibraryVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 知识库服务接口
 *
 * @author laomao
 */
public interface KnowledgeLibraryService extends IService<KnowledgeLibrary> {
    
    /**
     * 创建知识库
     *
     * @param dto 知识库信息
     * @return 知识库ID
     */
    Long createLibrary(KnowledgeLibraryDTO dto);
    
    /**
     * 更新知识库
     *
     * @param dto 知识库信息
     */
    void updateLibrary(KnowledgeLibraryDTO dto);
    
    /**
     * 删除知识库（同时删除所有文档）
     *
     * @param libraryId 知识库ID
     */
    void deleteLibrary(Long libraryId);
    
    /**
     * 获取知识库详情
     *
     * @param libraryId 知识库ID
     * @return 知识库详情（包含文档列表）
     */
    KnowledgeLibraryVO getLibraryDetail(Long libraryId);
    
    /**
     * 分页查询知识库列表
     *
     * @param page     页码
     * @param size     每页数量
     * @param keyword  搜索关键词
     * @param category 分类筛选
     * @return 分页结果
     */
    Page<KnowledgeLibraryVO> listLibraries(int page, int size, String keyword, String category);
    
    /**
     * 获取所有知识库（用于下拉选择）
     *
     * @return 知识库列表
     */
    List<KnowledgeLibraryVO> listAllLibraries();
    
    /**
     * 上传文档到知识库
     *
     * @param libraryId 知识库ID
     * @param file      文件
     * @return 文档ID
     */
    Long uploadDocument(Long libraryId, MultipartFile file);
    
    /**
     * 批量上传文档到知识库
     *
     * @param libraryId 知识库ID
     * @param files     文件列表
     * @return 文档ID列表
     */
    List<Long> uploadDocuments(Long libraryId, List<MultipartFile> files);
    
    /**
     * 从知识库中删除文档
     *
     * @param libraryId  知识库ID
     * @param documentId 文档ID
     */
    void deleteDocument(Long libraryId, Long documentId);
    
    /**
     * 处理知识库中的所有待处理文档
     *
     * @param libraryId 知识库ID
     */
    void processDocuments(Long libraryId);
    
    /**
     * 更新知识库统计信息
     *
     * @param libraryId 知识库ID
     */
    void updateStatistics(Long libraryId);
    
    /**
     * 知识库检索测试 - 检索知识库下所有文档
     *
     * @param libraryId 知识库ID
     * @param request   检索请求
     * @return 检索结果列表
     */
    List<com.moyun.agent.dto.RetrievalTestResult> testRetrieval(Long libraryId, com.moyun.agent.dto.RetrievalTestRequest request);
}
