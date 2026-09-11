package com.moyun.portal.util;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.moyun.common.exception.system.ServiceException;
import com.moyun.portal.domain.entity.PortalCreatorCertification;
import com.moyun.portal.mapper.PortalCreatorCertificationMapper;

/**
 * 实名权限校验器（v10.10 实名合规策略）
 *
 * <p>策略分层：
 * <ul>
 *     <li><b>强制实名</b>：打赏、积分兑换/消费等涉及资产变动的敏感场景，必须先通过身份实名认证</li>
 *     <li><b>提示实名</b>：发布文章/面经/专栏等创作行为不强制实名，由前端弹窗提示引导（可跳过）</li>
 * </ul>
 *
 * <p>实名判定：portal_creator_certification 中存在 cert_type=identity 且 status=approved 的记录。
 * 与 {@code RealNameVerifier}（核验渠道抽象）配合，后期接入第三方核验 API 后判定逻辑不变。</p>
 *
 * @author moyun
 */
@Component
public class RealNameChecker {

    @Autowired
    private PortalCreatorCertificationMapper certificationMapper;

    /**
     * 强制实名校验：未实名则抛出 ServiceException（打赏/积分消费等敏感场景调用）
     *
     * @param userId 当前登录用户ID（不可为 null）
     * @throws ServiceException 未登录 / 未完成实名认证
     */
    public void checkRealName(Long userId) {
        if (userId == null) {
            throw new ServiceException("请先登录");
        }
        if (!isRealNameVerified(userId)) {
            throw new ServiceException("该操作需要先完成实名认证，请前往「创作者认证」提交身份认证");
        }
    }

    /**
     * 查询用户是否已通过实名（身份）认证
     */
    public boolean isRealNameVerified(Long userId) {
        if (userId == null) {
            return false;
        }
        LambdaQueryWrapper<PortalCreatorCertification> qw = new LambdaQueryWrapper<>();
        qw.eq(PortalCreatorCertification::getUserId, userId)
                .eq(PortalCreatorCertification::getCertType, "identity")
                .eq(PortalCreatorCertification::getStatus, "approved")
                .last("LIMIT 1");
        List<PortalCreatorCertification> list = certificationMapper.selectList(qw);
        return list != null && !list.isEmpty();
    }
}
