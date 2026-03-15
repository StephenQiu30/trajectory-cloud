package com.trajectory.cloud.ai;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * AI 服务启动类
 *
 * @author StephenQiu30
 */
@SpringBootApplication(scanBasePackages = "com.trajectory.cloud")
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.trajectory.cloud.api")
@MapperScan("com.trajectory.cloud.ai.mapper")
@EnableScheduling
public class AiServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiServiceApplication.class, args);
    }

}
