package com.trajectory.cloud.file.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 阿里云 OSS 配置属性
 *
 * @author StephenQiu30
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "oss.client")
public class OssProperties {

    /**
     * 是否启用
     */
    private boolean enable;

    /**
     * 域名
     */
    private String endpoint;

    /**
     * 密钥 ID
     */
    private String accessKey;

    /**
     * 密钥 Secret
     */
    private String secretKey;

    /**
     * 存储桶名称
     */
    private String bucket;
}
