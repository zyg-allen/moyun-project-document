package com.moyun.ext.cms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.common.exception.system.ServiceException;
import com.moyun.core.base.page.PageDomain;
import com.moyun.ext.cms.service.CodeExecutorService;
import com.moyun.ext.cms.service.ICodeRunService;
import com.moyun.portal.domain.entity.PortalCodeRun;
import com.moyun.portal.mapper.PortalCodeRunMapper;
import com.moyun.util.bean.PageUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 在线代码运行 Service 实现（任务 3.6 学习者成长闭环）
 *
 * @author moyun
 */
@Service
public class CodeRunServiceImpl implements ICodeRunService {

    @Autowired private PortalCodeRunMapper codeRunMapper;
    @Autowired private CodeExecutorService codeExecutor;

    @Override
    public PortalCodeRun runCode(Long userId, String language, String code, String stdin) {
        if (userId == null) {
            throw new ServiceException("请登录后操作");
        }

        // 先落库一条 running 记录，便于审计与失败追溯
        PortalCodeRun record = new PortalCodeRun();
        record.setUserId(userId);
        record.setLanguage(language);
        record.setCode(code);
        record.setStdin(stdin);
        record.setStatus("running");
        record.setCreateTime(LocalDateTime.now());
        codeRunMapper.insert(record);

        // 执行（同步，沙箱内超时 5s）
        CodeExecutorService.ExecuteResult result;
        try {
            result = codeExecutor.execute(language, code, stdin, String.valueOf(userId));
        } catch (ServiceException e) {
            record.setStatus("failed");
            record.setErrorMsg(e.getMessage());
            record.setOutput("");
            record.setRuntimeMs(0);
            codeRunMapper.updateById(record);
            throw e;
        }

        record.setStatus(result.getStatus());
        record.setOutput(result.getOutput());
        record.setErrorMsg(result.getErrorMsg());
        record.setRuntimeMs(result.getRuntimeMs());
        record.setMemKb(result.getMemKb());
        codeRunMapper.updateById(record);
        return record;
    }

    @Override
    public Page<PortalCodeRun> listMyRuns(Long userId, PageDomain query) {
        if (userId == null) {
            throw new ServiceException("请登录后操作");
        }
        Page<PortalCodeRun> page = PageUtils.buildPage(query);
        LambdaQueryWrapper<PortalCodeRun> qw = Wrappers.<PortalCodeRun>lambdaQuery()
                .eq(PortalCodeRun::getUserId, userId)
                .orderByDesc(PortalCodeRun::getCreateTime);
        return codeRunMapper.selectPage(page, qw);
    }

    @Override
    public PortalCodeRun getRunDetail(Long id, Long userId) {
        if (userId == null) {
            throw new ServiceException("请登录后操作");
        }
        PortalCodeRun record = codeRunMapper.selectById(id);
        if (record == null) {
            return null;
        }
        // 仅允许读取本人运行记录，避免越权获取他人代码
        if (!userId.equals(record.getUserId())) {
            throw new ServiceException("无权查看该运行记录");
        }
        return record;
    }
}
