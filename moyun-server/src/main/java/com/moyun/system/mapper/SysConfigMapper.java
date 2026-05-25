package com.moyun.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyun.system.domain.entity.SysConfig;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 参数配置 数据层
 *
 * @author ruoyi
 */
@Mapper
public interface SysConfigMapper extends BaseMapper<SysConfig> {

    /**
     * 查询参数配置信息
     *
     * @param config 参数配置信息
     * @return 参数配置信息
     */
    List<SysConfig> selectConfigList(SysConfig config);

    /**
     * 通过键名查询参数配置信息
     *
     * @param configKey 参数键名
     * @return 参数配置信息
     */
    SysConfig selectConfigByKey(String configKey);

    /**
     * 根据键名查询参数配置信息
     *
     * @param configId 参数主键
     * @return 参数配置信息
     */
    SysConfig selectConfigById(Long configId);

    /**
     * 新增参数配置信息
     *
     * @param config 参数配置信息
     * @return 结果
     */
    int insertConfig(SysConfig config);

    /**
     * 修改参数配置信息
     *
     * @param config 参数配置信息
     * @return 结果
     */
    int updateConfig(SysConfig config);

    /**
     * 删除参数配置信息
     *
     * @param configId 参数主键
     * @return 结果
     */
    int deleteConfigById(Long configId);

    /**
     * 批量删除参数配置信息
     *
     * @param configIds 需要删除的参数主键
     * @return 结果
     */
    int deleteConfigByIds(Long[] configIds);

    /**
     * 校验参数键名是否唯一
     *
     * @param configKey 参数键名
     * @return 结果
     */
    SysConfig checkConfigKeyUnique(String configKey);
}