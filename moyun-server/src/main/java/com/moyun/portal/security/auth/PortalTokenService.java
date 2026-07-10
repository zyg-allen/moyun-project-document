package com.moyun.portal.security.auth;

import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.crypto.SecretKey;

import eu.bitwalker.useragentutils.UserAgent;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.moyun.common.constant.CacheConstants;
import com.moyun.common.constant.Constants;
import com.moyun.core.config.redis.RedisCache;
import com.moyun.portal.domain.model.PortalLoginUser;
import com.moyun.util.http.ServletUtils;
import com.moyun.util.ip.AddressUtils;
import com.moyun.util.ip.IpUtils;
import com.moyun.util.string.StringUtils;
import com.moyun.util.uuid.IdUtils;

/**
 * 门户token验证处理
 *
 * @author moyun
 */
@Slf4j
@Component("portalTokenService")
public class PortalTokenService {

    // 令牌自定义标识
    @Value("${token.header}")
    private String header;

    // 令牌秘钥
    @Value("${token.secret:}")
    private String secret;

    // 令牌有效期（默认720分钟 - 12小时，门户可以设置稍长一些）
    @Value("${token.expireTime:720}")
    private int expireTime;

    protected static final long MILLIS_SECOND = 1000;

    protected static final long MILLIS_MINUTE = 60 * MILLIS_SECOND;

    private static final Long MILLIS_MINUTE_TWENTY = 20 * 60 * 1000L;

    @Autowired
    private RedisCache redisCache;

    /**
     * 获取用户身份信息
     *
     * @return 用户信息
     */
    public PortalLoginUser getLoginUser(HttpServletRequest request) {
        String token = getToken(request);
        log.debug("获取门户登录用户，Token: {}", token);
        if (StringUtils.isNotEmpty(token)) {
            try {
                Claims claims = parseToken(token);
                String uuid = (String) claims.get(Constants.LOGIN_USER_KEY);
                String userKey = getTokenKey(uuid);
                log.debug("解析Token成功，UUID: {}, UserKey: {}", uuid, userKey);
                PortalLoginUser user = redisCache.getCacheObject(userKey);
                if (user == null) {
                    log.warn("Redis中未找到门户用户信息，UserKey: {}", userKey);
                } else {
                    log.debug("从Redis获取门户用户成功: {}", user.getUsername());
                }
                return user;
            } catch (Exception e) {
                log.error("获取门户用户信息异常'{}'", e.getMessage(), e);
            }
        } else {
            log.debug("请求中未找到Token");
        }
        return null;
    }

