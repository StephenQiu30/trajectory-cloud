package com.trajectory.cloud.common.rabbitmq.consumer;

import com.trajectory.cloud.common.rabbitmq.model.RabbitMessage;

/**
 * MQ 业务处理器核心接口
 * <p>
 * 基于策略模式设计，每种业务类型（BizType）对应一个具体的处理器实现。
 * 该接口定义了消息处理的标准协议，由 {@link MqConsumerDispatcher} 统一调度。
 * </p>
 *
 * @param <T> 消息体解包后的泛型类型
 * @author StephenQiu30
 */
public interface MqHandler<T> {

    /**
     * 获取当前处理器对应的业务类型。
     * <p>
     * 注册器 {@link MqHandlerRegistry} 会根据该类型进行消息路由分发。
     * </p>
     *
     * @return 业务类型唯一标识字符串
     */
    String getBizType();

    /**
     * 处理核心业务逻辑。
     * <p>
     * 由容器/门面解析数据后触发。执行过程中若抛出异常，将触发重试或进入死信队列。
     * </p>
     *
     * @param data          已解析的业务数据对象
     * @param rabbitMessage 原始包装消息（包含 msgId 等元数据，可用于链路追踪）
     * @throws Exception 业务执行异常
     */
    void onMessage(T data, RabbitMessage rabbitMessage) throws Exception;

    /**
     * 获取业务数据的实际类型。
     * <p>
     * 用于 {@link MqConsumerFacade} 在运行时自动进行 JSON 反序列化。
     * </p>
     *
     * @return 业务数据类型的 Class 对象
     */
    Class<T> getDataType();

    /**
     * 获取该业务允许的最大重试次数。
     * <p>
     * 提供默认实现，子类可根据业务敏感度进行覆盖。
     * </p>
     *
     * @return 最大重试次数
     */
    default int getMaxRetryCount() {
        return 3;
    }
}
