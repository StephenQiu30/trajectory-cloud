package com.trajectory.cloud.common.rabbitmq.event;

import com.trajectory.cloud.common.rabbitmq.enums.MqBizTypeEnum;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 事务型 MQ 本地观察者事件
 * <p>
 * 在微服务发生本地数据库事务（如：点赞、发布帖子）时，将 MQ 的发送包裹在本地事件中抛出。
 * MqSender 的内部监听器会拦截此事件，等到数据库主事务提交确认 (`TransactionPhase.AFTER_COMMIT`) 后，再真实投递至
 * RabbitMQ。
 * </p>
 *
 * @author StephenQiu30
 */
@Getter
public class TransactionalMqEvent extends ApplicationEvent {

    private final MqBizTypeEnum bizTypeEnum;
    private final String msgId;
    private final Object messagePayload;

    /**
     * 构造事务型 MQ 事件
     *
     * @param source         事件的发出者通常为 (this)
     * @param bizTypeEnum    MQ 策略路由类型
     * @param msgId          自定义消息 ID，传入 null 则由 MqSender 自动生成 UUID
     * @param messagePayload 发送的载体（实体对象或 DTO）
     */
    public TransactionalMqEvent(Object source, MqBizTypeEnum bizTypeEnum, String msgId, Object messagePayload) {
        super(source);
        this.bizTypeEnum = bizTypeEnum;
        this.msgId = msgId;
        this.messagePayload = messagePayload;
    }
}
