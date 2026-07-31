package com.moyun.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.moyun.system.domain.entity.SysListener;

import java.util.List;

/**
 * 流程监听Service接口
 *
 * @author Tony
 * @date 2022-12-25
 */
public interface ISysListenerService {

    /**
     * 查询流程监听
     *
     * @param id 流程监听主键
     * @return 流程监听
     */
    SysListener selectSysListenerById(Long id);

    /**
     * 查询流程监听列表
     *
     * @param sysListener 流程监听
     * @return 流程监听集合
     */
    List<SysListener> selectSysListenerList(SysListener sysListener);

    /**
     * 分页查询流程监听（MyBatis-Plus 标准分页，配合 PaginationInnerInterceptor）
     *
     * @param page        分页对象
     * @param sysListener 流程监听查询条件
     * @return 分页结果
     */
    IPage<SysListener> selectSysListenerPage(IPage<SysListener> page, SysListener sysListener);

    /**
     * 新增流程监听
     *
     * @param sysListener 流程监听
     * @return 结果
     */
    int insertSysListener(SysListener sysListener);

    /**
     * 修改流程监听
     *
     * @param sysListener 流程监听
     * @return 结果
     */
    int updateSysListener(SysListener sysListener);

    /**
     * 批量删除流程监听
     *
     * @param ids 需要删除的流程监听主键集合
     * @return 结果
     */
    int deleteSysListenerByIds(Long[] ids);

    /**
     * 删除流程监听信息
     *
     * @param id 流程监听主键
     * @return 结果
     */
    int deleteSysListenerById(Long id);
}