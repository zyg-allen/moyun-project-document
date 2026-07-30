package com.moyun.ext.flowable.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.moyun.system.domain.entity.SysExpression;

import java.util.List;

/**
 * 流程达式Service接口
 *
 * @author ruoyi
 * @date 2022-12-12
 */
public interface ISysExpressionService {
    /**
     * 查询流程达式
     *
     * @param id 流程达式主键
     * @return 流程达式
     */
    public SysExpression selectSysExpressionById(Long id);

    /**
     * 查询流程达式列表
     *
     * @param sysExpression 流程达式
     * @return 流程达式集合
     */
    public List<SysExpression> selectSysExpressionList(SysExpression sysExpression);

    /**
     * 分页查询流程达式（MyBatis-Plus 标准分页，配合 PaginationInnerInterceptor）
     *
     * @param page          分页对象
     * @param sysExpression 流程达式查询条件
     * @return 分页结果
     */
    public IPage<SysExpression> selectSysExpressionPage(IPage<SysExpression> page, SysExpression sysExpression);

    /**
     * 新增流程达式
     *
     * @param sysExpression 流程达式
     * @return 结果
     */
    public int insertSysExpression(SysExpression sysExpression);

    /**
     * 修改流程达式
     *
     * @param sysExpression 流程达式
     * @return 结果
     */
    public int updateSysExpression(SysExpression sysExpression);

    /**
     * 批量删除流程达式
     *
     * @param ids 需要删除的流程达式主键集合
     * @return 结果
     */
    public int deleteSysExpressionByIds(Long[] ids);

    /**
     * 删除流程达式信息
     *
     * @param id 流程达式主键
     * @return 结果
     */
    public int deleteSysExpressionById(Long id);
}
