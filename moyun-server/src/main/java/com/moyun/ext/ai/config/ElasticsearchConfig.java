package com.moyun.ext.ai.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URI;

/**
 * Elasticsearch 配置类
 *
 * <p>配置 Elasticsearch 8.x 客户端连接，用于向量存储和检索。
 * 仅在 app.embedding-store.type=es 时加载，避免未部署 ES 时启动失败。</p>
 *
 * <p>向量库切换：app.embedding-store.type=redis（默认，复用 Redis 8.0+ RediSearch）
 * 或 es（独立 Elasticsearch 集群）。</p>
 *
 * @author laomao
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "app.embedding-store.type", havingValue = "es")
public class ElasticsearchConfig {

    @Value("${elasticsearch.uris:http://localhost:9200}")
    private String uris;

    @Value("${elasticsearch.username:}")
    private String username;

    @Value("${elasticsearch.password:}")
    private String password;

    @Bean
    public ElasticsearchClient elasticsearchClient() {
        try {
            URI uri = URI.create(uris);
            String host = uri.getHost();
            int port = uri.getPort() > 0 ? uri.getPort() : 9200;
            String scheme = uri.getScheme() != null ? uri.getScheme() : "http";

            BasicCredentialsProvider credsProv = new BasicCredentialsProvider();
            if (username != null && !username.isEmpty()) {
                credsProv.setCredentials(AuthScope.ANY,
                    new UsernamePasswordCredentials(username, password));
            }

            RestClient restClient = RestClient.builder(new HttpHost(host, port, scheme))
                .setHttpClientConfigCallback(httpClientBuilder ->
                    httpClientBuilder.setDefaultCredentialsProvider(credsProv))
                .build();

            ElasticsearchTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
            ElasticsearchClient client = new ElasticsearchClient(transport);

            log.info("Elasticsearch client initialized: {}", uris);
            return client;
        } catch (Exception e) {
            log.error("Elasticsearch client init failed: {}", e.getMessage(), e);
            throw new RuntimeException("Elasticsearch init failed", e);
        }
    }
}
