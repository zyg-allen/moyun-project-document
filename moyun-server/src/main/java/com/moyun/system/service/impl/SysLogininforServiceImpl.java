package com.moyun.system.service.impl;

import com.moyun.system.domain.entity.SysLogininfor;
import com.moyun.system.domain.query.LogininforQuery;
import com.moyun.system.mapper.SysLogininforMapper;
import com.moyun.system.service.ISysLogininforService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 系统访问日志情况信息 服务层处理
 *
 * @author ruoyi
 */
@Service
public class SysLogininforServiceImpl implements ISysLogininforService {

    @Autowired
    private SysLogininforMapper logininforMapper;

    /**
     * 新增系统登录日志
     *
     * @param logininfor 访问日志对象
     */
    @Override
    public void insertLogininfor(SysLogininfor logininfor) {
        logininforMapper.insertLogininfor(logininfor);
    }

    /**
     * 查询系统登录日志集合
     *
     * @param query 访问日志查询条件
     * @return 登录记录集合
     */
    @Override
    public List<SysLogininfor> selectLogininforList(LogininforQuery query) {
        return logininforMapper.selectLogininforList(query);
    }

    /**
     * 批量删除系统登录日志
     *
     * @param infoIds 需要删除的登录日志ID
     * @return 结果
     */
    @Override
    public int deleteLogininforByIds(Long[] infoIds) {
        return logininforMapper.deleteLogininforByIds(infoIds);
    }

    /**
     * 清空系统登录日志
     */
    @Override
    public void cleanLogininfor() {
        logininforMapper.cleanLogininfor();
    }
}
