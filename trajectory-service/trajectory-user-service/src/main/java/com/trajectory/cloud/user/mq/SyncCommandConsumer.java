package com.trajectory.cloud.user.mq;

import com.rabbitmq.client.Channel;
import com.trajectory.cloud.common.rabbitmq.constants.RabbitMqConstant;
import com.trajectory.cloud.common.rabbitmq.consumer.MqConsumerDispatcher;
import com.trajectory.cloud.common.rabbitmq.model.RabbitMessage;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 数据同步指令消费者 (V5 门面代理版)
 *
 * @author StephenQiu30
 */
@Component
@Slf4j
public class SyncCommandConsumer {

    @Resource
    private MqConsumerDispatcher mqConsumerDispatcher;

    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = RabbitMqConstant.SYNC_COMMAND_QUEUE_USER, durable = "true"), exchange = @Exchange(value = RabbitMqConstant.SYNC_COMMAND_EXCHANGE, type = ExchangeTypes.TOPIC), key = RabbitMqConstant.SYNC_COMMAND_ROUTING_KEY_USER), ackMode = "MANUAL")
    public void receiveSyncCommand(RabbitMessage rabbitMessage, Channel channel,
                                   Message msg) throws IOException {
        if (rabbitMessage == null) {
            log.error("[SyncCommandConsumer] 接收到空消息包");
            return;
        }

        // 委派门面处理
        mqConsumerDispatcher.dispatch(rabbitMessage, channel, msg);
    }
}
