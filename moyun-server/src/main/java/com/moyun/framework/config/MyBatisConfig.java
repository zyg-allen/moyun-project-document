package com.moyun.framework.config;

import com.baomidou.mybatisplus.autoconfigure.SpringBootVFS;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.moyun.common.utils.StringUtils;
import org.apache.ibatis.io.VFS;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.util.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/**
 * Mybatis-Plus 配置类
 * 使用 MybatisSqlSessionFactoryBean 替代原生 SqlSessionFactoryBean
 * 以支持 MyBatis-Plus 的增强功能
 *
 * @author ruoyi
 */
@Configuration
public class MyBatisConfig {
    private static final Logger log = LoggerFactory.getLogger(MyBatisConfig.class);

    private final Environment env;

    static final String DEFAULT_RESOURCE_PATTERN = "**/*.class";

    public MyBatisConfig(Environment env) {
        this.env = env;
    }

    public String setTypeAliasesPackage(String typeAliasesPackage) {
        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        MetadataReaderFactory metadataReaderFactory = new CachingMetadataReaderFactory(resolver);
        List<String> allResult = new ArrayList<String>();
        try {
            for (String aliasesPackage : typeAliasesPackage.split(",")) {
                List<String> result = new ArrayList<String>();
                aliasesPackage = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX
                        + ClassUtils.convertClassNameToResourcePath(aliasesPackage.trim()) + "/" + DEFAULT_RESOURCE_PATTERN;
                Resource[] resources = resolver.getResources(aliasesPackage);
                if (resources != null && resources.length > 0) {
                    for (Resource resource : resources) {
                        if (resource.isReadable()) {
                            try {
                                String className = metadataReaderFactory.getMetadataReader(resource).getClassMetadata().getClassName();
                                result.add(Class.forName(className).getPackage().getName());
                            } catch (ClassNotFoundException e) {
                                log.warn("Mybatis typeAliasesPackage 扫描错误: {}", e.getMessage());
                            }
                        }
                    }
                }
                if (result.size() > 0) {
                    HashSet<String> hashResult = new HashSet<String>(result);
                    allResult.addAll(hashResult);
                }
            }
            if (allResult.size() > 0) {
                typeAliasesPackage = String.join(",", allResult.toArray(new String[0]));
            } else {
                throw new RuntimeException("mybatis typeAliasesPackage 路径扫描错误,参数typeAliasesPackage:" + typeAliasesPackage + "未找到任何包");
            }
        } catch (IOException e) {
            log.error("Mybatis typeAliasesPackage 扫描异常", e);
            throw new RuntimeException("mybatis typeAliasesPackage 路径扫描异常", e);
        }
        return typeAliasesPackage;
    }

    public Resource[] resolveMapperLocations(String[] mapperLocations) {
        ResourcePatternResolver resourceResolver = new PathMatchingResourcePatternResolver();
        List<Resource> resources = new ArrayList<Resource>();
        if (mapperLocations != null) {
            for (String mapperLocation : mapperLocations) {
                try {
                    Resource[] mappers = resourceResolver.getResources(mapperLocation);
                    resources.addAll(Arrays.asList(mappers));
                } catch (IOException e) {
                    log.warn("Mybatis mapperLocation 扫描错误: {}", mapperLocation, e);
                }
            }
        }
        return resources.toArray(new Resource[0]);
    }

    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
        // 优先使用 mybatis-plus 配置，兼容 mybatis 配置
        String typeAliasesPackage = env.getProperty("mybatis-plus.type-aliases-package");
        if (StringUtils.isEmpty(typeAliasesPackage)) {
            typeAliasesPackage = env.getProperty("mybatis.typeAliasesPackage");
        }
        
        String mapperLocations = env.getProperty("mybatis-plus.mapper-locations");
        if (StringUtils.isEmpty(mapperLocations)) {
            mapperLocations = env.getProperty("mybatis.mapperLocations");
        }
        
        String configLocation = env.getProperty("mybatis-plus.config-location");
        if (StringUtils.isEmpty(configLocation)) {
            configLocation = env.getProperty("mybatis.configLocation");
        }

        if (StringUtils.isEmpty(typeAliasesPackage)) {
            throw new IllegalArgumentException("mybatis-plus.type-aliases-package 或 mybatis.typeAliasesPackage 配置不能为空");
        }

        typeAliasesPackage = setTypeAliasesPackage(typeAliasesPackage);
        VFS.addImplClass(SpringBootVFS.class);

        // 使用 MyBatis-Plus 的 SqlSessionFactoryBean
        final MybatisSqlSessionFactoryBean sessionFactory = new MybatisSqlSessionFactoryBean();
        sessionFactory.setDataSource(dataSource);
        sessionFactory.setTypeAliasesPackage(typeAliasesPackage);
        sessionFactory.setMapperLocations(resolveMapperLocations(StringUtils.split(mapperLocations, ",")));
        if (StringUtils.isNotEmpty(configLocation)) {
            sessionFactory.setConfigLocation(new DefaultResourceLoader().getResource(configLocation));
        }
        return sessionFactory.getObject();
    }
}
