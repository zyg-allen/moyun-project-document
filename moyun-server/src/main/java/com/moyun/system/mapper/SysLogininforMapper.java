package com.moyun.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyun.system.domain.entity.SysLogininfor;
import com.moyun.system.domain.query.LogininforQuery;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 系统访问记录 数据层
 *
 * @author ruoyi
 */
@Mapper
public interface SysLogininforMapper extends BaseMapper<SysLogininfor> {

    /**
     * 查询系统访问记录集合
     *
     * @param query 访问记录查询条件
     * @return 访问记录集合
     */
    List<SysLogininfor> selectLogininforList(LogininforQuery query);

    /**
     * 批量删除系统访问记录
     *
     * @param infoIds 需要删除的访问记录ID
     * @return 结果
     */
    int deleteLogininforByIds(Long[] infoIds);

    /**
     * 清空系统访问记录
     */
    void cleanLogininfor();

    /**
     * 新增系统访问记录
     *
     * @param logininfor 访问记录对象
     * @return 结果
     */
    int insertLogininfor(SysLogininfor logininfor);
}
