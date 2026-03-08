package com.trajectory.cloud.file.config;

import com.trajectory.cloud.file.config.properties.MinioProperties;
import io.minio.MinioClient;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MinIO 配置
 *
 * @author StephenQiu30
 */
@Configuration
@EnableConfigurationProperties(MinioProperties.class)
@ConditionalOnProperty(prefix = "file.storage", name = "type", havingValue = "minio")
@Slf4j
public class MinioConfiguration {

    @Resource
    private MinioProperties minioProperties;

    @Bean
    public MinioClient minioClient() {
        MinioClient minioClient = MinioClient.builder()
                .endpoint(minioProperties.getEndpoint())
                .credentials(minioProperties.getAccessKey(), minioProperties.getSecretKey())
                .build();
        log.info("MinIO客户端初始化成功，endpoint: {}, bucket: {}", minioProperties.getEndpoint(), minioProperties.getBucket());
        return minioClient;
    }

}
