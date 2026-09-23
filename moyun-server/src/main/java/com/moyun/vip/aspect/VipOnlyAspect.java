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
import java.util.regex.Pattern;

/**
 * {@link VipOnly} 运行时切面
 *
 * <p>校验链（按方案 §6）：
 * 1. 端级开关关闭 → 放行（灰度全员免费）
 * 2. 注册表该接口被后台禁用 → 放行（运营逃生口）
 * 3. 未登录 → 401
 * 4. 权益校验：consume=true 扣减次数，false 仅校验；不足 → 402（引导开通）
 *
 * <p>注册表路径匹配：注册表统一存 pattern 化路径（{id} 形式，由 VipApiScanner 保证）。
 * 先精确命中（静态路径 pattern 与实际 URI 一致），未命中再把 pattern 中的
 * URI 模板变量（{id} / 存量 {id:[0-9]+}）折算为 Ant 通配 * 后用 AntPathMatcher 兜底，
 * 保证带路径参数接口（如 /portal/interview/voice/{id}/start）的后台禁用正确命中。
 *
 * @author moyun
 */
@Aspect
@Component
@Slf4j
public class VipOnlyAspect {


    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    /** 注册表 pattern 中的 URI 模板变量（{id} / 存量 {id:[0-9]+}）→ Ant 通配单段 * */
    private static final Pattern PATH_VAR_PATTERN = Pattern.compile("\\{[^/]+?\\}");

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
            // Ant 通配兜底（带 {id} 等 path variable 的接口：pattern 变量折算为 * 后匹配）
            List<VipApiRegistry> candidates = registryMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<VipApiRegistry>()
                            .eq(VipApiRegistry::getHttpMethod, method)
                            .eq(VipApiRegistry::getPlatformCode, platformCode));
            for (VipApiRegistry r : candidates) {
                if (PATH_MATCHER.match(toAntPattern(r.getApiPath()), uri)) {
                    return r.getEnabled() != null && r.getEnabled() == 0;
                }
            }
        } catch (Exception e) {
            log.warn("[vip] 注册表查询失败，按未禁用处理", e);
        }
        return false;
    }

    /** 注册表 pattern（含 {id} 等 URI 模板变量）→ Ant 通配 pattern（{var} → 单段 *） */
    private static String toAntPattern(String apiPath) {
        return PATH_VAR_PATTERN.matcher(apiPath).replaceAll("*");
    }
}
