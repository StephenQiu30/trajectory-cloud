package com.trajectory.cloud.common.rabbitmq.consumer;

import java.lang.annotation.*;

/**
 * MQ 幂等性处理注解
 * 用于标识 IMqHandler 的实现类或具体方法，开启基于 Redis 的分布式去重锁
 *
 * @author StephenQiu30
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MqIdempotent {

    /**
     * 缓存 Key 前缀
     *
     * @return 前缀字符串
     */
    String prefix() default "mq:dedupe:";

    /**
     * 过期时间（秒）
     * 默认 24 小时，防止重复消费
     *
     * @return 过期秒数
     */
    int expire() default 86400;

    /**
     * 幂等 Key 的 SpEL 表达式（可选）
     * 如果为空，默认使用 RabbitMessage 中的 msgId
     *
     * @return SpEL 表达式
     */
    String key() default "";
}
