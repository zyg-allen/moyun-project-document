package com.moyun.core.config;

import com.moyun.common.config.RuoYiConfig;
import com.moyun.common.constant.Constants;
import com.moyun.core.mvc.interceptors.RepeatSubmitInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;
import java.util.concurrent.TimeUnit;

/**
 * 通用配置
 *
 * @author allen-zyg
 */
@Configuration
public class ResourcesConfig implements WebMvcConfigurer
{
    private static final Logger log = LoggerFactory.getLogger(ResourcesConfig.class);

    @Autowired
    private RepeatSubmitInterceptor repeatSubmitInterceptor;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry)
    {
        /** 本地文件上传路径 —— 始终使用绝对路径，防止相对路径被解析到 Tomcat work 目录 */
        String profilePath = RuoYiConfig.getProfile();
        File profileDir = new File(profilePath);
        if (!profileDir.isAbsolute()) {
            profilePath = profileDir.getAbsolutePath();
        }
        registry.addResourceHandler(Constants.RESOURCE_PREFIX + "/**")
                .addResourceLocations("file:" + profilePath + "/");

        /** swagger配置 */
        registry.addResourceHandler("/swagger-ui/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/springfox-swagger-ui/")
                .setCacheControl(CacheControl.maxAge(5, TimeUnit.HOURS).cachePublic());
        
        /** knife4j文档配置 */
        registry.addResourceHandler("doc.html")
                .addResourceLocations("classpath:/META-INF/resources/");
        
        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/");
    }

    /**
     * 自定义拦截规则
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry)
    {
        registry.addInterceptor(repeatSubmitInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/doc.html", "/swagger-ui/**", "/v3/api-docs/**", "/webjars/**", "/swagger-resources/**");
    }

    /**
     * 跨域配置
     *
     * <p>通过环境变量 {@code CORS_ALLOWED_ORIGINS}（逗号分隔）配置允许的源；未配置时只在**非生产**使用本地开发白名单。</p>
     *
     * <p>内网 Origin 按**真实私网地址逐条精确放行**（解析 Origin 的 host，必须是 IPv4 私有段
     * 172.16/12、192.168/16、10/8 的字面量，或本机回环），且必须显式开启
     * {@code CORS_ALLOW_LAN_DEV_ORIGINS=true} **且**非生产 profile；生产未配置环境变量时**不放行任何跨域源**（fail-closed）。</p>
     */
    @Bean
    public CorsFilter corsFilter()
    {
        String envOrigins = System.getenv("CORS_ALLOWED_ORIGINS");
        boolean lanDevAllowed = isLanDevOriginsAllowed();
        boolean prod = isProdProfile();
        CorsConfiguration config = buildCorsConfiguration(envOrigins, prod);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource()
        {
            @Override
            public CorsConfiguration getCorsConfiguration(jakarta.servlet.http.HttpServletRequest request)
            {
                CorsConfiguration base = super.getCorsConfiguration(request);
                if (base == null || !lanDevAllowed)
                {
                    return base;
                }
                // 开发期手机/局域网设备经 vite dev server 访问：按"真实私网 IP 字面量"精确放行当前 Origin，
                // 不使用 * 通配 pattern（避免 http://192.168.evil.com 这类可注册域名被匹配）
                String origin = request.getHeader(org.springframework.http.HttpHeaders.ORIGIN);
                if (origin != null && isPrivateNetworkOrigin(origin))
                {
                    CorsConfiguration copy = new CorsConfiguration(base);
                    copy.addAllowedOrigin(origin);
                    log.debug("CORS 放行局域网开发 Origin：{}", origin);
                    return copy;
                }
                return base;
            }
        };
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }

    /**
     * 构建基础跨域配置（可单测：不依赖环境变量与 profile）
     *
     * @param envOrigins     {@code CORS_ALLOWED_ORIGINS} 的原始值（逗号分隔，可空）
     * @param prodProfile    是否生产 profile
     */
    static CorsConfiguration buildCorsConfiguration(String envOrigins, boolean prodProfile)
    {
        CorsConfiguration config = new CorsConfiguration();
        if (envOrigins != null && !envOrigins.trim().isEmpty())
        {
            for (String origin : envOrigins.split(","))
            {
                String trimmed = origin.trim();
                if (!trimmed.isEmpty())
                {
                    config.addAllowedOrigin(trimmed);
                }
            }
            log.info("CORS allowed origins 已从环境变量加载：{}", config.getAllowedOrigins());
        }
        else if (!prodProfile)
        {
            // 本地开发默认白名单：只放行**本机回环**（含端口通配——回环 pattern 的 host 段是固定的，
            // http://localhost:* 不会匹配 http://localhost.evil.com，故不存在内网 pattern 那类伪造问题）。
            // 局域网设备走 getCorsConfiguration 的私网精确放行（需显式开关）。
            config.addAllowedOriginPattern("http://localhost:*");
            config.addAllowedOrigin("http://localhost");
            config.addAllowedOriginPattern("http://127.0.0.1:*");
            config.addAllowedOrigin("http://127.0.0.1");
            config.addAllowedOriginPattern("https://localhost:*");
            config.addAllowedOrigin("https://localhost");
            log.info("CORS 未配置 CORS_ALLOWED_ORIGINS，使用本机开发白名单：{}", config.getAllowedOriginPatterns());
        }
        else
        {
            // 生产 fail-closed：不放行任何跨域源（同源部署不受影响）
            log.error("CORS 未配置 CORS_ALLOWED_ORIGINS 且当前为生产 profile：已 fail-closed，不放行任何跨域源");
        }
        // 设置访问源请求头 / 方法
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        // 允许携带 Cookie（正是因此，源白名单必须严格）
        config.setAllowCredentials(true);
        // 有效期 1800秒
        config.setMaxAge(1800L);
        return config;
    }

    /** 内网放行开关：显式环境变量开启，且非生产 profile */
    private static boolean isLanDevOriginsAllowed()
    {
        boolean enabled = "true".equalsIgnoreCase(String.valueOf(System.getenv("CORS_ALLOW_LAN_DEV_ORIGINS")).trim());
        return enabled && !isProdProfile();
    }

    /**
     * WebSocket 握手 Origin 校验（供 {@code PortalWebSocketAuthInterceptor} 调用）
     *
     * <p>WebSocket 不受同源策略保护，浏览器允许任意站点发起握手，因此服务端必须自己判定 Origin。
     * 判定口径与 CORS 保持一致：</p>
     * <ul>
     *   <li>**无 Origin**（小程序 / 原生 / 服务端客户端）→ 放行，交由票据或 Authorization 鉴权；</li>
     *   <li>回环（{@code localhost}/{@code 127.0.0.1}/{@code ::1}，含任意端口）→ 放行；</li>
     *   <li>环境变量 {@code CORS_ALLOWED_ORIGINS} 里的精确源 → 放行；</li>
     *   <li>显式开启 {@code CORS_ALLOW_LAN_DEV_ORIGINS=true} 且非生产时的**私网 IP 字面量** → 放行；</li>
     *   <li>其余（含 {@code http://192.168.evil.com} 这类"像内网"的可注册域名）→ 拒绝。</li>
     * </ul>
     */
    public static boolean isWsOriginAllowed(String origin)
    {
        if (origin == null || origin.isBlank())
        {
            return true;
        }
        try
        {
            java.net.URI uri = java.net.URI.create(origin);
            String host = uri.getHost();
            if (host != null && ("localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host) || "::1".equals(host)))
            {
                return true;
            }
        }
        catch (Exception ignored)
        {
            return false;
        }
        String envOrigins = System.getenv("CORS_ALLOWED_ORIGINS");
        if (envOrigins != null && !envOrigins.trim().isEmpty())
        {
            for (String allowed : envOrigins.split(","))
            {
                if (allowed.trim().equals(origin))
                {
                    return true;
                }
            }
        }
        return isLanDevOriginsAllowed() && isPrivateNetworkOrigin(origin);
    }

    /** Origin 的 host 是否为**私网 IP 字面量**或回环（拒绝任何域名，杜绝 {@code 192.168.evil.com} 这类伪造） */
    public static boolean isPrivateNetworkOrigin(String origin)
    {
        try
        {
            java.net.URI uri = java.net.URI.create(origin);
            String host = uri.getHost();
            if (host == null)
            {
                return false;
            }
            if ("localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host) || "::1".equals(host))
            {
                return true;
            }
            // 必须是 IPv4 字面量（含域名一律拒绝）
            java.util.regex.Matcher m = java.util.regex.Pattern
                    .compile("^(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})$").matcher(host);
            if (!m.matches())
            {
                return false;
            }
            int a = Integer.parseInt(m.group(1));
            int b = Integer.parseInt(m.group(2));
            if (a > 255 || b > 255 || Integer.parseInt(m.group(3)) > 255 || Integer.parseInt(m.group(4)) > 255)
            {
                return false;
            }
            return a == 10                                  // 10.0.0.0/8
                    || (a == 172 && b >= 16 && b <= 31)     // 172.16.0.0/12
                    || (a == 192 && b == 168);              // 192.168.0.0/16
        }
        catch (Exception e)
        {
            return false;
        }
    }

    private static boolean isProdProfile()
    {
        String profiles = System.getenv("SPRING_PROFILES_ACTIVE");
        if (profiles == null || profiles.isBlank())
        {
            profiles = System.getProperty("spring.profiles.active", "");
        }
        for (String p : String.valueOf(profiles).split(","))
        {
            String trimmed = p.trim().toLowerCase();
            if ("prod".equals(trimmed) || "production".equals(trimmed))
            {
                return true;
            }
        }
        return false;
    }
}