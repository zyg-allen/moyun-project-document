package com.moyun.portal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.moyun.portal.domain.entity.PortalGrowthRule;

/**
 * 成长规则配置 数据层
 *
 * @author moyun
 */
@Mapper
public interface PortalGrowthRuleMapper extends BaseMapper<PortalGrowthRule> {

    /**
     * 根据模块和行为查询启用的规则
     */

    PortalGrowthRule selectByModuleAndAction(@Param("module") String module, @Param("action") String action);

    /**
     * 查询所有启用的规则
     */

    java.util.List<PortalGrowthRule> selectAllEnabled();
}
