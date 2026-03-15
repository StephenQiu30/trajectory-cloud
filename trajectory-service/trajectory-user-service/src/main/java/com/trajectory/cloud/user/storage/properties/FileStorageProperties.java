package com.trajectory.cloud.user.storage.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "file.storage")
public class FileStorageProperties {

    private String type = "cos";
    private String pathPrefix = "stephen";
}
