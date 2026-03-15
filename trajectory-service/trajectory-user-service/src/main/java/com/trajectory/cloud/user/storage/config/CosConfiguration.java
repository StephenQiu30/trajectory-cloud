package com.trajectory.cloud.user.storage.config;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.region.Region;
import com.trajectory.cloud.user.storage.properties.CosProperties;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(CosProperties.class)
@ConditionalOnProperty(prefix = "file.storage", name = "type", havingValue = "cos")
@Slf4j
public class CosConfiguration {

    @Resource
    private CosProperties cosProperties;

    @Bean
    public COSClient cosClient() {
        COSCredentials cred = new BasicCOSCredentials(cosProperties.getAccessKey(), cosProperties.getSecretKey());
        Region region = new Region(cosProperties.getRegion());
        ClientConfig clientConfig = new ClientConfig(region);
        log.info("COS客户端初始化成功，region: {}, bucket: {}", cosProperties.getRegion(), cosProperties.getBucket());
        return new COSClient(cred, clientConfig);
    }
}
