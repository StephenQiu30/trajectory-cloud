package com.trajectory.cloud.user.storage.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "cos.client")
public class CosProperties {

    private Boolean enable = false;
    private String accessKey;
    private String secretKey;
    private String region;
    private String bucket;
}