    /**
     * 根据 token 字符串获取门户用户身份信息
     *
     * <p>用于 WebSocket 握手等无法直接从 HTTP 请求头获取 token 的场景（如 ?token=xxx 查询参数）。
     * 复用内部 parseToken + Redis 查询逻辑。</p>
     *
     * @param token 令牌（可带 Bearer 前缀）
     * @return 用户信息，token 为空或无效时返回 null
     */
    public PortalLoginUser getLoginUserByToken(String token) {
        if (StringUtils.isEmpty(token)) {
            return null;
        }
        if (token.startsWith(Constants.TOKEN_PREFIX)) {
            token = token.replace(Constants.TOKEN_PREFIX, "");
        }
        try {
            Claims claims = parseToken(token);
            String uuid = (String) claims.get(Constants.LOGIN_USER_KEY);
            String userKey = getTokenKey(uuid);
            return redisCache.getCacheObject(userKey);
        } catch (Exception e) {
            log.error("根据Token获取门户用户信息异常'{}'", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 设置用户身份信息
     */
    public void setLoginUser(PortalLoginUser loginUser) {
        if (StringUtils.isNotNull(loginUser) && StringUtils.isNotEmpty(loginUser.getToken())) {
            refreshToken(loginUser);
        }
    }

    /**
     * 删除用户身份信息
     */
    public void delLoginUser(String token) {
        if (StringUtils.isNotEmpty(token)) {
            String userKey = getTokenKey(token);
            redisCache.deleteObject(userKey);
        }
    }

    /**
     * 创建令牌
     *
     * @param loginUser 用户信息
     * @return 令牌
     */
    public String createToken(PortalLoginUser loginUser) {
        String token = IdUtils.fastUUID();
        loginUser.setToken(token);
        setUserAgent(loginUser);
        refreshToken(loginUser);

        Map<String, Object> claims = new HashMap<>();
        claims.put(Constants.LOGIN_USER_KEY, token);
        claims.put(Constants.JWT_USERNAME, loginUser.getUsername());
        claims.put("user_type", "portal"); // 标识为门户用户
        return createToken(claims);
    }

    /**
     * 验证令牌有效期，相差不足20分钟，自动刷新缓存
     *
     * @param loginUser 登录信息
     * @return 令牌
     */
    public void verifyToken(PortalLoginUser loginUser) {
        long expireTime = loginUser.getExpireTime();
        long currentTime = System.currentTimeMillis();
        if (expireTime - currentTime <= MILLIS_MINUTE_TWENTY) {
            refreshToken(loginUser);
        }
    }

    /**
     * 刷新令牌有效期
     *
     * @param loginUser 登录信息
     */
    public void refreshToken(PortalLoginUser loginUser) {
        loginUser.setLoginTime(System.currentTimeMillis());
        loginUser.setExpireTime(loginUser.getLoginTime() + expireTime * MILLIS_MINUTE);
        String userKey = getTokenKey(loginUser.getToken());
        redisCache.setCacheObject(userKey, loginUser, expireTime, TimeUnit.MINUTES);
    }

    /**
     * 设置用户代理信息
     *
     * @param loginUser 登录信息
     */
    public void setUserAgent(PortalLoginUser loginUser) {
        UserAgent userAgent = UserAgent.parseUserAgentString(ServletUtils.getRequest().getHeader("User-Agent"));
        String ip = IpUtils.getIpAddr();
        loginUser.setIpaddr(ip);
        loginUser.setLoginLocation(AddressUtils.getRealAddressByIP(ip));
        loginUser.setBrowser(userAgent.getBrowser().getName());
        loginUser.setOs(userAgent.getOperatingSystem().getName());
    }

    /**
     * 获取实际使用的密钥
     * <p>
     * 安全策略：fail-fast，TOKEN_SECRET 未配置或长度不足 64 字符（512位）时直接抛出
     * IllegalStateException 让应用启动失败，避免因疏忽启用弱密钥或回退到硬编码默认密钥。
     * </p>
     */
    private String getEffectiveSecret() {
        if (StringUtils.isEmpty(secret)) {
            throw new IllegalStateException("JWT密钥未配置，请设置环境变量 TOKEN_SECRET（至少64字符/512位，用于HS512算法）");
        }
        if (secret.length() < 64) {
            throw new IllegalStateException("JWT密钥长度不足64字符，HS512算法要求至少512位（64字节），请设置环境变量 TOKEN_SECRET（至少64字符），当前长度：" + secret.length());
        }
        return secret;
    }

    /**
     * 从数据声明生成令牌
     *
     * @param claims 数据声明
     * @return 令牌
     */
    private String createToken(Map<String, Object> claims) {
        SecretKey key = Keys.hmacShaKeyFor(getEffectiveSecret().getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .claims(claims)
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }

    /**
     * 从令牌中获取数据声明
     *
     * @param token 令牌
     * @return 数据声明
     */
    private Claims parseToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(getEffectiveSecret().getBytes(StandardCharsets.UTF_8));
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 从令牌中获取用户名
     *
     * @param token 令牌
     * @return 用户名
     */
    public String getUsernameFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.getSubject();
    }

    /**
     * 获取请求token
     *
     * @param request
     * @return token
     */
    private String getToken(HttpServletRequest request) {
        String token = request.getHeader(header);
        log.debug("从请求头获取门户Token，Header名称: {}，原始值: {}", header, token);
        if (StringUtils.isNotEmpty(token) && token.startsWith(Constants.TOKEN_PREFIX)) {
            token = token.replace(Constants.TOKEN_PREFIX, "");
            log.debug("移除Bearer前缀后的Token: {}", token);
        }
        return token;
    }

    private String getTokenKey(String uuid) {
        return CacheConstants.PORTAL_LOGIN_TOKEN_KEY + uuid;
    }
}