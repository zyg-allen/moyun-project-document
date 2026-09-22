package com.moyun.vip.aspect;

import com.moyun.common.exception.system.ServiceException;
import com.moyun.portal.util.PortalSecurityUtils;
import com.moyun.vip.annotation.VipOnly;
import com.moyun.vip.domain.entity.VipApiRegistry;
import com.moyun.vip.mapper.VipApiRegistryMapper;
import com.moyun.vip.service.IVipService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

/**
 * {@link VipOnly} 运行时切面
 *
 * <p>校验链（按方案 §6）：
 * 1. 端级开关关闭 → 放行（灰度全员免费）
 * 2. 注册表该接口被后台禁用 → 放行（运营逃生口）
 * 3. 未登录 → 401
 * 4. 权益校验：consume=true 扣减次数，false 仅校验；不足 → 402（引导开通）
 *
 * <p>注册表路径匹配：先精确（含 {id} pattern 直存的接口无法用 URI 命中），
 * 再 Ant 通配兜底，保证带路径参数接口的后台禁用同样生效。
 *
 * @author moyun
 */
@Aspect
@Component
@Slf4j
public class VipOnlyAspect {


    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    @Autowired
    private IVipService vipService;

    @Autowired
    private VipApiRegistryMapper registryMapper;

    @Around("@annotation(vipOnly)")
    public Object check(ProceedingJoinPoint pjp, VipOnly vipOnly) throws Throwable {
        // 1. 开关关闭 → 放行
        if (!vipService.isVipEnabled(vipOnly.platform())) {
            return pjp.proceed();
        }

        // 2. 注册表后台禁用 → 放行
        if (isDisabledInRegistry(vipOnly.platform())) {
            return pjp.proceed();
        }

        // 3. 取用户
        Long userId = PortalSecurityUtils.getUserId();
        if (userId == null) {
            throw new ServiceException("请先登录后再使用会员功能", 401);
        }

        // 4. 校验/消耗权益
        boolean pass = vipOnly.consume()
                ? vipService.consumeBenefit(userId, vipOnly.platform(), vipOnly.benefit())
                : vipService.hasBenefit(userId, vipOnly.platform(), vipOnly.benefit());
        if (!pass) {
            throw new ServiceException(
                    vipOnly.message().isBlank() ? "权益不足，请开通会员" : vipOnly.message(), 402);
        }
        return pjp.proceed();
    }

    /** 当前请求是否被后台在注册表中禁用（enabled=0） */
    private boolean isDisabledInRegistry(String platformCode) {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) {
                return false;
            }
            HttpServletRequest request = attrs.getRequest();
            String uri = request.getRequestURI();
            String method = request.getMethod();

            // 精确命中
            VipApiRegistry exact = registryMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<VipApiRegistry>()
                            .eq(VipApiRegistry::getApiPath, uri)
                            .eq(VipApiRegistry::getHttpMethod, method)
                            .last("LIMIT 1"));
            if (exact != null) {
                return exact.getEnabled() != null && exact.getEnabled() == 0;
            }
            // Ant 通配兜底（带 {id} 等 path variable 的接口）
            List<VipApiRegistry> candidates = registryMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<VipApiRegistry>()
                            .eq(VipApiRegistry::getHttpMethod, method)
                            .eq(VipApiRegistry::getPlatformCode, platformCode));
            for (VipApiRegistry r : candidates) {
                if (PATH_MATCHER.match(r.getApiPath(), uri)) {
                    return r.getEnabled() != null && r.getEnabled() == 0;
                }
            }
        } catch (Exception e) {
            log.warn("[vip] 注册表查询失败，按未禁用处理", e);
        }
        return false;
    }
}
