package com.moyun.ext.cms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moyun.common.exception.system.ServiceException;
import com.moyun.ext.cms.domain.query.UserResumeQuery;
import com.moyun.ext.cms.domain.vo.UserResumeVO;
import com.moyun.ext.cms.service.IUserResumeService;
import com.moyun.ext.cms.service.ResumePdfExporter;
import com.moyun.ext.cms.service.ResumeScoringService;
import com.moyun.portal.domain.entity.PortalUserResume;
import com.moyun.portal.mapper.PortalUserResumeMapper;
import com.moyun.util.string.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户简历 Service 实现（面试空间第2期）
 *
 * @author moyun
 */
@Service
public class UserResumeServiceImpl implements IUserResumeService {

    private static final Logger log = LoggerFactory.getLogger(UserResumeServiceImpl.class);

    @Autowired private PortalUserResumeMapper userResumeMapper;
    @Autowired private ResumeScoringService scoringService;
    @Autowired private ResumePdfExporter pdfExporter;
    @Autowired private ObjectMapper objectMapper;

    // ========================================================================
    // 列表 / 详情
    // ========================================================================
    @Override
    public Page<UserResumeVO> selectMyResumePage(Page<UserResumeVO> page, Long userId, UserResumeQuery query) {
        LambdaQueryWrapper<PortalUserResume> qw = Wrappers.lambdaQuery();
        qw.eq(PortalUserResume::getUserId, userId);
        // 默认排除已归档；若用户显式查询 archived，则精确匹配 archived（否则 ne+eq 恒为空）
        boolean onlyArchived = query != null && "archived".equals(query.getStatus());
        if (!onlyArchived) {
            qw.ne(PortalUserResume::getStatus, "archived");
        }
        if (query != null) {
            if (StringUtils.isNotEmpty(query.getStatus())) {
                qw.eq(PortalUserResume::getStatus, query.getStatus());
            }
            if (StringUtils.isNotEmpty(query.getKeyword())) {
                qw.like(PortalUserResume::getTitle, query.getKeyword());
            }
        }
        qw.orderByDesc(PortalUserResume::getUpdateTime);

        Page<PortalUserResume> entityPage = new Page<>(page.getCurrent(), page.getSize());
        userResumeMapper.selectPage(entityPage, qw);
        List<UserResumeVO> vos = entityPage.getRecords().stream()
                .map(this::toVO)
                .peek(vo -> vo.setMine(true))
                .collect(Collectors.toList());
        page.setRecords(vos);
        page.setTotal(entityPage.getTotal());
        return page;
    }

    @Override
    public UserResumeVO selectResumeDetail(Long id, Long userId) {
        PortalUserResume entity = userResumeMapper.selectById(id);
        if (entity == null) return null;
        if (!entity.getUserId().equals(userId)) {
            throw new ServiceException("无权访问该简历");
        }
        UserResumeVO vo = toVO(entity);
        vo.setMine(true);
        return vo;
    }

    // ========================================================================
    // 保存（新增/更新）
    // ========================================================================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveResume(UserResumeVO vo, Long userId) {
        if (userId == null) throw new ServiceException("请登录后操作");
        if (vo == null) throw new ServiceException("简历内容不能为空");

        PortalUserResume entity;
        boolean isNew = vo.getId() == null || vo.getId() <= 0;
        if (isNew) {
            entity = new PortalUserResume();
            entity.setUserId(userId);
            entity.setVersionNo(1);
            entity.setParentId(null);
            entity.setStatus("draft");
            entity.setCreateTime(LocalDateTime.now());
        } else {
            entity = userResumeMapper.selectById(vo.getId());
            if (entity == null) throw new ServiceException("简历不存在");
            if (!entity.getUserId().equals(userId)) throw new ServiceException("无权修改该简历");
            // 内容变更后，陈旧的评分与导出 PDF 自动失效，需重新评分/导出
            entity.setScore(null);
            entity.setScoreDetail(null);
            entity.setScoredTime(null);
            entity.setFileUrl(null);
            entity.setExportTime(null);
        }

        // 基本信息
        entity.setTitle(StringUtils.isNotEmpty(vo.getTitle()) ? vo.getTitle() : "我的简历");
        entity.setName(vo.getName());
        entity.setGender(vo.getGender());
        entity.setBirthDate(vo.getBirthDate());
        entity.setPhone(vo.getPhone());
        entity.setEmail(vo.getEmail());
        entity.setAvatar(vo.getAvatar());
        entity.setSelfIntro(vo.getSelfIntro());

        // 结构化内容序列化为 JSON
        entity.setJobIntention(toJson(vo.getJobIntention()));
        entity.setEducations(toJson(vo.getEducations()));
        entity.setWorks(toJson(vo.getWorks()));
        entity.setProjects(toJson(vo.getProjects()));
        entity.setSkills(toJson(vo.getSkills()));

