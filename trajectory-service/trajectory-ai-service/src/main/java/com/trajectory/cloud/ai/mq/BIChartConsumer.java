package com.trajectory.cloud.ai.mq;

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
 * BI 图表分析消息队列消费者
 * <p>
 * 采用 {@link MqConsumerDispatcher} 进行标准化分发处理，
 * 委托 {@link BIChartHandler} 执行实际的 AI 分析业务。
 * </p>
 *
 * @author StephenQiu30
 */
@Slf4j
@Component
public class BIChartConsumer {

    @Resource
    private MqConsumerDispatcher mqConsumerDispatcher;

    /**
     * 监听 BI 图表分析队列
     *
     * @param rabbitMessage RabbitMessage 对象
     * @param channel       RabbitMQ 通道
     * @param msg           Spring AMQP 消息对象
     */
    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = RabbitMqConstant.BI_CHART_QUEUE, durable = "true", arguments = {
            @Argument(name = "x-dead-letter-exchange", value = RabbitMqConstant.BI_CHART_DLX_EXCHANGE),
            @Argument(name = "x-dead-letter-routing-key", value = RabbitMqConstant.BI_CHART_DLX_ROUTING_KEY)
    }), exchange = @Exchange(value = RabbitMqConstant.BI_CHART_EXCHANGE, type = "direct"), key = RabbitMqConstant.BI_CHART_ROUTING_KEY), ackMode = "MANUAL")
    public void handleBIChart(RabbitMessage rabbitMessage, Channel channel, Message msg) throws IOException {
        mqConsumerDispatcher.dispatch(rabbitMessage, channel, msg);
    }

    /**
     * 监听 BI 图表分析死信队列，打印最终失败记录
     *
     * @param rabbitMessage RabbitMessage 对象
     */
    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = RabbitMqConstant.BI_CHART_DLX_QUEUE, durable = "true"), exchange = @Exchange(value = RabbitMqConstant.BI_CHART_DLX_EXCHANGE, type = "topic"), key = RabbitMqConstant.BI_CHART_DLX_ROUTING_KEY))
    public void handleDeadLetterBIChart(RabbitMessage rabbitMessage) {
        log.error("[BIChartConsumer] BI 图表分析消息进入死信队列, msgId: {}, 内容: {}",
                rabbitMessage.getMsgId(), rabbitMessage.getMsgText());
    }
}
