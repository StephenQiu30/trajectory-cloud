package com.trajectory.cloud.user.mail.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "mail")
public class MailProperties {

    private String from = "noreply@trajectory-cloud.com";
    private String fromName = "Trajectory";
}
