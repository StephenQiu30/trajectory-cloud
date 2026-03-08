package com.trajectory.cloud.common.rabbitmq.consumer;

import cn.hutool.json.JSONUtil;
import com.rabbitmq.client.Channel;
import com.trajectory.cloud.common.rabbitmq.config.MqConsumerProperties;
import com.trajectory.cloud.common.rabbitmq.model.RabbitMessage;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * MQ 消费端核心分发器 (Dispatcher)
 * <p>
 * 该类作为所有消费者的统一入口，编排了消息处理的完整生命周期：
 * 1. 自动路由分发：基于 {@link MqHandlerRegistry} 匹配业务处理器。
 * 2. 声明式幂等锁：解析处理器上的 {@link MqIdempotent} 注解并执行分布式去重。
 * 3. 自动数据解析：将字节流转化为处理器所需的泛型 DTO。
 * 4. 链路审计与监控：统计消费时长并统一异常分类处理。
 * </p>
 *
 * @author StephenQiu30
 */
@Slf4j
@Component
public class MqConsumerDispatcher {

    @Resource
    private MqHandlerRegistry handlerRegistry;

    @Resource
    private MqCacheHelper cacheHelper;

    @Resource
    private MqConsumerProperties mqConsumerProperties;

    /**
     * 统一分发并处理消息。
     * <p>
     * 该方法封装了幂等校验、反序列化、业务执行及 Ack 确认的闭环逻辑。
     * 业务 Listener 只需调用此方法即可，无需关心底层样板代码。
     * </p>
     *
     * @param rabbitMessage 原始封装消息对象
     * @param channel       RabbitMQ 通讯通道
     * @param msg           原生 Message 对象（用于提取重试状态等）
     * @param <T>           消息体泛型类型
     * @throws IOException 网络确认异常
     */
    public <T> void dispatch(RabbitMessage rabbitMessage, Channel channel, Message msg) throws IOException {
        long deliveryTag = msg.getMessageProperties().getDeliveryTag();
        long startTime = System.currentTimeMillis();

        // 1. 基础校验：消息体完整性
        if (rabbitMessage == null || rabbitMessage.getBizType() == null || rabbitMessage.getMsgId() == null) {
            log.error("[MqConsumerDispatcher] 关键信息缺失，拒绝消费: {}", rabbitMessage);
            channel.basicNack(deliveryTag, false, false); // 丢弃无效消息
            return;
        }

        String bizType = rabbitMessage.getBizType();
        String msgId = rabbitMessage.getMsgId();

        // 2. 路由匹配：获取业务处理器
        MqHandler<T> handler = handlerRegistry.getHandler(bizType);
        if (handler == null) {
            log.warn("[MqConsumerDispatcher] 未匹配到对应的处理器: [bizType={}], [msgId={}]，已自动确权防止堆积", bizType, msgId);
            channel.basicAck(deliveryTag, false);
            return;
        }

        try {
            // 3. 声明式幂等校验：获取处理器上的注解并判定
            MqIdempotent annotation = handler.getClass().getAnnotation(MqIdempotent.class);
            if (annotation != null) {
                String dedupeKey = annotation.prefix() + ":" + bizType + ":" + msgId;
                if (!cacheHelper.setIfAbsent(dedupeKey, annotation.expire())) {
                    log.info("[MqConsumerDispatcher] 触发幂等拦截，跳过重复处理: [bizType={}], [msgId={}]", bizType, msgId);
                    channel.basicAck(deliveryTag, false);
                    return;
                }
            }

            // 4. 数据自动转换
            T data = JSONUtil.toBean(rabbitMessage.getMsgText(), handler.getDataType());
            if (data == null) {
                log.error("[MqConsumerDispatcher] 消息内容格式非法，反序列化失败: [bizType={}], [msgId={}]", bizType, msgId);
                channel.basicNack(deliveryTag, false, false);
                return;
            }

            // 5. 执行核心业务
            log.debug("[MqConsumerDispatcher] 调度处理器执行: [bizType={}], [Handler={}]", bizType,
                    handler.getClass().getSimpleName());
            handler.onMessage(data, rabbitMessage);

            // 6. 确权：成功 ACK
            channel.basicAck(deliveryTag, false);
            long elapsed = System.currentTimeMillis() - startTime;
            if (elapsed > mqConsumerProperties.getSlowConsumeThresholdMs()) {
                log.warn("[MqConsumerDispatcher] 消费耗时过长： [bizType={}], [msgId={}], [耗时={}ms]", bizType, msgId, elapsed);
            } else {
                log.info("[MqConsumerDispatcher] 消费成功: [bizType={}], [msgId={}], [耗时={}ms]", bizType, msgId, elapsed);
            }

        } catch (Exception e) {
            log.error("[MqConsumerDispatcher] 业务执行异常: [bizType={}], [msgId={}]", bizType, msgId, e);
            // 设置 requeue=false，业务异常统一进入死信队列，防止无限重试
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
