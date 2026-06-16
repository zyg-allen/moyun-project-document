package com.moyun.portal.service;

import java.util.List;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.moyun.portal.domain.entity.PortalTag;
import com.moyun.portal.domain.query.TagQuery;
import com.moyun.portal.domain.vo.TagVO;

/**
 * 门户标签 业务层
 *
 * @author moyun
 */
public interface IPortalTagService {

    /**
     * 根据条件分页查询标签列表
     */
    Page<PortalTag> selectPortalTagPage(Page<PortalTag> page, TagQuery query);

    /**
     * 根据条件查询标签列表
     */
    List<PortalTag> selectPortalTagList(TagQuery query);

    PortalTag selectPortalTagById(Long id);

    int insertPortalTag(PortalTag portalTag);

    int updatePortalTag(PortalTag portalTag);

    int deletePortalTagById(Long id);

    int deletePortalTagByIds(Long[] ids);

    /**
     * 绑定标签到实体。
     *
     * @param entityType 实体类型（如 article / book / interview_question）
     * @param entityId   实体ID
     * @param tagIds     已存在的标签ID列表（可为 null/empty）
     * @param tagNames   通过名称提供的标签列表（不存在时会自动创建并绑定到指定 module）（可为 null/empty）
     * @param module     所属模块，用于当 tagNames 不存在时创建新标签时写入 module 字段（可为 null）
     */
    void bindTags(String entityType, Long entityId, List<Long> tagIds, List<String> tagNames, String module);

    /**
     * 解绑实体的全部标签
     */
    void unbindTags(String entityType, Long entityId);

    /**
     * 根据实体获取已绑定的标签 VO 列表
     */
    List<TagVO> getTagsByEntity(String entityType, Long entityId);

    /**
     * 获取热门标签（按 reference_count 降序）
     *
     * @param module 所属模块（可为 null 表示全部）
     * @param limit  返回数量
     */
    List<TagVO> getHotTags(String module, int limit);
}
