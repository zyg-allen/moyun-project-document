package com.moyun.agent.service;

/**
 * 架构图生成服务接口
 *
 * @author laomao
 */
public interface DiagramService {
    
    /**
     * 根据用户描述生成架构图数据
     *
     * @param content 用户输入的架构描述
     * @param style   图表风格: normal(普通) / enterprise(企业级)
     * @return 架构图 JSON 数据
     */
    String generateDiagram(String content, String style);
}
