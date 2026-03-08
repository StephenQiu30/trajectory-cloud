package com.trajectory.cloud.search.mq;

import com.trajectory.cloud.common.rabbitmq.constants.RabbitMqConstant;
import com.trajectory.cloud.common.rabbitmq.consumer.MqConsumerDispatcher;
import com.trajectory.cloud.common.rabbitmq.model.RabbitMessage;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.*;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * ES 同步消费者
 * <p>
 * 监听业务队列消息，并通过 {@link MqConsumerDispatcher} 实现标准化分发。
 * </p>
 *
 * @author StephenQiu30
 */
@Slf4j
@Component
public class EsSyncConsumer {

    @Resource
    private MqConsumerDispatcher mqConsumerDispatcher;

    /**
     * 监听 ES 同步主队列。
     *
     * @param rabbitMessage 封装的消息对象
     * @param channel       RabbitMQ 通讯通道
     * @param msg           原生消息对象
     * @throws IOException 确权异常
     */
    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = RabbitMqConstant.ES_SYNC_QUEUE, durable = "true", arguments = {
            @Argument(name = "x-dead-letter-exchange", value = RabbitMqConstant.ES_SYNC_DLX_EXCHANGE),
            @Argument(name = "x-dead-letter-routing-key", value = RabbitMqConstant.ES_SYNC_DLX_ROUTING_KEY)
    }), exchange = @Exchange(value = RabbitMqConstant.ES_SYNC_EXCHANGE, type = ExchangeTypes.DIRECT), key = RabbitMqConstant.ES_SYNC_ROUTING_KEY), ackMode = "MANUAL")
    public void receiveEsSyncMessage(RabbitMessage rabbitMessage, com.rabbitmq.client.Channel channel, Message msg)
            throws IOException {
        if (rabbitMessage == null) {
            log.warn("[EsSyncConsumer] 接收到空消息包，忽略处理");
            return;
        }

        // 执行统一分发
        mqConsumerDispatcher.dispatch(rabbitMessage, channel, msg);
    }

    /**
     * 监听 ES 同步死信队列，执行告警与日志记录。
     */
    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = RabbitMqConstant.ES_SYNC_DLX_QUEUE, durable = "true"), exchange = @Exchange(value = RabbitMqConstant.ES_SYNC_DLX_EXCHANGE, type = "topic"), key = RabbitMqConstant.ES_SYNC_DLX_ROUTING_KEY))
    public void handleDeadLetterEsSync(RabbitMessage rabbitMessage) {
        if (rabbitMessage == null) {
            return;
        }
        log.error("[EsSyncConsumer] 消息进入死信队列，请人工介入检查: [msgId={}], [content={}]",
                rabbitMessage.getMsgId(), rabbitMessage.getMsgText());
    }
}
