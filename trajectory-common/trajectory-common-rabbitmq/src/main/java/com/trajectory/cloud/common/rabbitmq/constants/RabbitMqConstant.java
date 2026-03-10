package com.trajectory.cloud.common.rabbitmq.constants;

/**
 * RabbitMQ 常量
 * <p>
 * 所有队列、交换机、路由键均以 {@code trajectory.} 为前缀，
 * 确保在共享 RabbitMQ 实例中的命名唯一性。
 * </p>
 *
 * @author StephenQiu30
 */
public interface RabbitMqConstant {

    // ==================== 邮件相关 ====================

    /**
     * 邮件交换机
     */
    String EMAIL_EXCHANGE = "trajectory.email.exchange";

    /**
     * 邮件队列
     */
    String EMAIL_QUEUE = "trajectory.email.queue";

    /**
     * 邮件路由键
     */
    String EMAIL_ROUTING_KEY = "trajectory.email.send";

    /**
     * 邮件死信交换机
     */
    String EMAIL_DLX_EXCHANGE = "trajectory.email.dlx.exchange";

    /**
     * 邮件死信队列
     */
    String EMAIL_DLX_QUEUE = "trajectory.email.dlx.queue";

    /**
     * 邮件死信路由键
     */
    String EMAIL_DLX_ROUTING_KEY = "trajectory.email.dlx";

    // ==================== WebSocket 相关 ====================

    /**
     * WebSocket 交换机
     */
    String WEBSOCKET_EXCHANGE = "trajectory.websocket.exchange";

    /**
     * WebSocket 推送队列
     */
    String WEBSOCKET_PUSH_QUEUE = "trajectory.websocket.push.queue";

    /**
     * WebSocket 推送路由键
     */
    String WEBSOCKET_PUSH_ROUTING_KEY = "trajectory.websocket.push";

    /**
     * WebSocket 广播队列
     */
    String WEBSOCKET_BROADCAST_QUEUE = "trajectory.websocket.broadcast.queue";

    /**
     * WebSocket 广播路由键
     */
    String WEBSOCKET_BROADCAST_ROUTING_KEY = "trajectory.websocket.broadcast";

    /**
     * WebSocket 死信交换机
     */
    String WEBSOCKET_DLX_EXCHANGE = "trajectory.websocket.dlx.exchange";

    /**
     * WebSocket 死信队列
     */
    String WEBSOCKET_DLX_QUEUE = "trajectory.websocket.dlx.queue";

    /**
     * WebSocket 死信路由键
     */
    String WEBSOCKET_DLX_ROUTING_KEY = "trajectory.websocket.dlx";

    // ==================== Elasticsearch 同步相关 ====================

    /**
     * ES 同步交换机
     */
    String ES_SYNC_EXCHANGE = "trajectory.es.sync.exchange";

    /**
     * ES 同步队列
     */
    String ES_SYNC_QUEUE = "trajectory.es.sync.queue";

    /**
     * ES 同步路由键
     */
    String ES_SYNC_ROUTING_KEY = "trajectory.es.sync";

    /**
     * ES 同步死信交换机
     */
    String ES_SYNC_DLX_EXCHANGE = "trajectory.es.sync.dlx.exchange";

    /**
     * ES 同步死信队列
     */
    String ES_SYNC_DLX_QUEUE = "trajectory.es.sync.dlx.queue";

    /**
     * ES 同步死信路由键
     */
    String ES_SYNC_DLX_ROUTING_KEY = "trajectory.es.sync.dlx";

    // ==================== Notification 相关 ====================

    /**
     * 通知交换机
     */
    String NOTIFICATION_EXCHANGE = "trajectory.notification.exchange";

    /**
     * 通知队列
     */
    String NOTIFICATION_QUEUE = "trajectory.notification.queue";

    /**
     * 通知路由键
     */
    String NOTIFICATION_ROUTING_KEY = "trajectory.notification.create";

    /**
     * 通知死信交换机
     */
    String NOTIFICATION_DLX_EXCHANGE = "trajectory.notification.dlx.exchange";

    /**
     * 通知死信队列
     */
    String NOTIFICATION_DLX_QUEUE = "trajectory.notification.dlx.queue";

    /**
     * 通知死信路由键
     */
    String NOTIFICATION_DLX_ROUTING_KEY = "trajectory.notification.dlx";

    // ==================== 数据同步指令相关 ====================

    /**
     * 数据同步指令交换机 (Topic)
     */
    String SYNC_COMMAND_EXCHANGE = "trajectory.sync.command.exchange";

    /**
     * 用户数据同步指令队列
     */
    String SYNC_COMMAND_QUEUE_USER = "trajectory.sync.command.user.queue";

    /**
     * 用户数据同步指令路由键
     */
    String SYNC_COMMAND_ROUTING_KEY_USER = "trajectory.sync.command.user";

    // ==================== AI 相关 ====================

    /**
     * AI 对话记录交换机
     */
    String AI_CHAT_RECORD_EXCHANGE = "trajectory.ai.chat.record.exchange";

    /**
     * AI 对话记录队列
     */
    String AI_CHAT_RECORD_QUEUE = "trajectory.ai.chat.record.queue";

    /**
     * AI 对话记录路由键
     */
    String AI_CHAT_RECORD_ROUTING_KEY = "trajectory.ai.chat.record.create";

    // ==================== BI 相关 ====================

    /**
     * BI 图表分析交换机
     */
    String BI_CHART_EXCHANGE = "trajectory.bi.chart.exchange";

    /**
     * BI 图表分析队列
     */
    String BI_CHART_QUEUE = "trajectory.bi.chart.queue";

    /**
     * BI 图表分析路由键
     */
    String BI_CHART_ROUTING_KEY = "trajectory.bi.chart.gen";

    /**
     * BI 图表分析死信交换机
     */
    String BI_CHART_DLX_EXCHANGE = "trajectory.bi.chart.dlx.exchange";

    /**
     * BI 图表分析死信队列
     */
    String BI_CHART_DLX_QUEUE = "trajectory.bi.chart.dlx.queue";

    /**
     * BI 图表分析死信路由键
     */
    String BI_CHART_DLX_ROUTING_KEY = "trajectory.bi.chart.dlx";

    // ==================== 图表分析事件相关 ====================

    /**
     * 图表分析事件交换机
     */
    String ANALYSIS_EVENT_EXCHANGE = "trajectory.notification.analysis.exchange";

    /**
     * 图表分析事件队列
     */
    String ANALYSIS_EVENT_QUEUE = "trajectory.notification.analysis.queue";

    /**
     * 图表分析事件路由键
     */
    String ANALYSIS_EVENT_ROUTING_KEY = "trajectory.notification.analysis.routing.key";
}
