package com.moyun.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.moyun.system.domain.entity.SysLogininfor;
import com.moyun.system.domain.query.LogininforQuery;

import java.util.List;

/**
 * 系统访问日志情况信息 服务层
 *
 * @author ruoyi
 */
public interface ISysLogininforService {
    /**
     * 新增系统登录日志
     *
     * @param logininfor 访问日志对象
     */
    public void insertLogininfor(SysLogininfor logininfor);

    /**
     * 查询系统登录日志集合
     *
     * @param query 访问记录查询条件
     * @return 登录记录集合
     */
    public List<SysLogininfor> selectLogininforList(LogininforQuery query);

    /**
     * 分页查询系统登录日志（MyBatis-Plus 标准分页，配合 PaginationInnerInterceptor）
     *
     * @param page  分页对象
     * @param query 访问记录查询条件
     * @return 分页结果
     */
    public IPage<SysLogininfor> selectLogininforPage(IPage<SysLogininfor> page, LogininforQuery query);

    /**
     * 批量删除系统登录日志
     *
     * @param infoIds 需要删除的登录日志ID
     * @return 结果
     */
    public int deleteLogininforByIds(Long[] infoIds);

    /**
     * 清空系统登录日志
     */
    public void cleanLogininfor();
}
