package com.trajectory.cloud.search;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 搜索服务启动类
 *
 * @author stephen
 */
@EnableScheduling
@SpringBootApplication(exclude = { DataSourceAutoConfiguration.class }, scanBasePackages = { "com.trajectory.cloud.search",
        "com.trajectory.cloud.common" })
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.trajectory.cloud.api")
@EnableElasticsearchRepositories(basePackages = "com.trajectory.cloud.search.repository")
public class SearchServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SearchServiceApplication.class, args);
    }
}
