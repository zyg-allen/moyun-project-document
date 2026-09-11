package com.moyun.core.aspectj;

import com.moyun.common.annotation.RateLimiter;
import com.moyun.common.enums.LimitType;
import com.moyun.common.exception.system.ServiceException;
import com.moyun.core.base.model.LoginUser;
import com.moyun.portal.domain.model.PortalLoginUser;
import com.moyun.util.ip.IpUtils;
import com.moyun.util.string.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

/**
 * 限流处理
 *
 * @author allen-zyg
 */
@Aspect
@Slf4j
@Component
public class RateLimiterAspect {

    private RedisTemplate<String, Object> redisTemplate;

    private RedisScript<Long> limitScript;

    @Autowired
    public void setRedisTemplate(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Autowired
    public void setLimitScript(RedisScript<Long> limitScript) {
        this.limitScript = limitScript;
    }

    @Before("@annotation(rateLimiter)")
    public void doBefore(JoinPoint point, RateLimiter rateLimiter) throws Throwable {
        int time = rateLimiter.time();
        int count = rateLimiter.count();

        String combineKey = getCombineKey(rateLimiter, point);
        List<String> keys = Collections.singletonList(combineKey);
        try {
            Long number = redisTemplate.execute(limitScript, keys, count, time);
            if (StringUtils.isNull(number) || number.intValue() > count) {
                throw new ServiceException("访问过于频繁，请稍候再试");
            }
            log.debug("限制请求'{}',当前请求'{}',缓存key'{}'", count, number.intValue(), combineKey);
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("服务器限流异常，请稍候再试");
        }
    }

    public String getCombineKey(RateLimiter rateLimiter, JoinPoint point) {
        StringBuilder stringBuilder = new StringBuilder(rateLimiter.key());
        if (rateLimiter.limitType() == LimitType.IP) {
            stringBuilder.append(IpUtils.getIpAddr()).append("-");
        } else {
            // 默认模式按登录用户限流：不同用户各自计数，避免全站共享计数器互相误伤
            Long userId = resolveUserId();
            if (userId != null) {
                stringBuilder.append("u").append(userId).append("-");
            }
        }
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        Class<?> targetClass = method.getDeclaringClass();
        stringBuilder.append(targetClass.getName()).append("-").append(method.getName());
        return stringBuilder.toString();
    }

    /**
     * 解析当前登录用户ID（后台 LoginUser / 门户 PortalLoginUser），未登录返回 null
     */
    private Long resolveUserId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null) {
                Object principal = authentication.getPrincipal();
                if (principal instanceof LoginUser loginUser) {
                    return loginUser.getUserId();
                }
                if (principal instanceof PortalLoginUser portalLoginUser) {
                    return portalLoginUser.getId();
                }
            }
        } catch (Exception e) {
            log.debug("限流键获取用户信息失败，退回共享计数", e);
        }
        return null;
    }
}