package com.moyun.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.moyun.system.domain.entity.SysListener;
import com.moyun.system.mapper.SysListenerMapper;
import com.moyun.system.service.ISysListenerService;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * 流程监听 服务层实现
 *
 * @author Tony
 * @date 2022-12-25
 */
@Service
public class SysListenerServiceImpl extends ServiceImpl<SysListenerMapper, SysListener> implements ISysListenerService {

    /**
     * 查询流程监听
     *
     * @param id 流程监听主键
     * @return 流程监听
     */
    @Override
    public SysListener selectSysListenerById(Long id) {
        return baseMapper.selectById(id);
    }

    /**
     * 查询流程监听列表
     *
     * @param sysListener 流程监听
     * @return 流程监听集合
     */
    @Override
    public List<SysListener> selectSysListenerList(SysListener sysListener) {
        return baseMapper.selectSysListenerList(sysListener);
    }

    /**
     * 新增流程监听
     *
     * @param sysListener 流程监听
     * @return 结果
     */
    @Override
    public int insertSysListener(SysListener sysListener) {
        return baseMapper.insert(sysListener);
    }

    /**
     * 修改流程监听
     *
     * @param sysListener 流程监听
     * @return 结果
     */
    @Override
    public int updateSysListener(SysListener sysListener) {
        return baseMapper.updateById(sysListener);
    }

    /**
     * 批量删除流程监听
     *
     * @param ids 需要删除的流程监听主键集合
     * @return 结果
     */
    @Override
    public int deleteSysListenerByIds(Long[] ids) {
        return baseMapper.deleteByIds(Arrays.asList(ids));
    }

    /**
     * 删除流程监听信息
     *
     * @param id 流程监听主键
     * @return 结果
     */
    @Override
    public int deleteSysListenerById(Long id) {
        return baseMapper.deleteById(id);
    }
}