        // 状态变更统一走 updateStatus 端点，saveResume 不接受前端 status，避免绕过状态机
        entity.setUpdateTime(LocalDateTime.now());

        if (isNew) {
            userResumeMapper.insert(entity);
        } else {
            userResumeMapper.updateById(entity);
        }
        return entity.getId();
    }

    // ========================================================================
    // 删除
    // ========================================================================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteResume(Long id, Long userId) {
        PortalUserResume entity = userResumeMapper.selectById(id);
        if (entity == null) throw new ServiceException("简历不存在");
        if (!entity.getUserId().equals(userId)) throw new ServiceException("无权删除该简历");
        // 级联删除所有以当前简历为根的子版本，避免产生孤儿数据
        LambdaQueryWrapper<PortalUserResume> childQw = Wrappers.<PortalUserResume>lambdaQuery()
                .eq(PortalUserResume::getParentId, id);
        userResumeMapper.delete(childQw);
        return userResumeMapper.deleteById(id);
    }

    // ========================================================================
    // 版本历史 / 复制为新版本
    // ========================================================================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long copyResumeAsNewVersion(Long id, Long userId) {
        PortalUserResume source = userResumeMapper.selectById(id);
        if (source == null) throw new ServiceException("简历不存在");
        if (!source.getUserId().equals(userId)) throw new ServiceException("无权操作该简历");

        Long rootId = source.getParentId() == null ? source.getId() : source.getParentId();
        List<PortalUserResume> versions = userResumeMapper.selectVersionHistory(rootId);
        int maxVersion = 0;
        for (PortalUserResume v : versions) {
            if (v.getVersionNo() != null && v.getVersionNo() > maxVersion) {
                maxVersion = v.getVersionNo();
            }
        }

        PortalUserResume copy = new PortalUserResume();
        copy.setUserId(userId);
        // 标题最长 100 字符（SQL VARCHAR(100)），预留 " (副本)" 5 字符
        String srcTitle = source.getTitle();
        String copyTitle = (srcTitle.length() > 95 ? srcTitle.substring(0, 95) : srcTitle) + " (副本)";
        copy.setTitle(copyTitle);
        copy.setParentId(rootId);
        copy.setVersionNo(maxVersion + 1);
        copy.setName(source.getName());
        copy.setGender(source.getGender());
        copy.setBirthDate(source.getBirthDate());
        copy.setPhone(source.getPhone());
        copy.setEmail(source.getEmail());
        copy.setAvatar(source.getAvatar());
        copy.setJobIntention(source.getJobIntention());
        copy.setEducations(source.getEducations());
        copy.setWorks(source.getWorks());
        copy.setProjects(source.getProjects());
        copy.setSkills(source.getSkills());
        copy.setSelfIntro(source.getSelfIntro());
        copy.setStatus("draft");
        copy.setCreateTime(LocalDateTime.now());
        copy.setUpdateTime(LocalDateTime.now());

        userResumeMapper.insert(copy);
        return copy.getId();
    }

    @Override
    public List<UserResumeVO> selectVersionHistory(Long id, Long userId) {
        PortalUserResume entity = userResumeMapper.selectById(id);
        if (entity == null) return Collections.emptyList();
        if (!entity.getUserId().equals(userId)) throw new ServiceException("无权访问该简历");

        Long rootId = entity.getParentId() == null ? entity.getId() : entity.getParentId();
        List<PortalUserResume> versions = userResumeMapper.selectVersionHistory(rootId);
        return versions.stream().map(this::toVO).peek(vo -> vo.setMine(true)).collect(Collectors.toList());
    }

    // ========================================================================
    // PDF 导出（不使用 @Transactional：PDF 文件生成涉及字体加载/磁盘 IO，耗时较长，
    // 不应占用 DB 连接；仅 updateById 自带事务回填 fileUrl/exportTime）
    // ========================================================================
    @Override
    public UserResumeVO exportResumePdf(Long id, Long userId) {
        PortalUserResume entity = userResumeMapper.selectById(id);
        if (entity == null) throw new ServiceException("简历不存在");
        if (!entity.getUserId().equals(userId)) throw new ServiceException("无权导出该简历");

        UserResumeVO vo = toVO(entity);
        String diskUrl = pdfExporter.exportToPdfUrl(vo);
        if (diskUrl == null) {
            throw new ServiceException("PDF 导出失败，服务器未配置中文字体或简历内容异常，请联系管理员");
        }
        // DB 存储磁盘可访问路径（内部用于读取文件流），返回前端的是需认证的下载端点
        entity.setFileUrl(diskUrl);
        entity.setExportTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        userResumeMapper.updateById(entity);

        // 返回给前端的 fileUrl 为认证下载端点，避免 PDF 经 /profile/** 公开访问泄露隐私
        vo.setFileUrl("/portal/interview/resume/user/file/" + entity.getId());
        vo.setExportTime(entity.getExportTime());
        vo.setMine(true);
        return vo;
    }

    /**
     * 读取简历 PDF 文件磁盘路径（供 Controller 认证下载端点调用）。
     * 已校验归属；返回 null 表示无导出文件或文件丢失。
     */
    @Override
    public String getResumePdfDiskPath(Long id, Long userId) {
        PortalUserResume entity = userResumeMapper.selectById(id);
        if (entity == null) return null;
        if (!entity.getUserId().equals(userId)) return null;
        return entity.getFileUrl();
    }

    // ========================================================================
    // 评分
    // ========================================================================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserResumeVO scoreResume(Long id, Long userId) {
        PortalUserResume entity = userResumeMapper.selectById(id);
        if (entity == null) throw new ServiceException("简历不存在");
        if (!entity.getUserId().equals(userId)) throw new ServiceException("无权评分该简历");

        UserResumeVO vo = toVO(entity);
        List<UserResumeVO.ScoreItem> items = scoringService.score(vo);
        int total = scoringService.total(items);

        entity.setScore(total);
        entity.setScoreDetail(toJson(items));
        entity.setScoredTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        userResumeMapper.updateById(entity);

        vo.setScore(total);
        vo.setScoreDetail(items);
        vo.setScoredTime(entity.getScoredTime());
        vo.setMine(true);
        return vo;
    }

    // ========================================================================
    // 状态更新
    // ========================================================================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateStatus(Long id, Long userId, String status) {
        if (!"draft".equals(status) && !"published".equals(status) && !"archived".equals(status)) {
            throw new ServiceException("非法状态值");
        }
        PortalUserResume entity = userResumeMapper.selectById(id);
        if (entity == null) throw new ServiceException("简历不存在");
        if (!entity.getUserId().equals(userId)) throw new ServiceException("无权修改该简历");
        entity.setStatus(status);
        entity.setUpdateTime(LocalDateTime.now());
        return userResumeMapper.updateById(entity);
    }

    // ========================================================================
    // 实体 → VO 转换（含 JSON 反序列化）
    // ========================================================================
    private UserResumeVO toVO(PortalUserResume entity) {
        if (entity == null) return null;
        UserResumeVO vo = new UserResumeVO();
        vo.setId(entity.getId());
        vo.setUserId(entity.getUserId());
        vo.setTitle(entity.getTitle());
        vo.setParentId(entity.getParentId());
        vo.setVersionNo(entity.getVersionNo());
        vo.setName(entity.getName());
        vo.setGender(entity.getGender());
        vo.setBirthDate(entity.getBirthDate());
        vo.setPhone(entity.getPhone());
        vo.setEmail(entity.getEmail());
        vo.setAvatar(entity.getAvatar());
        vo.setSelfIntro(entity.getSelfIntro());
        vo.setStatus(entity.getStatus());
        vo.setScore(entity.getScore());
        vo.setScoredTime(entity.getScoredTime());
        vo.setFileUrl(entity.getFileUrl());
        vo.setExportTime(entity.getExportTime());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());

        // JSON → 强类型
        vo.setJobIntention(fromJson(entity.getJobIntention(), UserResumeVO.JobIntention.class));
        vo.setEducations(fromJsonList(entity.getEducations(), UserResumeVO.EducationItem.class));
        vo.setWorks(fromJsonList(entity.getWorks(), UserResumeVO.WorkItem.class));
        vo.setProjects(fromJsonList(entity.getProjects(), UserResumeVO.ProjectItem.class));
        vo.setSkills(fromJsonList(entity.getSkills(), UserResumeVO.SkillItem.class));
        vo.setScoreDetail(fromJsonList(entity.getScoreDetail(), UserResumeVO.ScoreItem.class));
        return vo;
    }

    // ==================== JSON 工具 ====================

    private String toJson(Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.warn("JSON 序列化失败: {}", e.getMessage());
            return null;
        }
    }

    private <T> T fromJson(String json, Class<T> clazz) {
        if (StringUtils.isEmpty(json)) return null;
        try {
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            log.warn("JSON 反序列化失败 [{}]: {}", clazz.getSimpleName(), e.getMessage());
            return null;
        }
    }

    private <T> List<T> fromJsonList(String json, Class<T> clazz) {
        if (StringUtils.isEmpty(json)) return new ArrayList<>();
        try {
            return objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, clazz));
        } catch (Exception e) {
            log.warn("JSON List 反序列化失败 [{}]: {}", clazz.getSimpleName(), e.getMessage());
            return new ArrayList<>();
        }
    }
}
