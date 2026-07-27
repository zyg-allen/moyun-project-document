package com.moyun.agent.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.moyun.agent.exception.BusinessException;
import com.moyun.agent.exception.ErrorCode;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Elasticsearch配置类
 *
 * <p>配置Elasticsearch 8.x客户端连接，用于向量存储和检索</p>
 *
 * @author laomao
 * @time 2025/11/23
 */
@Slf4j
@Configuration
public class ElasticsearchConfig {

    /** ES服务器地址 */
    @Value("${elasticsearch.host}")
    private String host;

    /** ES服务器端口 */
    @Value("${elasticsearch.port}")
    private int port;

    /** ES用户名 */
    @Value("${elasticsearch.username}")
    private String username;

    /** ES密码 */
    @Value("${elasticsearch.password}")
    private String password;

    /**
     * 创建Elasticsearch客户端
     *
     * @return ElasticsearchClient实例
     */
    @Bean
    public ElasticsearchClient elasticsearchClient() {
        try {
            // 创建凭证提供者
            BasicCredentialsProvider credsProv = new BasicCredentialsProvider();
            credsProv.setCredentials(
                AuthScope.ANY,
                new UsernamePasswordCredentials(username, password)
            );

            // 创建RestClient
            RestClient restClient = RestClient.builder(
                new HttpHost(host, port, "http")
            )
            .setHttpClientConfigCallback(httpClientBuilder ->
                httpClientBuilder.setDefaultCredentialsProvider(credsProv)
            )
            .build();

            // 创建传输层
            ElasticsearchTransport transport = new RestClientTransport(
                restClient,
                new JacksonJsonpMapper()
            );

            // 创建客户端
            ElasticsearchClient client = new ElasticsearchClient(transport);

            log.info("✅ Elasticsearch客户端初始化成功, 连接地址: {}:{}", host, port);

            return client;

        } catch (Exception e) {
            log.error("❌ Elasticsearch客户端初始化失败: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.ES_QUERY_FAILED, "Elasticsearch客户端初始化失败", e);
        }
    }
}
