package com.trajectory.cloud.user.mail.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "login.email.code")
public class EmailCodeProperties {

    private int expireTime = 300;
    private int length = 6;
    private int sendLimit = 60;
    private int ipLimit = 10;
}
