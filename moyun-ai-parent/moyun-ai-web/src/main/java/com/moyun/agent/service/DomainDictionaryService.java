package com.moyun.agent.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.moyun.agent.entity.DomainDictionary;

import java.util.List;

/**
 * 领域词典服务接口
 *
 * <p>管理领域词典，用于RAG查询扩展</p>
 *
 * @author laomao
 */
public interface DomainDictionaryService extends IService<DomainDictionary> {

    /**
     * 获取全局词典
     *
     * @return 全局词典列表
     */
    List<DomainDictionary> listGlobal();

    /**
     * 获取专业词典（非全局）
     *
     * @return 专业词典列表
     */
    List<DomainDictionary> listProfessional();
}
