package com.trajectory.cloud.common.rabbitmq.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * MQ 消费者配置属性
 * <p>
 * 集中映射 {@code mq.consumer.*} 命名空间下的所有配置项，
 * 避免散落的 {@code @Value} 注解，支持 IDE 自动补全和类型安全绑定。
 * </p>
 *
 * @author StephenQiu30
 */
@Data
@Component
@ConfigurationProperties(prefix = "mq.consumer")
public class MqConsumerProperties {

    /**
     * 单条消息消费耗时告警阈值（毫秒）
     * <p>
     * 超过此值将输出 WARN 日志，便于排查性能瓶颈。
     * </p>
     * 对应配置：{@code mq.consumer.slow-consume-threshold-ms}
     */
    private long slowConsumeThresholdMs = 5000L;
}
