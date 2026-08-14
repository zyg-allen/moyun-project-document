package com.moyun.portal.util;

import com.moyun.common.exception.system.ServiceException;
import com.moyun.portal.domain.entity.PortalUser;
import com.moyun.portal.mapper.PortalUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 创作者权限校验器
 * <p>
 * 统一校验"认证创作者"权限，供文章/专栏/话题/面经等高价值创作入口调用。
 * <p>
 * 权限模型（三层）：
 * <ul>
 *     <li>游客：仅浏览</li>
 *     <li>登录用户：可评论、点赞、收藏、关注等低风险互动</li>
 *     <li>认证创作者（is_certified_creator=1）：可发布文章、创建专栏、发起话题、发布面经等高价值创作</li>
 * </ul>
 * 本校验器用于第三层的创作类接口。
 *
 * @author moyun
 */
@Component
public class CreatorPermissionChecker {

    private final PortalUserMapper portalUserMapper;

    @Autowired
    public CreatorPermissionChecker(PortalUserMapper portalUserMapper) {
        this.portalUserMapper = portalUserMapper;
    }

    /**
     * 校验指定用户是否为认证创作者，未认证则抛出 ServiceException。
     * <p>
     * 用法：在创作类接口的 service 方法开头调用
     * <pre>{@code
     * creatorPermissionChecker.checkCreator(userId);
     * }</pre>
     *
     * @param userId 当前登录用户ID（不可为 null，调用方应先校验登录态）
     * @throws ServiceException 未登录 / 用户不存在 / 未认证创作者
     */
    public void checkCreator(Long userId) {
        if (userId == null) {
            throw new ServiceException("请先登录");
        }
        PortalUser user = portalUserMapper.selectById(userId);
        if (user == null) {
            throw new ServiceException("用户不存在");
        }
        Integer isCertified = user.getIsCertifiedCreator();
        if (isCertified == null || isCertified != 1) {
            throw new ServiceException("该操作需要创作者认证，请先完成认证后再发布");
        }
    }
}
