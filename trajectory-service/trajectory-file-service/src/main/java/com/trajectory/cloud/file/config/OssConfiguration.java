package com.trajectory.cloud.file.config;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.trajectory.cloud.file.config.properties.OssProperties;
import jakarta.annotation.Resource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 阿里云 OSS 配置类
 *
 * @author StephenQiu30
 */
@Configuration
@ConditionalOnProperty(prefix = "file.storage", name = "type", havingValue = "oss")
public class OssConfiguration {

    @Resource
    private OssProperties ossProperties;

    @Bean
    public OSS ossClient() {
        return new OSSClientBuilder().build(
                ossProperties.getEndpoint(),
                ossProperties.getAccessKey(),
                ossProperties.getSecretKey());
    }
}
