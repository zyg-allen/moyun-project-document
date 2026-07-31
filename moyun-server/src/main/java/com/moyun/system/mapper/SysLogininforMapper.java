package com.moyun.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
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
     * 分页查询系统访问记录（MyBatis-Plus 标准分页，配合 PaginationInnerInterceptor）
     *
     * @param page  分页对象
     * @param query 访问记录查询条件
     * @return 分页结果
     */
    IPage<SysLogininfor> selectLogininforPage(IPage<SysLogininfor> page, @Param("query") LogininforQuery query);

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
     * 今日登录人数（去重用户名，前后台合计）
     */
    @Select("SELECT COUNT(DISTINCT user_name) FROM sys_logininfor WHERE login_time >= #{startTime}")
    long countTodayLoginUsers(@Param("startTime") LocalDateTime startTime);

    /**
     * 今日登录人数（按 user_type 过滤，去重用户名）
     *
     * @param startTime 起始时间
     * @param userType  登录来源类型（sys=后台用户 portal=门户用户）
     */
    @Select("SELECT COUNT(DISTINCT user_name) FROM sys_logininfor WHERE login_time >= #{startTime} AND user_type = #{userType}")
    long countTodayLoginUsersByType(@Param("startTime") LocalDateTime startTime, @Param("userType") String userType);

    /**
     * 今日登录总次数（前后台合计）
     */
    @Select("SELECT COUNT(*) FROM sys_logininfor WHERE login_time >= #{startTime}")
    long countTodayLoginCount(@Param("startTime") LocalDateTime startTime);

    /**
     * 今日登录成功次数
     */
    @Select("SELECT COUNT(*) FROM sys_logininfor WHERE login_time >= #{startTime} AND status = '0'")
    long countTodayLoginSuccess(@Param("startTime") LocalDateTime startTime);

    /**
     * 近N天每日登录趋势（折线图，区分成功/失败 + 前后台来源）
     * 使用 DATE_FORMAT 返回纯字符串，避免 java.sql.Date 序列化格式不一致导致日期 key 匹配失败
     * label 维度：portal_success/portal_fail/sys_success/sys_fail，前端可按需聚合展示
     */
    @Select("SELECT DATE_FORMAT(login_time, '%Y-%m-%d') AS date, " +
            "COUNT(*) AS value, " +
            "CONCAT(IFNULL(user_type,'sys'), '_', CASE WHEN status = '0' THEN 'success' ELSE 'fail' END) AS label " +
            "FROM sys_logininfor " +
            "WHERE login_time >= #{startTime} " +
            "GROUP BY DATE_FORMAT(login_time, '%Y-%m-%d'), user_type, status " +
            "ORDER BY date")
    List<Map<String, Object>> selectDailyLoginTrend(@Param("startTime") LocalDateTime startTime);
}
