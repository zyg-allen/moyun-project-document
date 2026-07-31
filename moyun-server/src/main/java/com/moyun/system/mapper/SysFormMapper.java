
// ... existing code ...
package com.moyun.system.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.moyun.system.domain.entity.SysForm;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 流程表单Mapper接口
 * 
 * @author Tony
 * @date 2021-03-30
 */
public interface SysFormMapper 
{
    /**
     * 查询流程表单
     * 
     * @param formId 流程表单ID
     * @return 流程表单
     */
    public SysForm selectSysFormById(Long formId);

    /**
     * 查询流程表单列表
     * 
     * @param sysForm 流程表单
     * @return 流程表单集合
     */
    public List<SysForm> selectSysFormList(SysForm sysForm);

    /**
     * 分页查询流程表单列表（MyBatis-Plus 标准分页，配合 PaginationInnerInterceptor）
     *
     * @param page    分页对象
     * @param sysForm 流程表单
     * @return 分页结果
     */
    IPage<SysForm> selectSysFormPage(IPage<SysForm> page, @Param("query") SysForm sysForm);

    /**
     * 新增流程表单
     * 
     * @param sysForm 流程表单
     * @return 结果
     */
    public int insertSysForm(SysForm sysForm);

    /**
     * 修改流程表单
     * 
     * @param sysForm 流程表单
     * @return 结果
     */
    public int updateSysForm(SysForm sysForm);

    /**
     * 批量删除流程表单
     * 
     * @param formIds 需要删除的流程表单ID
     * @return 结果
     */
    public int deleteSysFormByIds(Long[] formIds);

    /**
     * 删除流程表单
     * 
     * @param formId 流程表单ID
     * @return 结果
     */
    public int deleteSysFormById(Long formId);
}
