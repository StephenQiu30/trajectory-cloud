package com.trajectory.cloud.notification.mq;

import com.rabbitmq.client.Channel;
import com.trajectory.cloud.common.rabbitmq.constants.RabbitMqConstant;
import com.trajectory.cloud.common.rabbitmq.consumer.MqConsumerDispatcher;
import com.trajectory.cloud.common.rabbitmq.model.RabbitMessage;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.*;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 通知消息队列消费者
 * <p>
 * 监听业务事件队列（评论、点赞、关注），采用 {@link MqConsumerDispatcher} 进行标准化分发处理，
 * 自动创建对应类型的通知。
 * </p>
 *
 * @author StephenQiu30
 */
@Slf4j
@Component
public class NotificationMqConsumer {

    @Resource
    private MqConsumerDispatcher mqConsumerDispatcher;

    /**
     * 监听图表分析事件队列，自动创建分析通知
     *
     * @param rabbitMessage RabbitMessage 对象
     * @param channel       RabbitMQ 通道
     * @param msg           Spring AMQP 消息对象
     */
    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = RabbitMqConstant.ANALYSIS_EVENT_QUEUE, durable = "true", arguments = {
            @Argument(name = "x-dead-letter-exchange", value = RabbitMqConstant.NOTIFICATION_DLX_EXCHANGE),
            @Argument(name = "x-dead-letter-routing-key", value = RabbitMqConstant.NOTIFICATION_DLX_ROUTING_KEY)
    }), exchange = @Exchange(value = RabbitMqConstant.ANALYSIS_EVENT_EXCHANGE, type = "topic"), key = RabbitMqConstant.ANALYSIS_EVENT_ROUTING_KEY), ackMode = "MANUAL")
    public void handleAnalysisEvent(RabbitMessage rabbitMessage, Channel channel, Message msg) throws IOException {
        mqConsumerDispatcher.dispatch(rabbitMessage, channel, msg);
    }

    /**
     * 监听通知系统死信队列，打印最终失败记录
     *
     * @param rabbitMessage RabbitMessage 对象
     */
    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = RabbitMqConstant.NOTIFICATION_DLX_QUEUE, durable = "true"), exchange = @Exchange(value = RabbitMqConstant.NOTIFICATION_DLX_EXCHANGE, type = "topic"), key = RabbitMqConstant.NOTIFICATION_DLX_ROUTING_KEY))
    public void handleDeadLetterNotification(RabbitMessage rabbitMessage) {
        log.error("[NotificationMqConsumer] 通知消息进入死信队列, msgId: {}, 内容: {}",
                rabbitMessage.getMsgId(), rabbitMessage.getMsgText());
    }
}
