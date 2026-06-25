package com.moyun.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyun.system.domain.entity.SysLogininfor;
import com.moyun.system.domain.query.LogininforQuery;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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

    // ========== 运营首页登录统计 ==========

    /**
     * 今日登录人数（去重用户名）
     */
    @Select("SELECT COUNT(DISTINCT user_name) FROM sys_logininfor WHERE login_time >= #{startTime}")
    long countTodayLoginUsers(@Param("startTime") LocalDateTime startTime);

    /**
     * 今日登录总次数
     */
    @Select("SELECT COUNT(*) FROM sys_logininfor WHERE login_time >= #{startTime}")
    long countTodayLoginCount(@Param("startTime") LocalDateTime startTime);

    /**
     * 今日登录成功次数
     */
    @Select("SELECT COUNT(*) FROM sys_logininfor WHERE login_time >= #{startTime} AND status = '0'")
    long countTodayLoginSuccess(@Param("startTime") LocalDateTime startTime);

    /**
     * 近N天每日登录趋势（折线图，区分成功/失败）
     */
    @Select("SELECT DATE(login_time) AS date, " +
            "COUNT(*) AS value, " +
            "CASE WHEN status = '0' THEN 'success' ELSE 'fail' END AS label " +
            "FROM sys_logininfor " +
            "WHERE login_time >= #{startTime} " +
            "GROUP BY DATE(login_time), status " +
            "ORDER BY date")
    List<Map<String, Object>> selectDailyLoginTrend(@Param("startTime") LocalDateTime startTime);
}
