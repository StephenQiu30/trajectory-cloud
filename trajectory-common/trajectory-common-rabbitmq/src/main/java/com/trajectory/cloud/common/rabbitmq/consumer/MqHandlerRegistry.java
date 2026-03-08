package com.trajectory.cloud.common.rabbitmq.consumer;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MQ 处理器注册中心
 * <p>
 * 采用注册器模式，利用 Spring 容器自动发现并缓存系统中所有实现了 {@link MqHandler} 的 Bean。
 * 提供基于业务类型（BizType）的高效分发匹配能力，是消费端的核心路由组件。
 * </p>
 *
 * @author StephenQiu30
 */
@Slf4j
@Component
public class MqHandlerRegistry {

    @Resource
    private ApplicationContext applicationContext;

    /**
     * 处理器缓存映射表：bizType -> Handler 实例
     */
    private final Map<String, MqHandler<?>> handlerMap = new ConcurrentHashMap<>();

    /**
     * 系统初始化时自动扫描所有实现类并注册。
     * <p>
     * 若发现重复的 bizType 定义，将记录错误日志并中断启动，以保证路由唯一性。
     * </p>
     */
    @PostConstruct
    @SuppressWarnings("unchecked")
    public void init() {
        Map<String, MqHandler<?>> handlers = (Map<String, MqHandler<?>>) (Map<?, ?>) applicationContext
                .getBeansOfType(MqHandler.class);
        handlers.values().forEach(handler -> {
            String bizType = handler.getBizType();
            if (handlerMap.containsKey(bizType)) {
                log.error("[MqHandlerRegistry] 发现冲突的 MQ 处理器业务类型: {}, 已有: {}, 新发现: {}",
                        bizType, handlerMap.get(bizType).getClass().getSimpleName(),
                        handler.getClass().getSimpleName());
                throw new IllegalStateException("Duplicate MqHandler bizType: " + bizType);
            }
            handlerMap.put(bizType, handler);
            log.info("[MqHandlerRegistry] 自动发现并注册 MQ 处理器: [bizType = {}] -> [class = {}]",
                    bizType, handler.getClass().getSimpleName());
        });
    }

    /**
     * 根据业务类型标识获取具体的处理器实例。
     *
     * @param bizType 业务类型标识串
     * @param <T>     消息体泛型类型
     * @return 对应的 {@link MqHandler} 实例，若未匹配则返回 null
     */
    @SuppressWarnings("unchecked")
    public <T> MqHandler<T> getHandler(String bizType) {
        return (MqHandler<T>) handlerMap.get(bizType);
    }
}
