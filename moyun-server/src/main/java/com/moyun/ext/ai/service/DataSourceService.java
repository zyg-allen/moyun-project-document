package com.moyun.ext.ai.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.moyun.ext.ai.entity.DataSourceConfig;
import com.moyun.ext.ai.vo.TableInfoVO;
import com.moyun.ext.ai.vo.TableSchemaVO;

import java.util.List;

/**
 * 数据源管理服务接口
 *
 * @author laomao
 */
public interface DataSourceService extends IService<DataSourceConfig> {

    /**
     * 测试数据源连接
     *
     * @param config 数据源配置
     * @return 是否连接成功
     */
    boolean testConnection(DataSourceConfig config);

    /**
     * 获取数据源的所有表
     *
     * @param datasourceId 数据源ID
     * @return 表名列表
     */
    List<String> listTables(Long datasourceId);
    
    /**
     * 获取数据源的所有表详细信息
     *
     * @param datasourceId 数据源ID
     * @return 表信息列表
     */
    List<TableInfoVO> listTablesWithInfo(Long datasourceId);

    /**
     * 获取表结构
     *
     * @param datasourceId 数据源ID
     * @param tableName 表名
     * @return 表结构信息
     */
    TableSchemaVO getTableSchema(Long datasourceId, String tableName);

    /**
     * 同步表元数据
     *
     * @param datasourceId 数据源ID
     */
    void syncTableMetadata(Long datasourceId);

    /**
     * 检查数据源健康状态
     *
     * @param datasourceId 数据源ID
     * @return 健康状态
     */
    String checkHealth(Long datasourceId);
}
