package com.moyun.portal.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moyun.portal.domain.entity.PortalEntityTag;
import com.moyun.portal.domain.entity.PortalTag;
import com.moyun.portal.domain.query.TagQuery;
import com.moyun.portal.domain.vo.TagVO;
import com.moyun.portal.mapper.PortalEntityTagMapper;
import com.moyun.portal.mapper.PortalTagMapper;
import com.moyun.portal.service.IPortalTagService;

@Service
public class PortalTagServiceImpl extends ServiceImpl<PortalTagMapper, PortalTag> implements IPortalTagService {

    @Autowired
    private PortalTagMapper portalTagMapper;

    @Autowired
    private PortalEntityTagMapper portalEntityTagMapper;

    @Override
    public Page<PortalTag> selectPortalTagPage(Page<PortalTag> page, TagQuery query) {
        return baseMapper.selectPortalTagPage(page, query);
    }

    @Override
    public List<PortalTag> selectPortalTagList(TagQuery query) {
        return baseMapper.selectPortalTagList(query);
    }

    @Override
    public PortalTag selectPortalTagById(Long id) {
        return portalTagMapper.selectPortalTagById(id);
    }

    @Override
    public int insertPortalTag(PortalTag portalTag) {
        if (portalTag.getReferenceCount() == null) {
            portalTag.setReferenceCount(0L);
        }
        return portalTagMapper.insertPortalTag(portalTag);
    }

    @Override
    public int updatePortalTag(PortalTag portalTag) {
        return portalTagMapper.updatePortalTag(portalTag);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deletePortalTagById(Long id) {
        portalEntityTagMapper.delete(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<PortalEntityTag>()
                        .eq("tag_id", id));
        return portalTagMapper.deletePortalTagById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deletePortalTagByIds(Long[] ids) {
        if (ids == null || ids.length == 0) return 0;
        List<Long> idList = java.util.Arrays.asList(ids);
        portalEntityTagMapper.delete(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<PortalEntityTag>()
                        .in("tag_id", idList));
        return portalTagMapper.deletePortalTagByIds(ids);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bindTags(String entityType, Long entityId, List<Long> tagIds, List<String> tagNames, String module) {
        if (entityType == null || entityId == null) return;

        // 1) 收集最终需要绑定的 tagId 集合
        List<Long> finalIds = new ArrayList<>();
        if (tagIds != null) {
            for (Long id : tagIds) {
                if (id != null && id > 0) finalIds.add(id);
            }
        }
        if (tagNames != null && !tagNames.isEmpty()) {
            for (String rawName : tagNames) {
                if (rawName == null) continue;
                String name = rawName.trim();
                if (name.isEmpty()) continue;
                PortalTag tag = portalTagMapper.selectByNameAndModule(name, module);
                if (tag == null) {
                    tag = new PortalTag();
                    tag.setName(name);
                    tag.setModule(module);
                    tag.setReferenceCount(0L);
                    tag.setStatus("0");
                    tag.setCreateTime(LocalDateTime.now());
                    portalTagMapper.insertPortalTag(tag);
                }
                finalIds.add(tag.getId());
            }
        }

        // 去重
        finalIds = new ArrayList<>(new HashSet<>(finalIds));

        // 2) 查询旧绑定
        List<Long> oldIds = portalEntityTagMapper.selectTagIdsByEntity(entityType, entityId);
        if (oldIds == null) oldIds = Collections.emptyList();

        // 3) 删除旧绑定
        portalEntityTagMapper.deleteByEntity(entityType, entityId);

        // 4) 新增新绑定
        if (!finalIds.isEmpty()) {
            LocalDateTime now = LocalDateTime.now();
            List<PortalEntityTag> list = new ArrayList<>(finalIds.size());
            int sort = 0;
            for (Long tagId : finalIds) {
                PortalEntityTag e = new PortalEntityTag();
                e.setTagId(tagId);
                e.setEntityType(entityType);
                e.setEntityId(entityId);
                e.setSort(sort++);
                e.setCreateTime(now);
                list.add(e);
            }
            portalEntityTagMapper.insertBatch(list);
        }

        // 5) 维护 referenceCount：旧集合 -1，新集合 +1（避免对未变动的 tag 造成重复加减）
        HashSet<Long> oldSet = new HashSet<>(oldIds);
        HashSet<Long> newSet = new HashSet<>(finalIds);

        List<Long> decrementIds = oldSet.stream().filter(id -> !newSet.contains(id)).collect(Collectors.toList());
        List<Long> incrementIds = newSet.stream().filter(id -> !oldSet.contains(id)).collect(Collectors.toList());

        if (!decrementIds.isEmpty()) {
            portalTagMapper.updateReferenceCountBatch(decrementIds, -1L);
        }
        if (!incrementIds.isEmpty()) {
            portalTagMapper.updateReferenceCountBatch(incrementIds, 1L);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unbindTags(String entityType, Long entityId) {
        if (entityType == null || entityId == null) return;
        List<Long> oldIds = portalEntityTagMapper.selectTagIdsByEntity(entityType, entityId);
        portalEntityTagMapper.deleteByEntity(entityType, entityId);
        if (oldIds != null && !oldIds.isEmpty()) {
            portalTagMapper.updateReferenceCountBatch(new ArrayList<>(new HashSet<>(oldIds)), -1L);
        }
    }

    @Override
    public List<TagVO> getTagsByEntity(String entityType, Long entityId) {
        if (entityType == null || entityId == null) return Collections.emptyList();
        List<PortalTag> list = portalTagMapper.selectTagsByEntity(entityType, entityId);
        return toTagVOList(list);
    }

    @Override
    public List<TagVO> getHotTags(String module, int limit) {
        if (limit <= 0) limit = 20;
        if (limit > 200) limit = 200;
        List<PortalTag> list = portalTagMapper.selectHotTags(module, limit);
        return toTagVOList(list);
    }

    private List<TagVO> toTagVOList(List<PortalTag> list) {
        if (list == null) return Collections.emptyList();
        return list.stream().map(this::toTagVO).collect(Collectors.toList());
    }

    private TagVO toTagVO(PortalTag tag) {
        TagVO vo = new TagVO();
        vo.setId(tag.getId());
        vo.setName(tag.getName());
        vo.setSlug(tag.getSlug());
        vo.setSort(tag.getSort());
        vo.setStatus(tag.getStatus());
        vo.setModule(tag.getModule());
        vo.setReferenceCount(tag.getReferenceCount());
        return vo;
    }
}
