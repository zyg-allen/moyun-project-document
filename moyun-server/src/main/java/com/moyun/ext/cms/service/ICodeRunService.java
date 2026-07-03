package com.moyun.ext.cms.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.core.base.page.PageDomain;
import com.moyun.portal.domain.entity.PortalCodeRun;

/**
 * 在线代码运行 Service 接口（任务 3.6 学习者成长闭环）
 * <p>
 * 提供代码沙箱执行与运行历史查询能力，不引入 Docker，使用 ProcessBuilder + 超时限制 + 输出截断。
 *
 * @author moyun
 */
public interface ICodeRunService {

    /**
     * 同步执行用户提交的代码并落库运行记录。
     * 支持 java / python / javascript 三种语言，超时 5s 强制结束，输出截断 1MB。
     *
     * @param userId   运行者用户ID
     * @param language 编程语言
     * @param code     源代码
     * @param stdin    标准输入（可空）
     * @return 落库后的运行记录（含 output/errorMsg/status/runtimeMs）
     */
    PortalCodeRun runCode(Long userId, String language, String code, String stdin);

    /**
     * 查询当前用户的运行历史（分页，按创建时间倒序）
     */
    Page<PortalCodeRun> listMyRuns(Long userId, PageDomain query);

    /**
     * 查询运行详情（仅返回归属于当前用户的记录，避免越权读取他人代码）
     */
    PortalCodeRun getRunDetail(Long id, Long userId);
}
