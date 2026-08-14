package com.moyun.portal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.moyun.portal.domain.entity.PortalImportTemplateConfig;

import java.util.List;

/**
 * 导入模板字段配置 数据层
 *
 * @author moyun
 */
@Mapper
public interface PortalImportTemplateConfigMapper extends BaseMapper<PortalImportTemplateConfig> {

    /**
     * 按业务标识查询所有启用的字段配置（按 sort 升序、id 升序）
     *
     * @param businessKey 业务标识
     * @return 字段配置列表
     */
    List<PortalImportTemplateConfig> selectEnabledByBusinessKey(@Param("businessKey") String businessKey);

    /**
     * 按业务标识查询全部字段配置（含停用，运营管理用）
     *
     * @param businessKey 业务标识
     * @return 字段配置列表
     */
    List<PortalImportTemplateConfig> selectAllByBusinessKey(@Param("businessKey") String businessKey);

    /**
     * 按业务标识删除全部字段配置（运营重置场景）
     *
     * @param businessKey 业务标识
     * @return 删除行数
     */
    int deleteByBusinessKey(@Param("businessKey") String businessKey);
}
