package com.trajectory.cloud.notification.mq;

import com.trajectory.cloud.api.notification.model.vo.NotificationVO;
import com.trajectory.cloud.common.rabbitmq.enums.MqBizTypeEnum;
import com.trajectory.cloud.common.rabbitmq.model.NotificationMessage;
import com.trajectory.cloud.common.rabbitmq.utils.MqSender;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 通知消息队列生产者
 * <p>
 * 负责在通知创建后发送 MQ 消息，触发 WebSocket 实时推送。
 * </p>
 *
 * @author StephenQiu30
 */
@Slf4j
@Component
public class NotificationMqProducer {

    @Resource
    private MqSender mqSender;

    /**
     * 发送通知创建事件
     *
     * @param notificationVO 通知视图对象
     */
    public void sendNotificationCreated(NotificationVO notificationVO) {
        if (notificationVO == null || notificationVO.getId() == null) {
            log.warn("[NotificationMqProducer] 通知对象或ID为空，跳过发送");
            return;
        }

        try {
            NotificationMessage message = NotificationMessage.builder()
                    .notificationId(notificationVO.getId())
                    .userId(notificationVO.getUserId())
                    .title(notificationVO.getTitle())
                    .content(notificationVO.getContent())
                    .type(notificationVO.getType())
                    .relatedId(notificationVO.getRelatedId())
                    .relatedType(notificationVO.getRelatedType())
                    .bizId("notification_" + notificationVO.getId())
                    .build();

            mqSender.send(MqBizTypeEnum.NOTIFICATION_SEND, message.getBizId(), message);
            log.info("[NotificationMqProducer] 发送通知创建事件成功, notificationId: {}, userId: {}",
                    notificationVO.getId(), notificationVO.getUserId());
        } catch (Exception e) {
            log.error("[NotificationMqProducer] 发送通知创建事件失败, notificationId: {}, userId: {}",
                    notificationVO.getId(), notificationVO.getUserId(), e);
            // 不抛出异常，避免影响通知创建流程
        }
    }
}
