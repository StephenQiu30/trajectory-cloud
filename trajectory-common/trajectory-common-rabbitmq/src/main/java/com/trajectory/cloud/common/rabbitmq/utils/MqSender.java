package com.trajectory.cloud.common.rabbitmq.utils;

import cn.hutool.json.JSONUtil;
import com.trajectory.cloud.common.rabbitmq.enums.MqBizTypeEnum;
import com.trajectory.cloud.common.rabbitmq.event.TransactionalMqEvent;
import com.trajectory.cloud.common.rabbitmq.model.RabbitMessage;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;

/**
 * 统一的消息队列发送门面 (Facade Pattern)
 * <p>
 * 完全取代了早期散弹式的 EmailMqUtils, WebSocketMqUtils 等工具。
 * 结合 Strategy 模式（向本类传入
 * {@link MqBizTypeEnum}），使得消费者在处理消息时一定能带上正确的业务路由类型（BizType）。
 * 并且支持直接发送和观察者模式下基于当前 DB 事务的延迟安全发送。
 * </p>
 *
 * @author StephenQiu30
 */
@Slf4j
@Component
public class MqSender {

    @Resource(name = "rabbitTemplate")
    private RabbitTemplate rabbitTemplate;

    @Resource
    private ApplicationEventPublisher eventPublisher;

    /**
     * 【直接发送策略】立即构建消息载体，推入 RabbitMQ 网络。
     * 适合不受数据库事务约束的场景（如短信、验证码下发、与 DB 数据解耦的操作）。
     *
     * @param bizTypeEnum 业务枚举策略，明确了该消息的 Exchange, RoutingKey 和 BizType。
     * @param msgId       可选的分布式唯一消息流水号，若为 Null 则自动使用 UUID 填充。
     * @param payload     真实的投递数据载体
     */
    public void send(MqBizTypeEnum bizTypeEnum, String msgId, Object payload) {
        if (payload == null) {
            log.error("[MqSender] 发送被拒绝，因业务载体 (Payload) 为 null。业务分类: {}", bizTypeEnum.getValue());
            return;
        }

        try {
            // 利用 Builder 模式构建标准化载体，解决消费端 BizType 丢失的陈年旧疾
            RabbitMessage rabbitMessage = RabbitMessage.builder()
                    .msgId(msgId != null ? msgId : UUID.randomUUID().toString())
                    .bizType(bizTypeEnum.getValue())
                    .msgText(JSONUtil.toJsonStr(payload))
                    .build();

            rabbitTemplate.convertAndSend(bizTypeEnum.getExchange(), bizTypeEnum.getRoutingKey(), rabbitMessage);

            log.info("[MqSender - 直接发送成功] Exchange={}, Route={}, BizType={}, MsgId={}",
                    bizTypeEnum.getExchange(), bizTypeEnum.getRoutingKey(), bizTypeEnum.getValue(),
                    rabbitMessage.getMsgId());
        } catch (Exception e) {
            log.error("[MqSender - 网络投递异常] 业务类型: {}, 消息编号: {}", bizTypeEnum.getValue(), msgId, e);
            throw e;
        }
    }

    /**
     * 【直接发送策略】参数缺省版
     *
     * @param bizTypeEnum 业务枚举策略
     * @param payload     业务数据
     */
    public void send(MqBizTypeEnum bizTypeEnum, Object payload) {
        send(bizTypeEnum, null, payload);
    }

    /**
     * 【事务发送策略】 (Observer Pattern - 发出端)
     * 将 MQ 消息包成 Spring ApplicationEvent 抛出不立即执行网络投递。
     * 取而代之，会等到最外层调用此方法的 Spring `@Transactional` 拦截器将数据库成功 `COMMIT` 后触发投递。
     *
     * @param bizTypeEnum MQ 业务枚举
     * @param msgId       自定义消息流水号（可传 Null）
     * @param payload     发送载荷
     */
    public void sendTransactional(MqBizTypeEnum bizTypeEnum, String msgId, Object payload) {
        TransactionalMqEvent event = new TransactionalMqEvent(this, bizTypeEnum, msgId, payload);
        eventPublisher.publishEvent(event);
        log.debug("[MqSender - 抛出事务内观察事件] 等待数据库事务 COMMIT 后发送, BizType={}, MsgId={}", bizTypeEnum.getValue(), msgId);
    }

    /**
     * 【事务发送策略】参数缺省版
     *
     * @param bizTypeEnum MQ 业务枚举
     * @param payload     发送载荷
     */
    public void sendTransactional(MqBizTypeEnum bizTypeEnum, Object payload) {
        sendTransactional(bizTypeEnum, null, payload);
    }

    /**
     * 【事务型事件监听器】 (Observer Pattern - 接收端)
     * 自动拦截 {@link #sendTransactional} 发布出来的事件。
     * phase = TransactionPhase.AFTER_COMMIT （只有成功提交 DB 事务才发 MQ）
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void triggerTransactionalMqEvent(TransactionalMqEvent event) {
        log.debug("[MqSender - 探测到 DB 事务提交成功] 正在接管本地事件，向 RabbitMQ 投递消息...");
        send(event.getBizTypeEnum(), event.getMsgId(), event.getMessagePayload());
    }

}
