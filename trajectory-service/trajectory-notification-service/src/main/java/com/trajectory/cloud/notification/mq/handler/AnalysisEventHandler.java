package com.trajectory.cloud.notification.mq.handler;

import com.trajectory.cloud.api.ai.model.enums.ChartStatusEnum;
import com.trajectory.cloud.api.notification.model.enums.NotificationTypeEnum;
import com.trajectory.cloud.common.rabbitmq.consumer.MqHandler;
import com.trajectory.cloud.common.rabbitmq.consumer.MqIdempotent;
import com.trajectory.cloud.common.rabbitmq.enums.MqBizTypeEnum;
import com.trajectory.cloud.common.rabbitmq.model.RabbitMessage;
import com.trajectory.cloud.common.rabbitmq.model.event.AnalysisEvent;
import com.trajectory.cloud.notification.model.entity.Notification;
import com.trajectory.cloud.notification.service.NotificationService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 图表分析事件处理器
 *
 * @author StephenQiu30
 */
@Slf4j
@Component
@MqIdempotent(prefix = "mq:notification:analysis", expire = 86400)
public class AnalysisEventHandler implements MqHandler<AnalysisEvent> {

    @Resource
    private NotificationService notificationService;

    @Override
    public String getBizType() {
        return MqBizTypeEnum.ANALYSIS_EVENT.getValue();
    }

    @Override
    public void onMessage(AnalysisEvent event, RabbitMessage rabbitMessage) throws Exception {
        if (event.getChartId() == null || event.getUserId() == null) {
            log.error("[AnalysisEventHandler] 分析事件解析失败或缺少必要字段, msgId: {}", rabbitMessage.getMsgId());
            throw new IllegalArgumentException("缺少必要字段");
        }

        log.info("[AnalysisEventHandler] 收到分析事件, chartId: {}, userId: {}, status: {}",
                event.getChartId(), event.getUserId(), event.getStatus());

        String status = event.getStatus();
        String title = "图表分析任务通知";
        String content;

        if (ChartStatusEnum.SUCCEED.getValue().equals(status)) {
            content = String.format("您的图表分析任务《%s》已处理完成，快去查看吧！", event.getChartName());
        } else {
            content = String.format("您的图表分析任务《%s》处理失败：%.50s", event.getChartName(),
                    event.getExecMessage() != null ? event.getExecMessage() : "未知错误");
        }

        // 创建通知
        Notification notification = new Notification();
        notification.setType(NotificationTypeEnum.ANALYSIS.getCode());
        notification.setUserId(event.getUserId());
        notification.setTitle(title);
        notification.setContent(content);
        notification.setRelatedId(event.getChartId());
        notification.setRelatedType("chart");
        notification.setBizId("analysis_" + event.getChartId() + "_" + status);
        notification.setIsRead(0);

        notificationService.addNotification(notification);

        log.info("[AnalysisEventHandler] 分析通知创建成功, chartId: {}, notificationId: {}",
                event.getChartId(), notification.getId());
    }

    @Override
    public Class<AnalysisEvent> getDataType() {
        return AnalysisEvent.class;
    }
}
